package com.coinbase.walletlink

import com.coinbase.networking.connectivity.Internet
import com.coinbase.wallet.crypto.extensions.sha256
import com.coinbase.wallet.store.utils.JSON
import com.coinbase.walletlink.api.WalletLinkAPI
import com.coinbase.walletlink.api.WalletLinkWebSocket
import com.coinbase.walletlink.concurrency.OperationQueue
import com.coinbase.walletlink.dtos.RequestEthereumAddressesParams
import com.coinbase.walletlink.dtos.ServerRequestDTO
import com.coinbase.walletlink.dtos.SignEthereumMessageParams
import com.coinbase.walletlink.dtos.SignEthereumTransactionParams
import com.coinbase.walletlink.dtos.SubmitEthereumTransactionParams
import com.coinbase.walletlink.dtos.Web3RequestDTO
import com.coinbase.walletlink.dtos.Web3ResponseDTO
import com.coinbase.walletlink.dtos.asJsonString
import com.coinbase.walletlink.dtos.fromJsonString
import com.coinbase.walletlink.exceptions.WalletLinkException
import com.coinbase.walletlink.extensions.asBigInteger
import com.coinbase.walletlink.extensions.asJsonMap
import com.coinbase.walletlink.extensions.asUnit
import com.coinbase.walletlink.extensions.byteArrayUsingHexEncoding
import com.coinbase.walletlink.extensions.decryptUsingAES256GCM
import com.coinbase.walletlink.extensions.encryptUsingAES256GCM
import com.coinbase.walletlink.extensions.logError
import com.coinbase.walletlink.extensions.takeSingle
import com.coinbase.walletlink.extensions.toPrefixedHexString
import com.coinbase.walletlink.models.ClientMetadataKey
import com.coinbase.walletlink.models.HostRequest
import com.coinbase.walletlink.models.HostRequestId
import com.coinbase.walletlink.models.RequestEventType
import com.coinbase.walletlink.models.RequestMethod
import com.coinbase.walletlink.models.ResponseEventType
import com.coinbase.walletlink.models.Session
import com.coinbase.walletlink.rx.Singles
import com.coinbase.walletlink.storage.SessionStore
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private data class JoinSessionEvent(val sessionId: String, val joined: Boolean)

