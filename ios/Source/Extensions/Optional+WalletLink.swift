// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import BigInt

extension Optional where Wrapped == String {
    var asBigInt: BigInt? {
        guard let value = self else { return nil }

        return BigInt(value)
    }
}
