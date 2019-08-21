// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import CBCore

struct Web3RequestCanceledDTO: Codable, JSONDeserializable {
    let type: String = "WEB3_REQUEST_CANCELED"
    let id: String
    let origin: URL
}
