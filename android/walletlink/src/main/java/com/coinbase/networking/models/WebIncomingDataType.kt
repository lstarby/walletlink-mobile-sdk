package com.coinbase.networking.models

// Represents data type received via web live connection
sealed class WebIncomingDataType

// Text message received from server
data class WebIncomingText(val string: String) : WebIncomingDataType()

// Data message received from server
data class WebIncomingData(val data: ByteArray) : WebIncomingDataType()