package com.coinbase.walletlink.jsonadapters

import com.coinbase.walletlink.dtos.Web3RequestDTO
import com.squareup.moshi.FromJson

// internal class Web3RequestDTOAdapterAdapter : JsonAdapter<Web3RequestDTO<*>>() {
//    @FromJson
//    @Throws(IOException::class)
//    override fun fromJson(reader: JsonReader): Web3RequestDTO<*>? {
//        if (reader.peek() == JsonReader.Token.NULL) return reader.nextNull()
//        return BigDecimal(reader.nextString())
//    }
//
//    @ToJson
//    override fun toJson(writer: JsonWriter, value: Web3RequestDTO<*>?) {
//        if (value == null) writer.value(null as String?)
//        else {
//            writer.va
//            writer.value()
//        }
//    }
// }

internal class Web3RequestDTOAdapterAdapter {
    @FromJson
    fun fromJson(json: String): Web3RequestDTO<*>? {

        return null
    }
}

// @FromJson fun fromJson(item: ScheduleItem.ScheduleItemJson): ScheduleItem<Any> {
//    val moshi: Moshi = Moshi.Builder().build()
//    val json = moshi.adapter(Map::class.java).toJson(item.item as Map<*, *>)
//
//    return ScheduleItem(
//        item.date,
//        item.type,
//        item.schedule_item_groups ?: listOf(),
//        when (item.type) {
//            ItemType.GAME -> moshi.adapter(GameItem::class.java).fromJson(json)
//            ItemType.EVENT -> moshi.adapter(EventItem::class.java).fromJson(json)
//            ItemType.CHECK_IN, ItemType.CHECK_OUT ->
//                moshi.adapter(ReservationItem::class.java)
//                    .fromJson(json).apply { this!!.type = item.type }
//            else -> ScheduleItem.NullItem()
//        }!!
//    )
// }

//
// internal data class Web3RequestDTO<T>(
//    val id: String,
//    val origin: URL,
//    val request: Web3Request<T>
// ) : JsonSerializable {
//    override fun asJsonString(): String = JSON.toJsonString(this)
// }
//
// internal data class Web3Request<T>(val method: RequestMethod, val params: T)
//
// internal data class RequestEthereumAddressesParams(val appName: String)
//
// internal data class SignEthereumMessageParams(val message: String, val address: String, val addPrefix: Boolean)
//
// internal data class SignEthereumTransactionParams(
//    val fromAddress: String,
//    val toAddress: String?,
//    val weiValue: String,
//    val data: String,
//    val nonce: Int?,
//    val gasPriceInWei: String?,
//    val gasLimit: String?,
//    val chainId: Int,
//    val shouldSubmit: Boolean
// )
//
// internal data class SubmitEthereumTransactionParams(val signedTransaction: String, val chainId: Int)

// https://stackoverflow.com/questions/48913616/android-and-moshi-adapter-with-generic-type
