// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

/// A size bounded cache. Oldest keys will be evicted.
final class BoundedCache<K: Hashable, V> {
    private var cache = ConcurrentCache<K, V>()
    private var keys = NSMutableOrderedSet()
    private let maxSize: Int

    /// Number of entries in cache
    var count: Int {
        return cache.count
    }

    /// Default Constructor
    required init(maxSize: Int) {
        self.maxSize = maxSize
    }

    /// Subscript setter/getter
    subscript(_ key: K) -> V? {
        get { return cache[key] }
        set {
            if let value = newValue {
                return insertOrUpdate(key: key, value: value)
            }

            delete(key: key)
        }
    }

    // MARK: - Private

    private func insertOrUpdate(key: K, value: V) {
        if cache[key] != nil {
            keys.remove(key)
        }

        cache[key] = value
        keys.add(key)

        if keys.count > maxSize, let oldestKey = keys.firstObject as? K {
            delete(key: oldestKey)
        }
    }

    private func delete(key: K) {
        keys.remove(key)
        cache[key] = nil
    }
}
