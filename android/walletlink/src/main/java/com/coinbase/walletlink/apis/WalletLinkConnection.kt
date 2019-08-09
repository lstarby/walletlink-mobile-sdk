package com.coinbase.walletlink.apis

import com.coinbase.wallet.core.extensions.asUnit
import com.coinbase.wallet.core.extensions.logError
import com.coinbase.wallet.core.extensions.takeSingle
import com.coinbase.wallet.core.extensions.toPrefixedHexString
import com.coinbase.wallet.core.extensions.unwrap
import com.coinbase.wallet.core.extensions.zipOrEmpty
import com.coinbase.wallet.crypto.extensions.encryptUsingAES256GCM
import com.coinbase.wallet.http.Credentials
import com.coinbase.wallet.http.appendingPathComponent
import com.coinbase.wallet.http.connectivity.Internet
import com.coinbase.walletlink.dtos.Web3ResponseDTO
import com.coinbase.walletlink.dtos.asJsonString
import com.coinbase.walletlink.exceptions.WalletLinkException
import com.coinbase.walletlink.extensions.create
import com.coinbase.walletlink.models.ClientMetadataKey
import com.coinbase.walletlink.models.Dapp
import com.coinbase.walletlink.models.EventType
import com.coinbase.walletlink.models.HostRequest
import com.coinbase.walletlink.models.HostRequestId
import com.coinbase.walletlink.models.RequestMethod
import com.coinbase.walletlink.models.Session
import com.coinbase.walletlink.repositories.LinkRepository
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private data class JoinSessionEvent(val sessionId: String, val joined: Boolean)

