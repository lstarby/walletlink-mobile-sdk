// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBCore
import CBHTTP
import RxSwift

private typealias WalletLinkCallback = (requestId: Int32, subject: ReplaySubject<ClientResponseDTO>)

/// Represents a WalletLink WebSocket
final class WalletLinkWebSocket {
    private let serialScheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "WalletLinkWebSocket.serial")
    private let connection: WebSocket
    private var disposeBag = DisposeBag()
    private var callbackSequence = AtomicInt32()
    private var incomingRequestsSubject = PublishSubject<ServerRequestDTO>()
    private var pendingCallbacks = BoundedCache<Int32, ReplaySubject<ClientResponseDTO>>(maxSize: 300)

    /// Incoming WalletLink requests
    let incomingRequestsObservable: Observable<ServerRequestDTO>

    /// WalletLink Connection state
    let connectionStateObservable: Observable<WebConnectionState>

    /// Constructor
    ///
    /// - Parameters:
    ///     - url: WalletLink WebSocket URL
    required init(url: URL) {
        let connection = WebSocket(url: url)

        self.connection = connection
        incomingRequestsObservable = incomingRequestsSubject.asObservable()
        connectionStateObservable = connection.connectionStateObservable
    }

    /// Connect to WalletLink server
    func connect() -> Single<Void> {
        disposeBag = DisposeBag()

        connection.incomingObservable
            .observeOn(serialScheduler)
            .subscribe(onNext: { [weak self] in self?.processIncomingData($0) })
            .disposed(by: disposeBag)

        return connection.connect()
    }

    /// Disconnect from WalletLink server
    func disconnect() -> Single<Void> {
        disposeBag = DisposeBag()
        return connection.disconnect()
    }

    /// Join a WalletLink session
    ///
    /// - Parameters:
    ///   - sessionKey: sha256(session+secret) hash
    ///   - sessionId: session ID scanned offline (QR code, NFC, etc)
    ///
    /// - Returns: A single wrapping `Boolean` to indicate operation was successful
    func joinSession(using sessionKey: String, for sessionId: String) -> Single<Bool> {
        let callback = createCallback()
        let message = JoinSessionMessageDTO(id: callback.requestId, sessionId: sessionId, sessionKey: sessionKey)

        return send(message, callback: callback)
    }

    /// Set metadata in the current session
    ///
    /// - Parameters:
    ///   - key: Metadata key on WalletLink server
    ///   - value: Metadata value stored on WalletLink server. This is encrypted.
    ///   - sessionId: Session ID scanned offline (QR code, NFC, etc)
    ///
    /// - Returns: A single wrapping `Boolean` to indicate operation was successful
    func setMetadata(key: ClientMetadataKey, value: String, for sessionId: String) -> Single<Bool> {
        let callback = createCallback()
        let key = key.rawValue
        let message = SetMetadataMessageDTO(id: callback.requestId, sessionId: sessionId, key: key, value: value)

        return send(message, callback: callback)
    }

    /// Set session config once a link is established
    ///
    /// - Parameters:
    ///   - webhookId: Webhook ID used to push notifications to mobile client
    ///   - webhookUrl: Webhook URL used to push notifications to mobile client
    ///   - metadata: Metadata forwarded to host
    ///   - sessionId: Session ID scanned offline (QR code, NFC, etc)
    ///
    /// - Returns: A single wrapping `Boolean` to indicate operation was successful
    func setSessionConfig(
        webhookId: String,
        webhookUrl: URL,
        metadata: [String: String],
        for sessionId: String
    ) -> Single<Bool> {
        let callback = createCallback()
        let message = SetSessionConfigMessageDTO(
            id: callback.requestId,
            sessionId: sessionId,
            webhookId: webhookId,
            webhookUrl: webhookUrl.absoluteString,
            metadata: metadata
        )

        return send(message, callback: callback)
    }

    /// Publish new event to WalletLink server
    ///
    /// - Parameters:
    ///   - event: Event type to publish
    ///   - data: The encrypted data sent to host
    ///   - sessionId: Session ID scanned offline (QR code, NFC, etc)
    ///
    /// - Returns: A single wrapping `Boolean` to indicate operation was successful
    func publishEvent(_ event: EventType, data: String, to sessionId: String) -> Single<Bool> {
        let callback = createCallback()
        let message = PublishEventDTO(id: callback.requestId, sessionId: sessionId, event: event, data: data)

        return send(message, callback: callback)
    }

    // MARK: - Send message helper(s)

    private func send(_ message: JSONSerializable, callback: WalletLinkCallback) -> Single<Bool> {
        guard let jsonString = message.asJSONString else { return .error(WalletLinkError.unableToSerializeMessageJSON) }

        return Internet.statusChanges
            .filter { $0.isOnline }
            .takeSingle()
            .flatMap { _ in self.connection.sendString(jsonString) }
            .flatMap { callback.subject.takeSingle() }
            .map { $0.type.isOK }
            .retry(3, delay: 1)
            .timeout(15, scheduler: ConcurrentDispatchQueueScheduler(qos: .userInitiated))
            .logError()
            .map { success in
                self.pendingCallbacks[callback.requestId] = nil
                return success
            }
            .catchError { err in
                self.pendingCallbacks[callback.requestId] = nil
                throw err
            }
    }

    // MARK: - Pending requests callback management

    private func createCallback() -> WalletLinkCallback {
        let requestId = callbackSequence.incrementAndGet()
        let subject = ReplaySubject<ClientResponseDTO>.create(bufferSize: 1)

        pendingCallbacks[requestId] = subject

        return WalletLinkCallback(requestId: requestId, subject: subject)
    }

    /// This is called when the host sends a response to a request initiated by the signer
    private func receivedClientResponse(_ response: ClientResponseDTO) {
        guard let requestId = response.id else { return assertionFailure("Invalid message response \(response)") }

        let subject = pendingCallbacks[requestId]

        pendingCallbacks[requestId] = nil

        subject?.onNext(response)
    }

    /// this is called when the host sends a request initiated by the host
    private func receivedServerRequest(_ request: ServerRequestDTO) {
        incomingRequestsSubject.onNext(request)
    }

    private func processIncomingData(_ incoming: WebIncomingDataType) {
        guard
            case let .text(jsonString) = incoming,
            let json = jsonString.jsonObject as? [String: Any],
            let typeString = json["type"] as? String,
            let type = ServerMessageType(rawValue: typeString)
        else {
            return assertionFailure("Unknown WalletLink type \(incoming)")
        }

        switch type {
        case .ok, .fail, .publishEventOK:
            guard let response = ClientResponseDTO.fromJSONString(jsonString) else {
                return assertionFailure("[walletlink] Invalid client response \(jsonString)")
            }

            receivedClientResponse(response)
        case .event:
            guard let request = ServerRequestDTO.fromJSONString(jsonString) else {
                return assertionFailure("[walletlink] Invalid server request \(jsonString)")
            }

            receivedServerRequest(request)
        }
    }
}
