// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import os.log
import RxSwift

extension PrimitiveSequence {
    /// Log error if caught then rethrow
    func logError() -> PrimitiveSequence<Trait, Element> {
        return catchError { err in
            print("[walletlink] caught error \(err)")

            throw err
        }
    }
}
