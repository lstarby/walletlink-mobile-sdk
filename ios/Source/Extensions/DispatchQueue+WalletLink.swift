// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

extension DispatchQueue {
    /// Helper closure to run closure and get the result atomically
    func syncGet<T>(closure: (() -> T)) -> T {
        var value: T!

        sync { value = closure() }

        return value
    }
}
