// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

struct Web3ResponseDTO<T: Codable>: Codable, JSONSerializable {
    let type: String = "WEB3_RESPONSE"
    let id: String
    let response: Web3Response<T>

    init(id: String, result: T) {
        self.id = id
        response = Web3Response<T>(result: result, errorMessage: nil)
    }

    init(id: String, errorMessage: String) {
        self.id = id
        response = Web3Response<T>(result: nil, errorMessage: errorMessage)
    }
}

struct Web3Response<T: Codable>: Codable {
    let result: T?
    let errorMessage: String?
}
