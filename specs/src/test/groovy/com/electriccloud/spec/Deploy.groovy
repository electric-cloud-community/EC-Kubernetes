package com.electriccloud.spec

import spock.lang.*


class Deploy extends KubeHelper {
    static def projectName = 'EC-Kubernetes Specs Deploy'
    static def clusterName = 'Kube Spec Cluster'
    static def envName = 'Kube Spec Env'
    static def configName = 'Kube Spec Config'

    def doSetupSpec() {
        configName = 'Kube Spec Config'
        createCluster(projectName, envName, clusterName, configName)
    }


    @Unroll
    def "deploy service #imageName, #imageVersion, capacity #defaultCapacity, #containerPort:#listenerPort"() {
        given:
        def serviceName = 'Kube Deploy Spec'
        dslFile "dsl/Deploy.dsl", [
            serviceName    : serviceName,
            projectName    : projectName,
            clusterName    : clusterName,
            envName        : envName,
            imageName      : imageName,
            imageVersion   : imageVersion,
            defaultCapacity: defaultCapacity,
            containerPort  : containerPort,
            listenerPort   : listenerPort,
            maxCapacity    : maxCapacity,
            minCapacity    : minCapacity
        ]
        when:
        def result = deployService(projectName, serviceName)
        then:
        logger.debug(result.logs)
        assert result.outcome == 'success'
        def deployedServiceName = serviceName.replaceAll(/\s+/, '-').toLowerCase()
        def service = getService(deployedServiceName)
        logger.debug(objectToJson(service))
        def deployment = getDeployment(deployedServiceName)
        logger.debug(objectToJson(deployment))

        assert service.spec.ports.size() == 1
        assert service.spec.ports[0].port.toString() == listenerPort
        assert service.spec.selector
        assert service.spec.selector['ec-svc']
        assert service.spec.clusterIP
        assert service.spec.sessionAffinity == 'None'

        def container = deployment.spec.template.spec.containers[0]

        def expectedDefaultCapacity = defaultCapacity ? defaultCapacity.toInteger() : 1
        assert deployment.spec.replicas == expectedDefaultCapacity
        assert container.image == "${imageName}:${imageVersion ?: 'latest'}"
        assert container.ports[0].containerPort.toString() == containerPort

        /*
        def strategy = deployment.spec.strategy.rollingUpdate
        assert strategy
        def expectedMaxSurge = maxCapacity ? maxCapacity - expectedDefaultCapacity : 1
        def expectedMaxUnavailable = minCapacity ? expectedDefaultCapacity - minCapacity : 1
        assert strategy.maxSurge == expectedMaxSurge
        assert strategy.maxUnavailable == expectedMaxUnavailable
        */
        cleanup:
        undeployService(projectName, serviceName)
        dsl """
                deleteService(
                    projectName: '$projectName',
                    serviceName: '$serviceName'
                )
            """
        where:
        imageName                | imageVersion | defaultCapacity | containerPort | listenerPort | maxCapacity | minCapacity
        'imagostorm/hello-world' | null         | '1'             | '80'          | '8080'       | '2'         | '1'
        'imagostorm/hello-world' | '1.0'        | '1'             | '80'          | '8080'       | '2'         | '1'
        'imagostorm/hello-world' | '1.0'        | '2'             | '81'          | '8081'       | '3'         | null
        'imagostorm/hello-world' | '2.0'        | null            | '81'          | '8081'       | '3'         | null

    }

    def "deploy canary"() {
        given: "first-time deploy runs"
        def serviceName = 'Kube Deploy Spec Canary'
        dslFile "dsl/Deploy.dsl", [
            serviceName    : serviceName,
            projectName    : projectName,
            clusterName    : clusterName,
            envName        : envName,
            imageName      : 'imagostorm/hello-world',
            imageVersion   : '1.0',
            defaultCapacity: '1',
            containerPort  : '8080',
            listenerPort   : '80',
        ]
        def result = deployService(projectName, serviceName)
        assert result.outcome == 'success'
        when: "canary deploy runs"
        dslFile "dsl/Deploy.dsl", [
            serviceName: serviceName,
            projectName: projectName,
            clusterName: clusterName,
            envName: envName,
            serviceMappingParameters: [
                canaryDeployment: 'true',
                numberOfCanaryReplicas: '1'
            ]
        ]
        result = deployService(projectName, serviceName)
        then:
        logger.debug(result.logs)
        def deployedServiceName = serviceName.replaceAll(/\s+/, '-').toLowerCase() + '-canary'
        def canaryDeployment = getDeployment(deployedServiceName)
        logger.debug(objectToJson(canaryDeployment))
        assert canaryDeployment.status.replicas == 1
        assert result.outcome == 'success'
        cleanup:
        dslFile "dsl/Deploy.dsl", [
            serviceName: serviceName,
            projectName: projectName,
            clusterName: clusterName,
            envName: envName,
            serviceMappingParameters: [
                canaryDeployment: 'false'
            ]
        ]
        undeployService(projectName, serviceName)
        dsl """
            deleteService(projectName:'$projectName', serviceName: '$serviceName')     
        """
    }

