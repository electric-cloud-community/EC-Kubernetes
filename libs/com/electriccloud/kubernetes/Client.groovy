package com.electriccloud.kubernetes

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.PATCH

class Client {

    String endpoint
    String accessToken

    static final Integer DEBUG = 1
    static final Integer INFO = 2
    static final Integer WARNING = 3
    static final Integer ERROR = 4

    static Integer logLevel = INFO

    Client(String endpoint, String accessToken) {
        this.endpoint = endpoint
        this.accessToken = accessToken
    }

    Object doHttpRequest(Method method, String requestUri,
                         Object requestBody = null,
                         def queryArgs = null) {

        def http = new HTTPBuilder(this.endpoint)
        http.ignoreSSLIssues()
        def requestHeaders = [
            'Content-Type': 'application/json',
            'Authorization': "Bearer ${this.accessToken}"
        ]

        http.request(method, JSON) {
            if (requestUri) {
                uri.path = requestUri
            }
            if (queryArgs) {
                uri.query = queryArgs
            }
            headers = requestHeaders
            body = requestBody

            response.success = { resp, json ->
                logger DEBUG, "request was successful $resp.statusLine.statusCode $json"
                json
            }

            response.failure = { resp, reader ->
                logger ERROR, "Response: $reader"
                throw new RuntimeException("Request failed with $resp.statusLine")
            }
        }
    }

    def getClusterDetails(String clusterName){
        def result = doHttpRequest(GET, "/apis/apiregistration.k8s.io/v1beta1/apiservices/${clusterName}")
        result?.items
    }

    def getNamespaces() {
        def result = doHttpRequest(GET, "/api/v1/namespaces")
        result?.items
    }

    def getNamespace(String namespace) {
        def result = doHttpRequest(GET, "/api/v1/namespaces/${namespace}")
        result?.items
    }

    def getServices(String namespace) {
        def result = doHttpRequest(GET, "/api/v1/namespaces/${namespace}/services")
        result?.items
    }

    def getService(String namespace, String clusterName){
        def result = doHttpRequest(GET, "/api/v1/namespaces/${namespace}/services/${clusterName}")
        resul?.items
    }

    def getPods() {

    }



    def static getLogLevelStr(Integer level) {
        switch (level) {
            case DEBUG:
                return '[DEBUG] '
            case INFO:
                return '[INFO] '
            case WARNING:
                return '[WARNING] '
            default://ERROR
                return '[ERROR] '

        }
    }

    static def logger(Integer level, def message) {
        if ( level >= logLevel ) {
            println getLogLevelStr(level) + message
        }
    }
}