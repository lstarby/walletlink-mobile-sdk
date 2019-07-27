// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBCore
import CBCrypto
import os.log

private let kAES256GCMIVSize = 12
private let kAES256GCMAuthTagSize = 16

extension String {
    /// Encrypt string using AES256 algorithm for given secret and iv
    ///
    ///     - Secret: Secret used to encrypt the data
    ///
    /// - Returns: The encrypted data
    /// - Throws: `WalletLinkError.unableToEncryptData` if unable to encrypt data
    func encryptUsingAES256GCM(secret: String) throws -> String {
        guard
            let secretData = secret.asHexEncodingData,
            let dataToEncrypt = self.data(using: .utf8),
            let iv = Data.randomBytes(kAES256GCMIVSize),
            let (encryptedData, authTag) = try? AES256GCM.encrypt(
                data: dataToEncrypt,
                key: secretData,
                initializationVector: iv
            )
        else {
            throw WalletLinkError.unableToEncryptData
        }

        var mutableData = Data()
        mutableData.append(iv)
        mutableData.append(authTag)
        mutableData.append(encryptedData)

        return mutableData.toHexString()
    }

    /// Decrypt string with AES256 GCM using provided secret
    func decryptUsingAES256GCM(secret: String) throws -> Data {
        guard
            let data = self.asHexEncodingData,
            let secretData = secret.asHexEncodingData,
            data.count > (kAES256GCMAuthTagSize + kAES256GCMIVSize)
        else {
            throw WalletLinkError.unableToDecryptData
        }

        let iv = data.subdata(in: 0 ..< kAES256GCMIVSize)
        let authTag = data.subdata(in: kAES256GCMIVSize ..< kAES256GCMAuthTagSize + kAES256GCMIVSize)
        let dataToDecrypt = data.subdata(in: kAES256GCMAuthTagSize + kAES256GCMIVSize ..< data.count)

        guard
            let decryptedData = try? AES256GCM.decrypt(
                data: dataToDecrypt,
                key: secretData,
                initializationVector: iv,
                authenticationTag: authTag
            )
        else {
            throw WalletLinkError.unableToDecryptData
        }

        return decryptedData
    }
}
