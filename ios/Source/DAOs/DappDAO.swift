// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBDatabase
import RxSwift

final class DappDAO {
    private let database: Database

    required init(database: Database) {
        self.database = database
    }

    /// Insert or update dapp
    ///
    /// - Parameters:
    ///     - dapp: Dapp model to store
    ///
    /// - Returns: A Single indicating the save operation success or an exception is thrown
    func save(dapp: Dapp) -> Single<Void> {
        return database.addOrUpdate(dapp).asVoid()
    }

    /// Get Dapp details using origin URL
    ///
    /// - Parameters:
    ///     - url: Origin URL
    ///
    /// - Returns: A Single wrapping instance of dapp if found. Otherwise, a nil is returned
    func getDapp(url: URL) -> Single<Dapp?> {
        let predicate = NSPredicate(format: "url == [c] %@", url.absoluteString)

        return database.fetchOne(predicate: predicate)
    }
}
