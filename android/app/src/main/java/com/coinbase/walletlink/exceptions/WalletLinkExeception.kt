package com.coinbase.walletlink.exceptions

class WalletLinkExeception {
    class unableToEncryptData : RuntimeException("Unable to encrypt data")

    class unableToDecryptData : RuntimeException("Unable to decrypt data")
}