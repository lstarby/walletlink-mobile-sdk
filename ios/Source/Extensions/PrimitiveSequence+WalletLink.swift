// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import os.log
import RxSwift

extension PrimitiveSequence where Trait == RxSwift.SingleTrait {
    /// Helper method to return Single.just(())
    static func justVoid() -> Single<Void> {
        return .just(())
    }

    /// Maps the current observable to a `Single<Void>`
    func asVoid() -> PrimitiveSequence<SingleTrait, Void> {
        return map { _ -> Void in () }
    }
}

extension PrimitiveSequence {
    /// Log error if caught then rethrow
    func logError() -> PrimitiveSequence<Trait, Element> {
        return catchError { err in
            os_log("[walletlink] %@:%d Error %@", type: .error)

            throw err
        }
    }
}
