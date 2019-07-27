// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import os.log
import RxSwift

// FIXME: hish - move to core
extension PrimitiveSequence {
    /// Log error if caught then rethrow
    func logError() -> PrimitiveSequence<Trait, Element> {
        return catchError { err in
            print("[walletlink] caught error \(err)")

            throw err
        }
    }
}
