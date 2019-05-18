package com.coinbase.walletlink.error

class WebSocketException {
    class UnableToSendData : RuntimeException("Unable to send data/text on live websocket")
}