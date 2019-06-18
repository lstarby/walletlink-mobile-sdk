// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import BigInt

extension ServerRequestDTO {
    func asHostRequest(secret: String, url: URL) -> HostRequest? {
        guard
            let decrypted = try? data.decryptUsingAES256GCM(secret: secret),
            let json = try? JSONSerialization.jsonObject(with: decrypted, options: []) as? [String: Any]
        else {
            assertionFailure("Invalid request \(self)")
            return nil
        }

        switch event {
        case .web3Request:
            guard
                let requestObject = json?["request"] as? [String: Any],
                let requestMethodString = requestObject["method"] as? String,
                let method = RequestMethod(rawValue: requestMethodString),
                let web3Request = parseWeb3Request(method: method, data: decrypted, url: url)
            else {
                assertionFailure("Invalid web3Request \(self)")
                return nil
            }

            return web3Request
        case .web3Response:
            print("[walletlink] ignore pending response")
            return nil
        }
    }

    private func parseWeb3Request(method: RequestMethod, data: Data, url: URL) -> HostRequest? {
        switch method {
        case .requestEthereumAddresses:
            guard let dto = Web3RequestDTO<RequestEthereumAddressesParams>.fromJSON(data) else {
                assertionFailure("Invalid requestEthereumAddresses \(self)")
                return nil
            }

            let requestId = hostRequestId(web3Request: dto, url: url)

            return .dappPermission(requestId: requestId)
        case .signEthereumMessage:
            guard let dto = Web3RequestDTO<SignEthereumMessageParams>.fromJSON(data) else {
                assertionFailure("Invalid signEthereumMessage \(self)")
                return nil
            }

            let params = dto.request.params
            let requestId = hostRequestId(web3Request: dto, url: url)

            return .signMessage(
                requestId: requestId,
                address: params.address,
                message: params.message,
                isPrefixed: params.addPrefix
            )
        case .signEthereumTransaction:
            guard
                let dto = Web3RequestDTO<SignEthereumTransactionParams>.fromJSON(data),
                let weiValue = BigInt(dto.request.params.weiValue)
            else {
                assertionFailure("Invalid signEthereumTransaction \(self)")
                return nil
            }

            let params = dto.request.params
            let requestId = hostRequestId(web3Request: dto, url: url)

            return .signAndSubmitTx(
                requestId: requestId,
                fromAddress: params.fromAddress,
                toAddress: params.toAddress,
                weiValue: weiValue,
                data: params.data.dataUsingHexEncoding() ?? Data(),
                nonce: params.nonce,
                gasPrice: params.gasPriceInWei.asBigInt,
                gasLimit: params.gasLimit.asBigInt,
                chainId: params.chainId,
                shouldSubmit: params.shouldSubmit
            )
        case .submitEthereumTransaction:
            guard
                let dto = Web3RequestDTO<SubmitEthereumTransactionParams>.fromJSON(data),
                let signedTx = dto.request.params.signedTransaction.dataUsingHexEncoding()
            else {
                assertionFailure("Invalid SubmitEthereumTransactionParams \(self)")
                return nil
            }

            let params = dto.request.params
            let requestId = hostRequestId(web3Request: dto, url: url)

            return .submitSignedTx(requestId: requestId, signedTx: signedTx, chainId: params.chainId)
        }
    }

    private func hostRequestId<T>(web3Request: Web3RequestDTO<T>, url: URL) -> HostRequestId {
        return HostRequestId(
            id: web3Request.id,
            sessionId: sessionId,
            eventId: eventId,
            url: url,
            dappUrl: web3Request.origin,
            dappName: nil
        )
    }
}
