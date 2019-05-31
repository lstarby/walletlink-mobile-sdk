// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBCrypto
import os.log

private let hexadecimalCharacters = "0123456789abcdef"
private let kAES256GCMIVSize = 12
private let kAES256GCMAuthTagSize = 16

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
    ///
    /// - Returns: The encrypted data
    /// - Throws: `WalletLinkError.unableToEncryptData` if unable to encrypt data
    func encryptUsingAES256GCM(secret: String) throws -> String {
        guard
            let secretData = secret.dataUsingHexEncoding(),
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
            let data = self.dataUsingHexEncoding(),
            let secretData = secret.dataUsingHexEncoding(),
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

    /// Strip out "0x" prefix if one exists. Otherwise, no-op
    func strip0x() -> String {
        return starts(with: "0x") ? String(self[index(startIndex, offsetBy: 2)...]) : self
    }

    /// Convert to hex Data if possible
    func dataUsingHexEncoding() -> Data? {
        let strippedLowerStr = strip0x().lowercased()
        let str = strippedLowerStr.count % 2 == 0 ? strippedLowerStr : "0" + strippedLowerStr

        let length = str.count / 2
        var bytes = [UInt8](repeating: 0, count: length)

        for i in 0 ..< length {
            let hexLeft = str[str.index(str.startIndex, offsetBy: i * 2)]
            let hexRight = str[str.index(str.startIndex, offsetBy: i * 2 + 1)]
            guard let idxLeft = hexadecimalCharacters.index(of: hexLeft) else {
                return nil
            }
            guard let idxRight = hexadecimalCharacters.index(of: hexRight) else {
                return nil
            }
            let valLeft = hexadecimalCharacters.distance(from: hexadecimalCharacters.startIndex, to: idxLeft)
            let valRight = hexadecimalCharacters.distance(from: hexadecimalCharacters.startIndex, to: idxRight)
            bytes[i] = UInt8(valLeft * 16 + valRight)
        }
        return Data(bytes: bytes)
    }
}
