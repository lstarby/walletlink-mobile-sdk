package com.coinbase.walletlink.apis

import com.coinbase.walletlink.dtos.ServerRequestDTO
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.POST
import java.net.URL

internal class WalletLinkAPI { // TODO (private val http: WalletLinkHTTP) {
    fun markEventAsSeen(eventId: String, sessionId: String, secret: String, url: URL): Single<Unit> {
        // TODO: return http.markEventAsSeen(eventId, sessionId, secret, url)
        TODO()
    }

    fun getUnseenEvents(sessionId: String, secret: String, url: URL): Single<List<ServerRequestDTO>> {
        // TODO return http.getUnseenEvents(sessionId, secret, url)
        TODO()
    }
}

internal interface WalletLinkHTTP {
    /**
     * Mark a given event as seen
     *
     * @param eventId The event ID
     * @param sessionId The session ID
     * @param secret The session secret
     *
     * @return A Single wrapping a ServerRequestDTO
     */
    @POST("")
    fun markEventAsSeen(eventId: String, sessionId: String, secret: String, url: URL): Single<Unit>

//    {
//        let credentials = Credentials(sessionId: sessionId, secret: secret)
//
//        return HTTP.post(
//            service: HTTPService(url: url),
//        path: "/events/\(eventId)/seen",
//        credentials: credentials
//        )
//        .asVoid()
//            .logError()
//            .catchErrorJustReturn(())
//    }

    /**
     * Fetch all unseen events
     *
     * @param sessionId The session ID
     * @param unseen If true, returns only unseen requests
     * @param sessionKey Generated session key
     *
     * @return A Single wrapping a list of encrypted host requests
     */
    @GET("")
    fun getUnseenEvents(sessionId: String, secret: String, url: URL): Single<List<ServerRequestDTO>>
//    {
//        let credentials = Credentials(sessionId: sessionId, secret: secret)
//
//        return HTTP.get(
//            service: HTTPService(url: url),
//        path: "/events",
//        credentials: credentials,
//        parameters: ["unseen": "true"],
//        for: GetEventsDTO.self
//        )
//        .map { response in
//                response.body.events.map { event in
//                        ServerRequestDTO(
//                            sessionId: sessionId,
//                            type: .event,
//                    event: event.event,
//                    eventId: event.id,
//                    data: event.data
//                    )
//                }
//        }
//    }
}
