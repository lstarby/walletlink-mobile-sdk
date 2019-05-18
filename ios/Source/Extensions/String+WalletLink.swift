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

    /// Encrypt
    func encryptUsingAES256GCM(secret: String, iv: Data) throws -> String {
        guard
            let secretData = Data(base64Encoded: secret),
            let dataToEncrypt = self.data(using: .utf8),
            let (encryptedData, _) = try? AES256GCM.encrypt(
                data: dataToEncrypt,
                key: secretData,
                initializationVector: iv
            )
        else {
            throw WalletLinkError.unableToEncryptData
        }

        var mutableData = Data()
        mutableData.append(iv)
        mutableData.append(encryptedData)

        return mutableData.base64EncodedString()
    }
}
