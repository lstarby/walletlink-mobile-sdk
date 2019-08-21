// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import CBHTTP

extension Credentials {
    init(sessionId: String, secret: String) {
        self.init(username: sessionId, password: Credentials.createSessionKey(sessionId: sessionId, secret: secret))
    }

    private static func createSessionKey(sessionId: String, secret: String) -> String {
        return "\(sessionId), \(secret) WalletLink".sha256()
    }
}
