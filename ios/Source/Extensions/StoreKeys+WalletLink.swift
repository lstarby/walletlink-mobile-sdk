// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBStore

extension StoreKeys {
    /// Store key to keeping track of WalletLink sessions
    static let sessions = KeychainStoreKey<SessionList>(
        "walletlink_session_list",
        accessible: .afterFirstUnlockThisDeviceOnly
    )
}
