// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBStore

extension StoreKeys {
    /// Store key to keeping track of WalletLink sessions
    static let sessions = KeychainStoreKey<SessionList>("walletlink_sessions", accessible: .whenUnlocked)

    /// Store key to track when pending requests were last fetched for a session
    static func sessionLastRefreshed(sessionId: String) -> StoreKey<Date> {
        return UserDefaultsStoreKey<Date>("sessionLastRefreshed", uuid: sessionId, syncNow: true)
    }
}
