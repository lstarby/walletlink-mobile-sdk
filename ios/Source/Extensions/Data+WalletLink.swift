// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation
import os.log

extension Data {
    /// Generate random `Data` based on given length
    ///
    /// - Parameter:
    ///     - numberOfBytes: Size of random `Data` object to generate
    ///
    /// - Returns: Randomized bytes with given size encapsulated in a `Data` object
    static func randomBytes(_ numberOfBytes: Int) -> Data? {
        var randomBytes = [UInt8](repeating: 0, count: numberOfBytes)
        let status = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)

        if status != errSecSuccess { return nil }

        return Data(randomBytes)
    }

    /// Convert to JSON dictionary if possible
    var jsonDictionary: [String: Any]? {
        return jsonObject as? [String: Any]
    }

    /// Convert to JSON object if possible
    var jsonObject: Any? {
        do {
            return try JSONSerialization.jsonObject(with: self, options: [])
        } catch {
            os_log("exception: %@", type: .error, error.localizedDescription)
            return nil
        }
    }
}
