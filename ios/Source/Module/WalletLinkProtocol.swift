// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import RxSwift

public protocol WalletLinkProtocol: class {
    /// Incoming host requests
    var requestsObservable: Observable<HostRequest> { get }

    /// Default required constructor
    ///
    /// - Parameters:
    ///     - userId: User ID to deliver push notifications to
    ///     - notificationUrl: Webhook URL used to push notifications to mobile client
    init(userId: String, notificationUrl: URL)

    /// Starts WalletLink connection with the server if a stored session exists. Otherwise, this is a noop. This method
    /// should be called immediately on app launch.
    ///
    /// - Parameters:
    ///     - metadata: client metadata forwarded to host once link is established
    func connect(metadata: [ClientMetadataKey: String])

    /// Disconnect from WalletLink server and stop observing session ID updates to prevent reconnection.
    func disconnect()

    /// Connect to WalletLink server using parameters extracted from QR code scan
    ///
    /// - Parameters:
    ///     - sessionId: WalletLink host generated session ID
    ///     - secret: WalletLink host/guest shared secret
    ///     - rpcUrl: WalletLink server websocket URL
    ///     - metadata: client metadata forwarded to host once link is established
    ///
    /// - Returns: A single wrapping `Void` if connection was successful. Otherwise, an exception is thrown
    func link(sessionId: String, secret: String, rpcUrl: URL, metadata: [ClientMetadataKey: String]) -> Single<Void>

    /// Set metadata in all active sessions. This metadata will be forwarded to all the hosts
    ///
    /// - Parameters:
    ///   - key: Metadata key
    ///   - value: Metadata value
    ///
    /// - Returns: A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
    func setMetadata(key: ClientMetadataKey, value: String) -> Single<Void>

    /// Send signature request approval to the requesting host
    ///
    /// - Parameters:
    ///     - requestId: WalletLink host generated request ID
    ///     - signedData: User signed data
    ///
    /// - Returns: A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
    func approve(requestId: HostRequestId, signedData: Data) -> Single<Void>

    /// Send signature request rejection to the requesting host
    ///
    /// - Parameters:
    ///     - requestId: WalletLink host generated request ID
    ///
    /// - Returns: A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
    func reject(requestId: HostRequestId) -> Single<Void>

    /// Get an event
    ///
    /// - Parameters:
    ///   - eventId: The event ID
    ///   - sessionId: The session ID
    ///   - rpcUrl: The RPC URL
    ///
    /// - Returns: A Single wrapping the HostRequest
    func getRequest(eventId: String, sessionId: String, rpcUrl: URL) -> Single<HostRequest>
}
