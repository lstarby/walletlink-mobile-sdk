//
//  WalletLinkProtocol.swift
//  WalletLink
//
//  Created by Hish Bouabdallah on 5/19/19.
//  Copyright © 2019 Coinbase Inc. All rights reserved.
//

import RxSwift

public protocol WalletLinkProtocol: class {

    /// Incoming signature requests
    var signatureRequestObservable: Observable<SignatureRequest> { get }

    /// Constructor
    ///
    /// - Parameters:
    ///     -  url: WalletLink server URL
    ///     - metadata: client metadata forwarded to host once link is established
    init(url: URL, metadata: [ClientMetadataKey : String])

    /// Connect to WalletLink server using parameters extracted from QR code scan
    ///
    /// - Parameters:
    ///     - sessionId: WalletLink host generated session ID
    ///     - secret: WalletLink host/guest shared secret
    ///
    /// - Returns: A single wrapping `Void` if connection was successful. Otherwise, an exception is thrown
    func connect(sessionId: String, secret: String) -> Single<Void>

    /// Set metadata in all active sessions. This metadata will be forwarded to all the hosts
    ///
    /// - Parameters:
    ///   - key: Metadata key
    ///   - value: Metadata value
    ///
    /// - Returns: True if the operation succeeds
    func setMetadata(key: String, value: String) -> Single<Void>

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
