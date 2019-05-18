// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import RxSwift

class RxConcurrentOperation<T>: ConcurrentOperation {
    var result: Result<T>? {
        didSet { state = .finished }
    }

    func run(on queue: OperationQueue) -> Single<T> {
        return Single<T>.create { single in
            self.completionBlock = {
                guard let result = self.result else {
                    return single(.error(ConcurrencyError.operationMissingReturnValue))
                }

                switch result {
                case let .success(value):
                    single(.success(value))
                case let .error(error):
                    single(.error(error))
                }
            }

            queue.addOperation(self)

            return Disposables.create()
        }
    }
}

extension RxConcurrentOperation {
    enum Result<T> {
        case success(T)
        case error(Error)
    }
}
