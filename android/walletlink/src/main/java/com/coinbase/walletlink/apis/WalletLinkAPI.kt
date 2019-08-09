package com.coinbase.walletlink.apis

import com.coinbase.wallet.core.extensions.asUnit
import com.coinbase.wallet.core.extensions.logError
import com.coinbase.wallet.http.Credentials
import com.coinbase.wallet.http.HTTP
import com.coinbase.wallet.http.HTTPService
import com.coinbase.walletlink.dtos.GetEventsDTO
import com.coinbase.walletlink.dtos.ServerRequestDTO
import com.coinbase.walletlink.extensions.create
import com.coinbase.walletlink.models.ServerMessageType
import io.reactivex.Single
import java.net.URL

internal class WalletLinkAPI {
    /**
     * Mark a given event as seen
     *
     * @param eventId The event ID
     * @param sessionId The session ID
     * @param secret The session secret
     *
     * @return A Single wrapping a ServerRequestDTO
     */
    fun markEventAsSeen(eventId: String, sessionId: String, secret: String, url: URL): Single<Unit> = HTTP
        .post(
            service = HTTPService(url),
            path = "/events/$eventId/seen",
            credentials = Credentials.create(sessionId = sessionId, secret = secret)
        )
        .asUnit()
        .logError()
        .onErrorReturn { Unit }

    /**
     * Fetch all unseen events
     *
     * @param sessionId The session ID
     * @param unseen If true, returns only unseen requests
     * @param sessionKey Generated session key
     *
     * @return A Single wrapping a list of encrypted host requests
     */
    fun getUnseenEvents(sessionId: String, secret: String, url: URL): Single<List<ServerRequestDTO>> = HTTP
        .get(
            service = HTTPService(url),
            path = "/events",
            credentials = Credentials.create(sessionId = sessionId, secret = secret),
            parameters = mapOf("unseen" to "true"),
            clazz = GetEventsDTO::class
        )
        .map { response ->
            val events = response.events ?: emptyList()
            events.map { event ->
                ServerRequestDTO(
                    sessionId = sessionId,
                    type = ServerMessageType.Event,
                    event = event.event,
                    eventId = event.id,
                    data = event.data
                )
            }
        }
}
