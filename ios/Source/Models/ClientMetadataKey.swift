// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import Foundation

public enum ClientMetadataKey: String {
    /// Client ethereum address metadata key
    case ethereumAddress = "EthereumAddress"

    /// Flag to destroy secure session on both client and host side
    case destroyed = "__destroyed"
}
