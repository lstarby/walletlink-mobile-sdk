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
            print("[walletlink] caught error \(err)")

            throw err
        }
    }
}

extension PrimitiveSequence where Trait == RxSwift.SingleTrait {
    /// Retry Single on error using given delay.
    ///
    /// - Parameters:
    ///     - maxAttempts: Maximum number of times to attempt the sequence subscription.
    ///     - delay: Number of miliseconds to wait before firing the next retry attempt
    ///     - sceduler: Scheduler to run delay timers on.
    ///
    /// - Returns: Next sequence in the stream or error is thrown once maxAttempts is reached.
    func retry(
        _ maxAttempts: Int,
        delay: RxTimeInterval,
        scheduler: SchedulerType = ConcurrentDispatchQueueScheduler(qos: .userInitiated)
    ) -> PrimitiveSequence<SingleTrait, Element> {
        return retryWhen { errors in
            errors.enumerated().flatMap { attempt, error -> Observable<Void> in
                guard maxAttempts > attempt + 1 else { return .error(error) }

                return Observable<Int>.timer(delay, scheduler: scheduler).asVoid()
            }
        }
    }
}
