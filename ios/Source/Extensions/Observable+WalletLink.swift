// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import RxSwift

extension Observable {
    /// Take one entry from an observable and return it as a single
    func takeSingle() -> Single<Element> {
        return take(1).asSingle()
    }

    /// Helper method to return Single.just(())
    public static func justVoid() -> Observable<Void> {
        return Observable<Void>.just(())
    }
}

extension Observable where Element: OptionalType {
    /// Safe unwrap element. Note this will block the chain until a valid non-nil value is available
    func unwrap() -> Observable<Element.Wrapped> {
        return filter { $0.asOptional != nil }
            .map { element in
                guard let element = element.asOptional else { throw ObservableError.unableToUnwrap }
                return element
            }
    }
}

protocol OptionalType {
    associatedtype Wrapped
    var asOptional: Wrapped? { get }
}

extension Optional: OptionalType {
    var asOptional: Wrapped? { return self }
}

private enum ObservableError: Error {
    // shoud never be thrown. Needed to support `unwrap()` function above
    case unableToUnwrap
}
