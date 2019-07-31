// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBDatabase

struct Dapp: DatabaseModelObject {
    /// Unique ID
    let id: String

    /// Dapp Origin URL
    let url: URL

    /// Dapp name
    let name: String?

    /// Dapp logo URL
    let logoURL: URL?

    init(url: URL, name: String?, logoURL: URL?) {
        self.url = url
        self.name = name
        self.logoURL = logoURL
        id = url.absoluteString.uppercased()
    }
}
