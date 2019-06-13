// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

/// A size bounded Set. Oldest keys will be evicted.
final class BoundedSet<T: Hashable> {
    private var set = NSMutableOrderedSet()
    private let maxSize: Int

    /// Number of entries in set
    var count: Int {
        return set.count
    }

    /// Default Constructor
    required init(maxSize: Int) {
        self.maxSize = maxSize
    }

    /// Get whether entry exists
    ///
    /// - Parameter key: Check if item exists in the set
    ///
    /// - Returns: True if has item
    func has(_ item: T) -> Bool {
        return set.contains(item)
    }

    /// Add item to the set
    ///
    /// - Parameter item: Item to add to the set
    func add(_ item: T) {
        if has(item) {
            set.remove(item)
        }

        set.add(item)

        while set.count > maxSize, set.isNotEmpty {
            set.removeObject(at: 0)
        }
    }
}
