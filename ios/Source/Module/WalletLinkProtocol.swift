// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import RxSwift

public protocol WalletLinkProtocol: class {
    /// Incoming signature requests
    var signatureRequestObservable: Observable<SignatureRequest> { get }

    /// Constructor
    ///
    /// - Parameters:
    ///     -  url: WalletLink server URL
    init(url: URL)

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
    ///
    /// - Returns: A single wrapping `Void` if connection was successful. Otherwise, an exception is thrown
    func link(sessionId: String, secret: String) -> Single<Void>

    /// Set metadata in all active sessions. This metadata will be forwarded to all the hosts
    ///
    /// - Parameters:
    ///   - key: Metadata key
    ///   - value: Metadata value
    ///
    /// - Returns: True if the operation succeeds
    func setMetadata(key: ClientMetadataKey, value: String) -> Single<Void>

    /// Send signature request approval to the requesting host
    ///
    /// - Parameters:
    ///     - requestId: WalletLink request ID
    ////    - signedData: User signed data
    ///
    /// - Returns: A single wrapping a `Void` if successful, or an exception is thrown
    func approve(requestId _: String, signedData _: Data) -> Single<Void>

    /// Send signature request rejection to the requesting host
    ///
    /// - Parameters:
    ///     - requestId: WalletLink request ID
    ///
    /// - Returns: A single wrapping a `Void` if successful, or an exception is thrown
    func reject(requestId _: String) -> Single<Void>
}
