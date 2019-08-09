package com.coinbase.walletlink.dtos

import com.coinbase.walletlink.models.EventType
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetEventsDTO(val events: List<EventDTO>?, val timestamp: Long, val error: String?)

@JsonClass(generateAdapter = true)
internal data class EventDTO(val id: String, val event: EventType, val data: String)
