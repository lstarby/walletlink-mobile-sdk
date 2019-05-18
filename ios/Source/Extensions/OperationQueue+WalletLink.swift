// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import RxSwift

extension OperationQueue {
    /// Add Single into an opertion qeueue
    ///
    /// - Parameters:
    ///     - single: Instance of single to execute as part of the operation queue
    ///
    /// - Returns: A single wrapping result based on the operation generic type
    func addSingle<T>(_ single: Single<T>) -> Single<T> {
        let operation = SingleConcurrentOperation(single: single)
        return operation.run(on: self)
    }
}

// MARK: - Private Single wrapping operation

private class SingleConcurrentOperation<T>: RxConcurrentOperation<T> {
    private let single: Single<T>

    required init(single: Single<T>) {
        self.single = single
    }

    override func main() {
        if isCancelled {
            result = .error(ConcurrencyError.operationCanceled)
            return
        }

        _ = single.subscribe(onSuccess: { result in
            self.result = .success(result)
        }, onError: { err in
            self.result = .error(err)
        })
    }
}