internal class WalletLinkConnection private constructor(
    private val url: URL,
    private val userId: String,
    private val notificationUrl: URL,
    private val linkRepository: LinkRepository,
    private val metadata: ConcurrentHashMap<ClientMetadataKey, String>
) {
    private val requestsSubject = PublishSubject.create<HostRequest>()
    private val joinSessionEventsSubject = PublishSubject.create<JoinSessionEvent>()
    private val socket = WalletLinkWebSocket(url.appendingPathComponent("rpc"))
    private val disposeBag = CompositeDisposable()
    private val isConnectedObservable: Observable<Boolean> = socket.connectionStateObservable.map { it.isConnected }

    /**
     * Incoming WalletLink host requests
     */
    val requestsObservable: Observable<HostRequest> = requestsSubject.hide()

    init {
        socket.incomingRequestsObservable
            .flatMap { linkRepository.getHostRequest(it, url).toObservable() }
            .unwrap()
            .subscribe { requestsSubject.onNext(it) }
            .addTo(disposeBag)

        observeConnection()
    }

    constructor(
        url: URL,
        userId: String,
        notificationUrl: URL,
        linkRepository: LinkRepository,
        metadata: Map<ClientMetadataKey, String>
    ) : this(
        url = url,
        userId = userId,
        notificationUrl = notificationUrl,
        linkRepository = linkRepository,
        metadata = ConcurrentHashMap(metadata)
    )

    /**
     * Stop connection when WalletLink instance is deallocated
     */
    fun disconnect() {
        disposeBag.clear()
        stopConnection().subscribe()
    }

    /**
     * Connect to WalletLink server using parameters extracted from QR code scan
     *
     * @param sessionId WalletLink host generated session ID
     * @param secret WalletLink host/guest shared secret
     *
     * @return A single wrapping `Unit` if connection was successful. Otherwise, an exception is thrown
     */
    fun link(sessionId: String, secret: String): Single<Unit> {
        val session = linkRepository.getSession(id = sessionId, url = url)

        if (session != null && session.secret == secret) {
            return Single.just(Unit)
        }

        return isConnectedObservable
            .doOnSubscribe { linkRepository.saveSession(url = url, sessionId = sessionId, secret = secret) }
            .filter { it }
            .takeSingle()
            .flatMap { joinSessionEventsSubject.filter { it.sessionId == sessionId }.takeSingle() }
            .map { if (!it.joined) throw WalletLinkException.InvalidSession }
            .timeout(1500, TimeUnit.SECONDS) // FIXME: hish
            .logError()
            .onErrorResumeNext { exception ->
                linkRepository.delete(url, sessionId)
                throw exception
            }
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

        return linkRepository.getSessions(url)
            .mapNotNull { session ->
                try {
                    val encryptedValue = value.encryptUsingAES256GCM(secret = session.secret)
                    return@mapNotNull socket.setMetadata(key, encryptedValue, session.id).logError()
                } catch (err: WalletLinkException.UnableToEncryptData) {
                    return@mapNotNull null
                }
            }
            .zipOrEmpty()
            .asUnit()
    }

    /**
     * Send signature request approval to the requesting host
     *
     * @param requestId WalletLink host generated request ID
     * @param responseData Response data
     *
     * @return A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
     */
    fun approve(requestId: HostRequestId, responseData: ByteArray): Single<Unit> {
        val session = linkRepository.getSession(id = requestId.sessionId, url = url)
            ?: return Single.error(WalletLinkException.SessionNotFound)

        val markEventAsSeen = linkRepository.markAsSeen(requestId, url)

        return when (requestId.method) {
            RequestMethod.RequestEthereumAccounts -> {
                val address = responseData.toString(Charsets.UTF_8)
                val response = Web3ResponseDTO(
                    id = requestId.id,
                    method = requestId.method,
                    result = listOf(address.toLowerCase())
                )

                return markEventAsSeen
                    .flatMap { submitWeb3Response(response, session) }
                    .flatMap {
                        val dapp = Dapp(requestId.dappURL, requestId.dappName, requestId.dappImageURL)
                        linkRepository.saveDapp(dapp)
                    }
            }
            RequestMethod.SignEthereumMessage,
            RequestMethod.SubmitEthereumTransaction,
            RequestMethod.SignEthereumTransaction -> {
                val response = Web3ResponseDTO(
                    id = requestId.id,
                    method = requestId.method,
                    result = responseData.toPrefixedHexString()
                )

                return markEventAsSeen.flatMap { submitWeb3Response(response, session) }
            }
            RequestMethod.RequestCanceled -> {
                Single.error(WalletLinkException.UnsupportedRequestMethodApproval)
            }
        }
    }

    /**
     * Send signature request rejection to the requesting host
     *
     * @param sessionId WalletLink host generated request ID
     *
     * @return A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
     */
    fun reject(requestId: HostRequestId): Single<Unit> {
        val session = linkRepository.getSession(id = requestId.sessionId, url = url)
            ?: return Single.error(WalletLinkException.SessionNotFound)

        val response = Web3ResponseDTO<String>(
            id = requestId.id,
            method = requestId.method,
            errorMessage = "User rejected signature request"
        )

        return linkRepository.markAsSeen(requestId, url)
            .flatMap { submitWeb3Response(response, session = session) }
    }

    // Connection management

    private fun startConnection(): Single<Unit> {
        return Internet.statusChanges
            .filter { it.isOnline }
            .takeSingle()
            .flatMap { socket.connect() }
            .logError()
    }

    private fun stopConnection(): Single<Unit> {
        return socket.disconnect()
            .logError()
            .onErrorReturn { Single.just(Unit) }
    }

    // Session management

    private fun joinSessions(sessions: List<Session>): Single<Unit> = sessions
        .map { joinSession(it).asUnit().onErrorReturn { Unit } }
        .zipOrEmpty()
        .map { fetchPendingRequests() }
        .logError()

    private fun joinSession(session: Session): Single<Boolean> {
        val sessionKey = Credentials.create(session.id, session.secret).password

        return socket.joinSession(sessionKey, session.id)
            .flatMap { success ->
                if (!success) {
                    joinSessionEventsSubject.onNext(
                        JoinSessionEvent(
                            sessionId = session.id,
                            joined = false
                        )
                    )
                    return@flatMap Single.just(false)
                }

                return@flatMap setSessionConfig(session = session)
            }
            .map { success ->
                if (success) {
                    println("[walletlink] successfully joined session ${session.id}")

                    joinSessionEventsSubject.onNext(
                        JoinSessionEvent(
                            sessionId = session.id,
                            joined = true
                        )
                    )

                    return@map true
                } else {
                    println("[walletlink] Invalid session ${session.id}. Removing...")

                    linkRepository.delete(url = url, sessionId = session.id)
                    joinSessionEventsSubject.onNext(
                        JoinSessionEvent(
                            sessionId = session.id,
                            joined = false
                        )
                    )

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

    private fun fetchPendingRequests() {
        linkRepository.getSessions(url)
            .map { linkRepository.getPendingRequests(it, url) }
            .zipOrEmpty()
            .map { requests -> requests.flatMap { it } }
            .logError()
            .subscribeBy(onSuccess = { requests -> requests.forEach { requestsSubject.onNext(it) } })
            .addTo(disposeBag)
    }

    // Request Handlers

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
            .flatMap { socket.publishEvent(EventType.Web3Response, encryptedString, session.id) }
            .map { if (!it) throw WalletLinkException.UnableToSendSignatureRequestConfirmation }
    }

    // Observers

    private fun observeConnection() {
        var joinedSessionIds = HashSet<String>()
        val sessionSerialScheduler = Schedulers.single()
        val connSerialScheduler = Schedulers.single()

        val sessionChangesObservable = linkRepository.observeSessions(url)
            .distinctUntilChanged()
            .observeOn(connSerialScheduler)
            .flatMap { sessions ->
                // If credentials list is not empty, try connecting to WalletLink server
                if (sessions.isNotEmpty()) {
                    return@flatMap startConnection().map { sessions }.onErrorReturn { sessions }.toObservable()
                }

                // Otherwise, disconnect
                return@flatMap stopConnection().map { sessions }.onErrorReturn { sessions }.toObservable()
            }

        Observables.combineLatest(isConnectedObservable, sessionChangesObservable)
            .observeOn(sessionSerialScheduler)
            .debounce(300, TimeUnit.MILLISECONDS)
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
            .addTo(disposeBag)
    }
}
