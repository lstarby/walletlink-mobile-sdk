// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import CBCore

struct Web3RequestDTO<T: Codable>: Codable, JSONDeserializable {
    let type: String = "WEB3_REQUEST"
    let id: String
    let origin: URL
    let request: Web3Request<T>
}

struct Web3Request<T: Codable>: Codable {
    let method: RequestMethod
    let params: T
}

struct RequestEthereumAccountsParams: Codable {
    let appName: String?
    let appLogoUrl: URL?
}

struct SignEthereumMessageParams: Codable {
    let message: String
    let address: String
    let addPrefix: Bool
    let typedDataJson: String?
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
