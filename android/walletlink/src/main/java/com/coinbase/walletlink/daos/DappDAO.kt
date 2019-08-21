// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.daos

import com.coinbase.wallet.core.extensions.asUnit
import com.coinbase.wallet.core.util.Optional
import com.coinbase.wallet.libraries.databases.db.Database
import com.coinbase.walletlink.models.Dapp
import io.reactivex.Single
import java.net.URL

class DappDAO(private val database: Database<*>) {
    /**
     * Insert or update dapp
     *
     * @param dapp Dapp model to store
     *
     * @return A Single indicating the save operation success or an exception is thrown
     */
    fun save(dapp: Dapp): Single<Unit> = database.addOrUpdate(dapp).asUnit()

    /**
     * Get Dapp details using origin URL
     *
     * @param url Origin URL
     *
     * @returns A Single wrapping instance of dapp if found. Otherwise, a nil is returned
     */
    fun getDapp(
        url: URL
    ): Single<Optional<Dapp>> = database.fetchOne("SELECT * FROM Dapp where url = ?", url.toString())
}
