package com.coinbase.walletlink.exceptions

class WebSocketException {
    class UnableToSendData : RuntimeException("Unable to send data/text on live websocket")
}
