package com.electriccloud.client.api


import com.electriccloud.client.HttpClient
import groovy.json.JsonBuilder

import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.Method.GET
import static com.electriccloud.helpers.config.ConfigHelper.message

class KubernetesApi extends HttpClient {


    def baseUri
    def token
    def defaultHeaders() { ["Authorization": "Bearer ${getToken()}", Accept: "application/json"] }
    def json = new JsonBuilder()

    KubernetesApi(baseUri, token) {
        this.baseUri = baseUri
        this.token = token
        log.info("Connecting cluster endpoint ${this.baseUri}")
        log.info("Cluster access token: ${this.token}")
    }


    def getService(name, namespace = "default") {
        def uri = "api/v1/namespaces/${namespace}/services/${name}"
        request(baseUri, uri, GET, null, defaultHeaders(), null, false)
    }

    def getDeployment(name, namespace = "default") {
        def uri = "apis/apps/v1beta1/namespaces/${namespace}/deployments/${name}"
        request(baseUri, uri, GET, null, defaultHeaders(), null, false)
    }

    def getReplicaSets(namespace = "default"){
        def uri = "/apis/apps/v1/namespaces/${namespace}/replicasets"
        def resp = request(baseUri, uri, GET, null, defaultHeaders(), null, true)
        resp
    }

    def getServices(namespace = "default") {
        message("getting services")
        def uri = "api/v1/namespaces/${namespace}/services"
        def resp = request(baseUri, uri, GET, null, defaultHeaders(), null, false)
        log.info("Got services:")
        resp.json.items.forEach { log.info(" ${it.metadata.name} | type: ${it.spec.type} | clusterIP: ${it.spec.clusterIP} | externalIP: ${it.status.loadBalancer} | exposePort: ${it.spec.ports.port}") }
        resp
    }

    def getDeployments(namespace = "default") {
        def uri = "apis/apps/v1beta1/namespaces/${namespace}/deployments"
        def resp = request(baseUri, uri, GET, null, defaultHeaders(), null, false)
        log.info("Got deployments:")
        resp.json.items.forEach { log.info(" ${it.metadata.name} | pods: ${it.spec.replicas} | Available: ${it.status.availableReplicas}") }
        resp
    }

    def getPods(namespace = 'default') {
        def uri = "api/v1/namespaces/${namespace}/pods"
        def resp = request(baseUri, uri, GET, null, defaultHeaders(), null, false)
        log.info("Got pods:")
        resp.json.items.forEach { log.info("Pod Name: ${it.metadata.name} | Pod IP: ${it.status.podIP} | Host IP: ${it.status.hostIP}") }
        resp
    }


    def deleteDeployments(namespace = 'default') {
        def uri = "/apis/apps/v1beta1/namespaces/${namespace}/deployments"
        request(baseUri, uri, DELETE, null, defaultHeaders(), null, false)
    }

    def deleteDeployment(name, namespace='default') {
        def uri = "/apis/apps/v1beta1/namespaces/${namespace}/deployments/${name}"
        request(baseUri, uri, DELETE, null, defaultHeaders(), null, false)
    }

    def deletePods(namespace='default'){
        def uri = "api/v1/namespaces/${namespace}/pods"
        def resp = request(baseUri, uri, DELETE, null, defaultHeaders(), null, false)
        resp
    }

    def deletePod(name, namespace='default'){
        def uri = "api/v1/namespaces/${namespace}/pods/${name}"
        def resp = request(baseUri, uri, DELETE, null, defaultHeaders(), null, false)
        resp
    }

    def deleteReplicaSets(namespace = 'default'){
        def uri ="/apis/apps/v1/namespaces/${namespace}/replicasets"
        def resp = request(baseUri, uri, DELETE, null, defaultHeaders(), null, false)
        resp
    }

    def deleteReplicaSet(name, namespace='default'){
        def uri ="/apis/apps/v1/namespaces/${namespace}/replicasets/${name}"
        def resp = request(baseUri, uri, DELETE, null, defaultHeaders(), null, false)
        resp
    }


    def deleteService(name, namespace='default') {
        def uri = "api/v1/namespaces/${namespace}/services/${name}"
        request(baseUri, uri, DELETE, null, defaultHeaders(), null, true)
    }


}