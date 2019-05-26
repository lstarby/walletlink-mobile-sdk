// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

/// A simple thread-safe cache.
final class ConcurrentCache<K: Hashable, V> {
    private let accessQueue = DispatchQueue(label: "WalletLink.ConcurrentCache", attributes: .concurrent)
    private var cache = [K: V]()

    /// Number of entries in cache
    var count: Int {
        return cache.count
    }

    /// Subscript setter/getter
    subscript(_ key: K) -> V? {
        get { return accessQueue.syncGet { cache[key] } }
        set { accessQueue.sync(flags: .barrier) { cache[key] = newValue } }
    }

    // Remove all entries
    func removeAll() {
        accessQueue.sync(flags: .barrier) { cache.removeAll() }
    }

    /// Helper to check if cache contains the given key
    func has(_ key: K) -> Bool {
        return self[key] != nil
    }

    // Get values
    var values: Dictionary<K, V>.Values {
        return accessQueue.syncGet { cache.values }
    }
}
