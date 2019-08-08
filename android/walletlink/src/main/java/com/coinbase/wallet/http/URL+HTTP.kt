package com.coinbase.wallet.http

import java.net.URL

fun URL.appendingPathComponent(component: String): URL {
    val filePieces = mutableListOf(this.file)
    val path = if (component.startsWith("/")) {
        component.substring(1)
    } else {
        component
    }

    if (!file.endsWith("/")) {
        filePieces.add("/")
    }

    filePieces.add(path)

    return URL(protocol, host, port, filePieces.joinToString(separator = ""), null)
}
