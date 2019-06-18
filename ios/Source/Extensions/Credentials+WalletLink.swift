// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBHTTP

extension Credentials {
    public init(sessionId: String, secret: String) {
        self.init(username: sessionId, password: Credentials.createSessionKey(sessionId: sessionId, secret: secret))
    }

    private static func createSessionKey(sessionId: String, secret: String) -> String {
        return "\(sessionId), \(secret) WalletLink".sha256()
    }
}
