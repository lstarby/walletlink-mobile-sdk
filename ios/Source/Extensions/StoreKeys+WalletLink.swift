// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBStore

extension StoreKeys {
    /// Store key to keeping track of WalletLink sessions
    static let sessions = KeychainStoreKey<SessionList>("walletlink_sessions", accessible: .whenUnlocked)

    /// Store key to track when pending requests were last fetched for a session
    static func requestsFetchToken(sessionId: String) -> StoreKey<UInt64> {
        return UserDefaultsStoreKey<UInt64>("requestsFetchToken", uuid: sessionId, syncNow: true)
    }
}