internal class WalletLinkConnection(
    val url: URL,
    val userId: String,
    val notificationUrl: URL,
    val sessionStore: SessionStore,
    val metadata: ConcurrentHashMap<ClientMetadataKey, String>
) {
    private val requestsSubject = PublishSubject.create<HostRequest>()
    private val joinSessionEventsSubject = PublishSubject.create<JoinSessionEvent>()
    private val api = WalletLinkAPI(url)
    private val socket = WalletLinkWebSocket(url)
    private val disposeBag = CompositeDisposable()
    private val operationQueue = OperationQueue(maxConcurrentThreads = 1)
    private val isConnectedObservable: Observable<Boolean> = socket.connectionStateObservable.map { it.isConnected }

    /**
     * Incoming WalletLink host requests
     */
    val requestsObservable = requestsSubject.hide()

    init {
        socket.incomingRequestsObservable
            .subscribe { handleIncomingRequest(it) }
            .let { disposeBag.add(it) }

        observeConnection()
    }

    /**
     * Stop connection when WalletLink instance is deallocated
     */
    fun disconnect() {
        // FIXME: hish - operationQueue.cancelAllOperations()
        disposeBag.clear()
        stopConnection().subscribe()
    }

    /**
     * Connect to WalletLink server using parameters extracted from QR code scan
     *
     * @param sessionId WalletLink host generated session ID
     * @param name Host name
     * @param secret WalletLink host/guest shared secret
     *
     * @return A single wrapping `Unit` if connection was successful. Otherwise, an exception is thrown
     */
    fun link(sessionId: String, name: String, secret: String): Single<Unit> {
        val session = sessionStore.getSession(id = sessionId, url = url)
        // FIXME: hish - uncomment
//        if (session != null && session.secret == secret) {
//            return Single.just(Unit)
//        }

        return isConnectedObservable
            .doOnSubscribe { sessionStore.save(rpcUrl = url, sessionId = sessionId, name = name, secret = secret) }
            .filter { it }
            .takeSingle()
            .flatMap { joinSessionEventsSubject.filter { it.sessionId == sessionId }.takeSingle() }
            .map { if (!it.joined) throw WalletLinkException.InvalidSession }
            .timeout(1500, TimeUnit.SECONDS) // FIXME: hish - back to 15
            .logError()
    }

    /**
     * Set metadata in all active sessions. This metadata will be forwarded to all the hosts
     *
     * @param key Metadata key
     * @param value Metadata value
     *
     * @return A single wrapping `Unit` if operation was successful. Otherwise, an exception is thrown
     */
    fun setMetadata(key: ClientMetadataKey, value: String): Single<Unit> {
        metadata[key] = value

        val singles: List<Single<Boolean>> = sessionStore.getSessions(url).mapNotNull { session ->
            try {
                val encryptedValue = value.encryptUsingAES256GCM(secret = session.secret)
                return@mapNotNull socket.setMetadata(key, encryptedValue, session.id).logError()
            } catch (err: WalletLinkException.UnableToEncryptData) {
                return@mapNotNull null
            }
        }

        return Singles.zip(singles).asUnit()
    }

    /**
     * Send signature request approval to the requesting host
     *
     * @param sessionId WalletLink host generated session ID
     * @param requestId WalletLink request ID
     * @param signedData User signed data
     *
     * @return A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
     */
    fun approve(sessionId: String, requestId: String, signedData: ByteArray): Single<Unit> {
        val session = sessionStore.getSession(id = sessionId, url = url)
            ?: return Single.error(WalletLinkException.NoConnectionFound(url))

        val response = Web3ResponseDTO(id = requestId, result = signedData.toPrefixedHexString())

        return submitWeb3Response(response, session = session)
    }

    /**
     * Send signature request rejection to the requesting host
     *
     * @param sessionId WalletLink host generated session ID
     * @param requestId WalletLink request ID
     *
     * @return A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
     */
    fun reject(sessionId: String, requestId: String): Single<Unit> {
        val session = sessionStore.getSession(id = sessionId, url = url)
            ?: return Single.error(WalletLinkException.NoConnectionFound(url))

        val response = Web3ResponseDTO<String>(id = requestId, errorMessage = "User rejected signature request")

        return submitWeb3Response(response, session = session)
    }

    /**
     * Get a Host initiated request
     *
     * @param eventId The request's event ID
     * @param sessionId The request's session ID
     *
     * @return A Single wrapping the HostRequest
     */
    fun getRequest(eventId: String, sessionId: String): Single<HostRequest> = TODO("FIXME: hish")

    // MARK: - Connection management

    private fun startConnection(): Single<Unit> {
        // FIXME: hish - operationQueue.cancelAllOperations()

        val connectSingle = Internet.statusChanges
            .map { println("hish: before $it"); it }
            .filter { it.isOnline }
            .map { println("hish: after $it") }
            .takeSingle()
            .flatMap { socket.connect() }
            .logError()

        return operationQueue.addSingle(connectSingle)
    }

    private fun stopConnection(): Single<Unit> {
        // FIXME: hish - operationQueue.cancelAllOperations()

        val disconnectSingle = socket.disconnect()
            .logError()
            .onErrorReturn { Single.just(Unit) }

        return operationQueue.addSingle(disconnectSingle)
    }

    // MARK: - Session management

    private fun joinSessions(sessions: List<Session>): Single<Unit> {
        val joinSessionSingles = sessions.map { joinSession(it).asUnit().onErrorReturn { Unit } }

        return Singles.zip(joinSessionSingles).asUnit()
    }

    private fun joinSession(session: Session): Single<Boolean> {
        val sessionKey = "${session.id}, ${session.secret} WalletLink".sha256()

        return socket.joinSession(sessionKey, session.id)
            .flatMap { success ->
                if (!success) {
                    joinSessionEventsSubject.onNext(JoinSessionEvent(sessionId = session.id, joined = false))
                    return@flatMap Single.just(false)
                }

                return@flatMap setSessionConfig(session = session)
            }
            .map { success ->
                if (success) {
                    println("[walletlink] successfully joined session ${session.id}")

                    joinSessionEventsSubject.onNext(JoinSessionEvent(sessionId = session.id, joined = true))

                    return@map true
                } else {
                    println("[walletlink] Invalid session ${session.id}. Removing...")

                    sessionStore.delete(rpcUrl = url, sessionId = session.id)
                    joinSessionEventsSubject.onNext(JoinSessionEvent(sessionId = session.id, joined = false))

                    return@map false
                }
            }
            .logError()
    }

    private fun setSessionConfig(session: Session): Single<Boolean> {
        val encryptedMetadata = HashMap<String, String>()
        for ((key, value) in metadata) {
            try {
                encryptedMetadata[key.rawValue] = value.encryptUsingAES256GCM(secret = session.secret)
            } catch (err: Exception) {
                return Single.error(WalletLinkException.UnableToEncryptData)
            }
        }

        return socket.setSessionConfig(
            webhookId = userId,
            webhookUrl = notificationUrl,
            metadata = encryptedMetadata,
            sessionId = session.id
        )
    }

    // MARK: Request Handlers

    private fun handleIncomingRequest(request: ServerRequestDTO) {
        val signatureRequest = parseRequest(request) ?: return

        // FIXME: hish - delete this once UI is available
        when (signatureRequest) {
            is HostRequest.DappPermission -> Schedulers.io().scheduleDirect {
                val eth = metadata[ClientMetadataKey.ETHEREUM_ADDRESS] ?: return@scheduleDirect
                val response = Web3ResponseDTO(
                    id = signatureRequest.requestId.id,
                    result = listOf(eth.toLowerCase())
                )

                val json = response.asJsonString()
                val session = sessionStore.getSession(
                    id = request.sessionId,
                    url = signatureRequest.requestId.rpcUrl
                ) ?: return@scheduleDirect

                val encryptedData = json.encryptUsingAES256GCM(session.secret)
                socket.publishEvent(ResponseEventType.WEB3_RESPONSE, encryptedData, signatureRequest.requestId.sessionId)
                    .logError()
                    .subscribe()
            }
            else -> {
            }
        }

        requestsSubject.onNext(signatureRequest)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseRequest(request: ServerRequestDTO): HostRequest? {
        val session = sessionStore.getSession(id = request.sessionId, url = url) ?: return null

        try {
            val decrypted = request.data.decryptUsingAES256GCM(secret = session.secret)
            val jsonString = decrypted.toString(Charsets.UTF_8)
            val json = jsonString.asJsonMap() ?: return null

            when (request.event) {
                RequestEventType.WEB3_REQUEST -> {
                    val requestObject = json["request"] as? Map<String, Any> ?: return null
                    val requestMethodString = requestObject["method"] as? String ?: return null
                    val method = RequestMethod.fromRawValue(requestMethodString) ?: return null

                    return parseWeb3Request(request, method, jsonString)
                }
            }
        } catch (exception: Exception) {
            return null
        }
    }

    private inline fun <reified T> submitWeb3Response(response: Web3ResponseDTO<T>, session: Session): Single<Unit> {
        val json = response.asJsonString()
        val encryptedString: String

        try {
            encryptedString = json.encryptUsingAES256GCM(secret = session.secret)
        } catch (exception: Exception) {
            return Single.error(WalletLinkException.UnableToEncryptData)
        }

        return isConnectedObservable
            .filter { it }
            .takeSingle()
            .flatMap { socket.publishEvent(ResponseEventType.WEB3_RESPONSE, encryptedString, session.id) }
            .map { if (!it) throw WalletLinkException.UnableToEncryptData }
    }

    private fun parseWeb3Request(request: ServerRequestDTO, method: RequestMethod, json: String): HostRequest? {
        when (method) {
            RequestMethod.REQUEST_ETHEREUM_ADDRESS -> {
                val dto = Web3RequestDTO.fromJsonString<RequestEthereumAddressesParams>(json) ?: return null
                val requestId = hostRequestId(web3Request = dto, serverRequest = request)

                return HostRequest.DappPermission(requestId)
            }
            RequestMethod.SIGN_ETHEREUM_MESSAGE -> {
                val dto = Web3RequestDTO.fromJsonString<SignEthereumMessageParams>(json) ?: return null
                val params = dto.request.params
                val requestId = hostRequestId(web3Request = dto, serverRequest = request)

                return HostRequest.SignMessage(
                    requestId = requestId,
                    address = params.address,
                    message = params.message,
                    isPrefixed = params.addPrefix
                )
            }
            RequestMethod.SIGN_ETHEREUM_TRANSACTION -> {
                val dto = Web3RequestDTO.fromJsonString<SignEthereumTransactionParams>(json) ?: return null

                val weiValue = BigInteger(dto.request.params.weiValue)
                val params = dto.request.params
                val requestId = hostRequestId(web3Request = dto, serverRequest = request)

                return HostRequest.SignAndSubmitTx(
                    requestId = requestId,
                    fromAddress = params.fromAddress,
                    toAddress = params.toAddress,
                    weiValue = weiValue,
                    data = params.data.byteArrayUsingHexEncoding() ?: ByteArray(size = 0),
                    nonce = params.nonce,
                    gasPrice = params.gasPriceInWei.asBigInteger,
                    gasLimit = params.gasLimit.asBigInteger,
                    chainId = params.chainId,
                    shouldSubmit = params.shouldSubmit
                )
            }
            RequestMethod.SUBMIT_ETHEREUM_TRANSATION -> {
                val dto: Web3RequestDTO<SubmitEthereumTransactionParams> = JSON.fromJsonString(json) ?: return null
                val signedTx = dto.request.params.signedTransaction.byteArrayUsingHexEncoding() ?: return null
                val params = dto.request.params
                val requestId = hostRequestId(web3Request = dto, serverRequest = request)

                return HostRequest.SubmitSignedTx(requestId = requestId, signedTx = signedTx, chainId = params.chainId)
            }
        }
    }

    private fun <T> hostRequestId(web3Request: Web3RequestDTO<T>, serverRequest: ServerRequestDTO): HostRequestId {
        return HostRequestId(
            id = web3Request.id,
            sessionId = serverRequest.sessionId,
            eventId = serverRequest.eventId,
            rpcUrl = url,
            dappUrl = web3Request.origin,
            dappName = null
        )
    }

    // Observers

    private fun observeConnection() {
        var joinedSessionIds = HashSet<String>()
        val serialScheduler = Schedulers.single()
        val sessionChangesObservable = sessionStore.observeSessions(url)
            .distinctUntilChanged()
            .flatMap { sessions ->
                // If credentials list is not empty, try connecting to WalletLink server
                if (sessions.isNotEmpty()) {
                    return@flatMap startConnection().map { sessions }.onErrorReturn { sessions }.toObservable()
                }

                // Otherwise, disconnect
                return@flatMap stopConnection().map { sessions }.onErrorReturn { sessions }.toObservable()
            }

        Observables.combineLatest(isConnectedObservable, sessionChangesObservable)
            .debounce(300, TimeUnit.MILLISECONDS)
            .observeOn(serialScheduler)
            .flatMap { (isConnected, sessions) ->
                if (!isConnected) {
                    joinedSessionIds.clear()
                    return@flatMap Observable.just(Unit)
                }

                val currentSessionIds = HashSet<String>(sessions.map { it.id })

                // remove unlinked sessions
                joinedSessionIds = joinedSessionIds.filterTo(HashSet()) { currentSessionIds.contains(it) }

                val newSessions = sessions.filter { !joinedSessionIds.contains(it.id) }
                newSessions.forEach { joinedSessionIds.add(it.id) }

                joinSessions(sessions = newSessions).toObservable()
            }
            .onErrorReturn { Unit }
            .subscribe()
            .let { disposeBag.add(it) }
    }
}
