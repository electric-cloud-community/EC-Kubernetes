package com.electriccloud.kubernetes

import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

class Client {

    String endpoint
    String accessToken
    String kubernetesVersion


    static final Integer DEBUG = 1
    static final Integer INFO = 2
    static final Integer WARNING = 3
    static final Integer ERROR = 4

    static Integer logLevel = INFO

    Client(String endpoint, String accessToken, String version) {
        this.endpoint = endpoint
        this.kubernetesVersion = version
        this.accessToken = accessToken
    }

    Object doHttpRequest(Method method, String requestUri,
                         Object requestBody = null,
                         def queryArgs = null) {

        def http = new HTTPBuilder(this.endpoint)
        http.ignoreSSLIssues()
        def requestHeaders = [
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
                throw EcException
                    .code(ErrorCodes.UnknownError)
                    .location(this.getClass().getCanonicalName())
                    .message("Request for '$requestUri' failed with $resp.statusLine")
                    .build()
            }
        }
    }


    def getNamespaces() {
        def result = doHttpRequest(GET, "/api/v1/namespaces")
        result?.items
    }

    def getServices(String namespace) {
        def result = doHttpRequest(GET, "/api/v1/namespaces/${namespace}/services")
        result?.items
    }

    def getDeployments(String namespace, String labelSelector = null) {
        def query = [:]
        if (labelSelector) {
            query.labelSelector = labelSelector
        }
        def result = doHttpRequest(GET, "/apis/${versionSpecificAPIPath("deployments")}/namespaces/${namespace}/deployments", null, query)
        result?.items
    }

    def getPods(String namespace, String labelSelector = null) {
        def query = [:]
        if (labelSelector) {
            query.labelSelector = labelSelector
        }
        def result= doHttpRequest(GET, "/api/v1/namespaces/${namespace}/pods")
        result?.items
    }


    def getPod(String namespace, String podId) {
        def result = doHttpRequest(GET, "/api/v1/namespaces/${namespace}/pods/${podId}")
        result
    }

    def getPodMetrics(String namespace, String podId) {
        def result = doHttpRequest(GET, "/api/v1/namespaces/kube-system/services/http:heapster:/proxy/apis/metrics/v1alpha1/namespaces/${namespace}/pods/${podId}")
        result
    }

    def getContainerLogs(String namespace, String pod, String container) {
        def http = new HTTPBuilder(endpoint)
        http.ignoreSSLIssues()
        return http.request(GET, TEXT) { req ->
            uri.path = "/api/v1/namespaces/${namespace}/pods/${pod}/log"
            uri.query = [container: container, tailLines: 100]
            headers.Authorization = "Bearer ${this.accessToken}"
            headers.Accept = "application/json"

            response.success = { resp, reader ->
                String logs = reader.text
                logs
            }
            response.failure = { resp, reader ->
                throw EcException
                    .code(ErrorCodes.UnknownError)
                    .location(this.getClass().getCanonicalName())
                    .message("Request failed with $resp.statusLine: $reader")
                    .build()
            }

        }
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



    boolean isVersionGreaterThan17() {
        try {
            float version = Float.parseFloat(this.kubernetesVersion)
            version >= 1.8
        } catch (NumberFormatException ex) {
            logger WARNING, "Invalid Kubernetes version '$kubernetesVersion'"
            true
        }
    }

    boolean isVersionGreaterThan15() {
        try {
            float version = Float.parseFloat(this.kubernetesVersion)
            version >= 1.6
        } catch (NumberFormatException ex) {
            logger WARNING, "Invalid Kubernetes version '$kubernetesVersion'"
            // default to considering this > 1.5 version
            true
        }
    }


    String versionSpecificAPIPath(String resource) {
        switch (resource) {
            case 'deployments':
                return isVersionGreaterThan15() ? (isVersionGreaterThan17() ? 'apps/v1beta2' : 'apps/v1beta1') : 'extensions/v1beta1'
            default:
                throw EcException
                    .code(ErrorCodes.UnknownError)
                    .location(this.class.getCanonicalName())
                    .message("Unsupported resource '$resource' for determining version specific API path")
                    .build()
        }
    }

    static def logger(Integer level, def message) {
        if (level >= logLevel) {
            println getLogLevelStr(level) + message
        }
    }


}