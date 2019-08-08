package com.coinbase.walletlink.dtos

import com.coinbase.walletlink.models.EventType

internal data class GetEventsDTO(val events: List<EventDTO>, val timestamp: UInt, val error: String?)

internal data class EventDTO(val id: String, val event: EventType, val data: String)
