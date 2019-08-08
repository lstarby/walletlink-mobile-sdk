package com.coinbase.walletlink

import androidx.room.Dao
import androidx.room.Database
import androidx.room.TypeConverters
import com.coinbase.wallet.libraries.databases.converters.BigDecimalConverter
import com.coinbase.wallet.libraries.databases.converters.BigIntegerConverter
import com.coinbase.wallet.libraries.databases.converters.UrlConverter
import com.coinbase.wallet.libraries.databases.db.RoomDatabaseProvider
import com.coinbase.wallet.libraries.databases.interfaces.DatabaseDaoInterface
import com.coinbase.walletlink.models.Dapp

@Database(entities = [Dapp::class], version = 1)
@Suppress("ArrayInDataClass")
@TypeConverters(
    BigIntegerConverter::class,
    BigDecimalConverter::class,
    UrlConverter::class
)
abstract class WalletLinkDatabase : RoomDatabaseProvider() {
    abstract val dappDao: DappRoomDao

    override fun modelMappings(): Map<Class<*>, DatabaseDaoInterface<*>> = mapOf(Dapp::class.java to dappDao)
}

@Dao
interface DappRoomDao : DatabaseDaoInterface<Dapp>
