// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

struct Web3RequestCanceledDTO: Codable, JSONDeserializable {
    let type: String = "WEB3_REQUEST_CANCELED"
    let id: String
    let origin: URL
}
