// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

struct Web3RequestDTO<T: Codable>: Codable, JSONDeserializable {
    let id: String
    let origin: URL
    let request: Web3Request<T>
}

struct Web3Request<T: Codable>: Codable {
    let method: RequestMethod
    let params: T
}

struct RequestEthereumAddressesParams: Codable {
    let appName: String
}

struct SignEthereumMessageParams: Codable {
    let message: String
    let address: String
    let addPrefix: Bool
}

struct SignEthereumTransactionParams: Codable {
    let fromAddress: String
    let toAddress: String?
    let weiValue: String
    let data: String
    let nonce: Int?
    let gasPriceInWei: String?
    let gasLimit: String?
    let chainId: Int
    let shouldSubmit: Bool
}

struct SubmitEthereumTransactionParams: Codable {
    let signedTransaction: String
    let chainId: Int
}
