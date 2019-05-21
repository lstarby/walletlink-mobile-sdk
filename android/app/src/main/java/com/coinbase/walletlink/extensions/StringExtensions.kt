package com.coinbase.walletlink.extensions

import com.coinbase.walletlink.exceptions.WalletLinkExeception


/// Encrypt string using AES256 algorithm for given secret and iv
///
///     - Secret: Secret used to encrypt the data
///     - iv: Initialization vector. Acts as a salt
///
/// - Returns: The encrypted data
/// - Throws: An `WalletLinkError.unableToEncryptData` if unable to encrypt data
@Throws(WalletLinkExeception.unableToEncryptData::class)
fun String.encryptUsingAES256GCM(secret: String, iv: ByteArray): String {
    // FIXME: hish
    return ""
//    guard
//    let secretData = Data(base64Encoded: secret),
//    let dataToEncrypt = self.data(using: .utf8),
//    let (encryptedData, _) = try? AES256GCM.encrypt(
//        data: dataToEncrypt,
//        key: secretData,
//        initializationVector: iv
//        )
//        else {
//            throw WalletLinkError.unableToEncryptData
//        }
//
//        var mutableData = Data()
//        mutableData.append(iv)
//        mutableData.append(encryptedData)
//
//        return mutableData.base64EncodedString()
    }