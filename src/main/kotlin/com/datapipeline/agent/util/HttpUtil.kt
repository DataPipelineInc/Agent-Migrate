package com.datapipeline.agent.util

import com.datapipeline.agent.*
import io.netty.handler.codec.http.cookie.DefaultCookie
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientSession

fun sendRequest(
    config: Config,
    method: HttpMethod,
    uri: String,
    json: Any? = null,
    timeoutMs: Long = 10000L,
    validStatusCode: List<Int> = arrayListOf(200)
): HttpResponse<*> {
    val client = WebClient.create(VERTX)
    val session = WebClientSession.create(client)
    when (config) {
        Config.OLD_AGENT -> session.cookieStore().put(DefaultCookie("token", config.token))
        Config.DP -> session.addHeader("Authorization", dp_conf[DpConfSpec.token])
        else -> {}
    }
    if (method == HttpMethod.POST) {
        session.addHeader("Content-Type", "application/json")
        LOGGER.info { json?.toString() }
    }
    var response: HttpResponse<*>? = null
    var exception: Throwable? = null
    val request = session.request(method, config.port, config.host, uri)
        .timeout(timeoutMs)
    val future = when (method) {
        HttpMethod.GET -> request.send()
        HttpMethod.POST, HttpMethod.PUT -> request.sendJson(json)
        else -> throw Exception("Method [$method] not allowed.")
    }
    future
        .onSuccess { response = it }
        .onFailure { exception = it }
    while (future.isComplete.not()) {
        Thread.sleep(500L)
    }
    if (exception != null) {
        throw exception as Throwable
    }
    if (response == null) {
        throw Exception("Failed to get response.")
    } else {
        val resp = response!!
        LOGGER.info { "Type: [$config], Method: [$method], URI: [$uri], Body Content: [${resp.body()}]" }
        return resp.takeIf { validStatusCode.contains(it.statusCode()) }
            ?: throw Exception("Invalid status code: [${resp.statusCode()}], URI: [${uri}], Body content: [${resp.body()}]")
    }
}