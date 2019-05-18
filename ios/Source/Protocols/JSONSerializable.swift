// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

/// Protocol representing a class that's serializable
protocol JSONSerializable {
    var asJSONDictionary: [String: Any]? { get }
    var asJSONString: String? { get }
}

/// Default implementation for Codable conformers
extension JSONSerializable where Self: Codable {
    var asJSONDictionary: [String: Any]? {
        do {
            let data = try JSONEncoder().encode(self)
            return data.jsonDictionary
        } catch {
            print("Unable to parse json dictionary for \(self) due to error \(error)")
            return nil
        }
    }

    var asJSONString: String? {
        do {
            let data = try JSONEncoder().encode(self)
            return String(data: data, encoding: .utf8)
        } catch {
            print("Unable to parse json string for \(self) due to error \(error)")
            return nil
        }
    }
}
