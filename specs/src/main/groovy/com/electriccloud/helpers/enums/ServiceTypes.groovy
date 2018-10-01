package com.electriccloud.helpers.enums

class ServiceTypes {

    enum ServiceType {

        LOAD_BALANCER("LoadBalancer"),
        CLUSTER_IP("ClusterIP"),
        NODE_PORT("NodePort")

        String value

        ServiceType(value){
            this.value = value
        }


    }

}


