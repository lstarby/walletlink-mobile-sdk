// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBCrypto
import os.log

extension String {
    /// Convert to JSON object if possible
    var jsonObject: Any? {
        do {
            guard let data = self.data(using: .utf8) else { return nil }
            return try JSONSerialization.jsonObject(with: data, options: [])
        } catch let e {
            os_log("exception: %@", type: .error, e.localizedDescription)
            return nil
        }
    }

    /// Encrypt string using AES256 algorithm for given secret and iv
    ///
    ///     - Secret: Secret used to encrypt the data
    ///     - iv: Initialization vector. Acts as a salt
    ///
    /// - Returns: The encrypted data
    /// - Throws: `WalletLinkError.unableToEncryptData` if unable to encrypt data
    func encryptUsingAES256GCM(secret: String, iv: Data) throws -> String {
        let secretData = Data(hex: secret)

        guard
            let dataToEncrypt = self.data(using: .utf8),
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

    func decryptUsingAES256GCM(secret: String) throws -> Data {
        let data = Data(hex: self)
        let secretData = Data(hex: secret)
        let iv = data.subdata(in: 0..<12)
        let authTag = data.subdata(in: 12..<28)
        let dataToDecrypt = data.subdata(in: 28..<data.count)

        // FIXME: hish - make sure to check for out of index

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

extension Data {
    func subdata(in range: ClosedRange<Index>) -> Data {
        return subdata(in: range.lowerBound ..< range.upperBound + 1)
    }
}