    def "update previously deployed service (compatibility with ec-track label)"() {
        given: "the service was already deployed without ec-track label"
        def serviceName = 'old-deployed-service'
        def container =  [
            name: 'nginx',
            image: 'nginx:1.10',
            ports: [[containerPort: 80]]
        ]
        def deployment = [
            kind: 'Deployment',
            metadata: [
                name: serviceName
            ],
            spec: [
                replicas: 2,
                template: [
                    spec: [
                        containers: [container]
                    ],
                    metadata: [
                        name: serviceName,
                        labels: [
                            'ec-svc': serviceName,
                            'ec-track': 'stable'
                        ]
                    ]
                ],
                selector: [
                    matchLabels: [
                        'ec-svc': serviceName
                    ]
                ]
            ]
        ]
        def service = [
            kind: 'Service',
            apiVersion: 'v1',
            metadata: [
                name: serviceName
            ],
            spec: [
                selector: [
                    'ec-svc': serviceName
                ],
                ports: [[protocol: 'TCP', port: 80, targetPort: 80]]
            ]
        ]

        createDeployment(getEndpoint(), getToken(), deployment)
        createService(getEndpoint(), getToken(), service)
        dslFile "dsl/Deploy.dsl", [
            serviceName    : serviceName,
            projectName    : projectName,
            clusterName    : clusterName,
            envName        : envName,
            imageName      : 'nginx',
            imageVersion   : '1.10',
            defaultCapacity: '2',
            containerPort  : '80',
            listenerPort   : '80',
        ]
        when:
        def result = deployService(projectName, serviceName)
        then:
        logger.debug(result.logs)
        def deploy = getDeployment(serviceName)
        def svc = getService(serviceName)
        logger.debug(objectToJson(deploy))
        logger.debug(objectToJson(svc))
        assert deploy.spec.selector.matchLabels['ec-track'] == 'stable'
        assert deploy.status.availableReplicas == 2
        def ip = svc.status.loadBalancer.ingress[0].ip
        def endpoint = "http://${ip}:80"
        def content = new URL(endpoint).text
        assert content =~ /Welcome to nginx/
        cleanup:
        undeployService(projectName, serviceName)
        dsl """
            deleteService(projectName: '$projectName', serviceName: '$serviceName')
        """
    }

    @Unroll
    @Ignore("Until deploy strategies are implemented")
    def "Rolling Deploy #minAvailabilityPercentage%:#minAvailabilityCount #maxRunningPercentage%:#maxRunningCount"() {
        given:
        def serviceName = 'Kube Deploy Spec'
        def imageName = 'imagostorm/hello-world'
        dslFile "dsl/Deploy.dsl", [
            serviceName             : serviceName,
            projectName             : projectName,
            clusterName             : clusterName,
            envName                 : envName,
            imageName               : imageName,
            containerPort           : '80',
            listenerPort            : '8080',
            serviceMappingParameters: [
                deploymentStrategy       : 'rollingDeployment',
                minAvailabilityPercentage: minAvailabilityPercentage,
                minAvailabilityCount     : minAvailabilityCount,
                maxRunningCount          : maxRunningCount,
                maxRunningPercentage     : maxRunningPercentage
            ]
        ]
        when:
        def result = deployService(projectName, serviceName)
        then:
        logger.debug(result.logs)
        def deployment = getDeployment(getServiceName(serviceName))
        logger.debug(objectToJson(deployment))
        def rollingUpdate = deployment.spec.strategy.rollingUpdate

        if (maxRunningCount) {
            assert rollingUpdate.maxSurge == maxRunningCount.toInteger()
        }
        if (maxRunningPercentage) {
            assert rollingUpdate.maxSurge == "${maxRunningPercentage.toInteger() + 100}%"
        }
        if (minAvailabilityPercentage) {
            def expectedUnavailable = 100 - minAvailabilityPercentage.toInteger()
            assert rollingUpdate.maxUnavailable == "${expectedUnavailable}%"
        }
        if (minAvailabilityCount) {
            def expectedUnavalilable = 1 - minAvailabilityCount.toInteger()
            assert rollingUpdate.maxUnavailable == expectedUnavalilable
        }
        cleanup:
        undeployService(projectName, serviceName)
        dsl """
                deleteService(
                    serviceName: '$serviceName',
                    projectName: '$projectName'
                )
            """
        where:
        minAvailabilityCount | minAvailabilityPercentage | maxRunningCount | maxRunningPercentage
        '1'                  | null                      | '2'             | null
        null                 | '10'                      | '2'             | null
        null                 | '10'                      | null            | '150'

    }

    def getServiceName(serviceName) {
        serviceName.replaceAll(/\s+/, '-').toLowerCase()
    }


    def doCleanupSpec() {
        cleanupCluster(configName)
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }


}
