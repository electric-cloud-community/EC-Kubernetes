package com.electriccloud.client

import groovy.json.JsonBuilder
import groovyx.net.http.HTTPBuilder
import org.apache.log4j.Logger
import static groovyx.net.http.ContentType.JSON



class HttpClient {

    public static Logger log = Logger.getLogger("appLogger")

    HTTPBuilder http

    HttpClient() {
        this.http = new HTTPBuilder()
    }

    def request = { baseUri, path, method, requestBody, requestHeaders, queryParams, debugMode = true ->
        http.ignoreSSLIssues()
        http.request("${baseUri}/${path}", method, JSON) { request ->
            if (requestHeaders) {
                headers = requestHeaders
            }
            if (requestBody) {
                body = requestBody
            }
            if (queryParams) {
                uri.query = queryParams
            }
            headers.Accept = JSON
            response.success = { resp, json ->
                if (debugMode == true) {
                    log.debug("Request successfully completed.")
                    log.debug("${resp.statusLine}")
                    log.debug( "json body: \n" + new JsonBuilder(json).toPrettyString())
                }
                [resp: resp,
                 json: json]
            }
            response.failure = { resp, reader ->
                log.debug(resp)
                log.debug(reader)
                log.debug( "json body: \n" + new JsonBuilder(reader).toPrettyString())
                throw new RuntimeException("Request failed", new Throwable(new JsonBuilder(reader).toPrettyString()))
            }
        }
    }




}