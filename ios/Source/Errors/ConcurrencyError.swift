// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

enum ConcurrencyError: Error {
    case operationMissingReturnValue

    case operationCanceled
}
