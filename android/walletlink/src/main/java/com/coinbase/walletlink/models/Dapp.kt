// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.models

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.NOCASE
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.coinbase.wallet.libraries.databases.interfaces.DatabaseModelObject
import java.net.URL

/**
 * Represents a WalletLink DApp
 *
 * @property id Unique identifier
 * @property url Dapp Origin URL
 * @property name Dapp name
 * @property logoURL Dapp logo URL
 */
@Entity(tableName = "Dapp", indices = [Index(value = ["url"], unique = true)])
data class Dapp internal constructor(
    @PrimaryKey
    @ColumnInfo(name = "id", collate = NOCASE)
    override val id: String,

    @ColumnInfo(name = "url")
    val url: URL,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "logoURL")
    val logoURL: URL?

) : DatabaseModelObject {
    constructor(
        url: URL,
        name: String?,
        logoURL: URL?
    ) : this(
        id = url.toString().toUpperCase(),
        url = url,
        name = name,
        logoURL = logoURL
    )
}
