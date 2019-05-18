// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBStore

extension StoreKeys {
    static let sessions = UserDefaultsStoreKey<[String]>("walletlink_sessions", syncNow: true)

    static func secret(for sessionId: String) -> KeychainStoreKey<String> {
        return KeychainStoreKey<String>("walletlink_secrets", uuid: sessionId, accessible: .whenUnlocked)
    }
}
