// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

extension NSOrderedSet {
    /// Determine whether set is empty
    var isEmpty: Bool {
        let currentCount = count
        return currentCount == 0
    }

    /// Determine whether set is not empty
    var isNotEmpty: Bool {
        let currentCount = count
        return currentCount > 0
    }
}
