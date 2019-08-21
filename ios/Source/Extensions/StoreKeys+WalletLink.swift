// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import CBStore

extension StoreKeys {
    /// Store key to keeping track of WalletLink sessions
    static let sessions = KeychainStoreKey<SessionList>(
        "walletlink_session_list",
        accessible: .afterFirstUnlockThisDeviceOnly
    )
}
