import groovy.json.JsonSlurper
import spock.lang.*
import com.electriccloud.spec.*

class Discover extends KubeHelper {
    static def projectName = 'EC-Kubernetes Specs Discover'
    static def clusterName = 'Kube Spec Cluster'
    static def envName = 'Kube Spec Env'
    static def serviceName = 'kube-spec-discovery-test'
    static def configName
    static def secretName

    def doSetupSpec() {
        configName = 'Kube Spec Config'
        createCluster(projectName, envName, clusterName, configName)
        dslFile 'dsl/Discover.dsl', [
            projectName: projectName,
            params     : [
                envName                         : '',
                envProjectName                  : '',
                clusterName                     : '',
                namespace                       : '',
                projName                        : '',
                ecp_kubernetes_applicationScoped: '',
                ecp_kubernetes_applicationName  : '',
                ecp_kubernetes_apiEndpoint      : '',
                ecp_kubernetes_apiToken         : '',
            ]
        ]
    }

    def doCleanupSpec() {
        cleanupCluster(configName)
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }

    def "create application-scoped services"() {
        given:
        def sampleName = 'nginx-spec-application'
        cleanupService(sampleName)
        deploySample(sampleName)
        def applicationName = "Discovered Application Spec"
        when:
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Discover',
                actualParameter: [
                    clusterName: '$clusterName',
                    namespace: 'default',
                    envProjectName: '$projectName',
                    envName: '$envName',
                    projName: '$projectName',
                    ecp_kubernetes_applicationScoped: 'true',
                    ecp_kubernetes_applicationName: '$applicationName'
                ]
            )
        """
        then:
        logger.debug(result.logs)
        def application = dsl "getApplication(projectName: '$projectName', applicationName: '$applicationName')"
        logger.debug(objectToJson(application))
        assert application.application?.applicationId
        def services = dsl "getServices(projectName: '$projectName', applicationName: '$applicationName')"
        logger.debug(objectToJson(services))
        assert services.service
        cleanup:
        cleanupService(sampleName)
        dsl "deleteApplication(projectName:'$projectName', applicationName: '$applicationName')"
    }

    def "create environment from scratch"() {
        given:
        def sampleName = 'nginx-spec-scratch'
        cleanupService(sampleName)
        deploySample(sampleName)
        def token = System.getenv('KUBE_TOKEN')
        assert token
        def endpoint = System.getenv('KUBE_ENDPOINT')
        assert endpoint
        when:
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Discover',
                actualParameter: [
                    clusterName: 'Created Cluster',
                    namespace: 'default',
                    envProjectName: '$projectName',
                    envName: 'Spec Kubernetes Created Env',
                    projName: '$projectName',
                    ecp_kubernetes_apiEndpoint: '$endpoint',
                    ecp_kubernetes_apiToken: '$token'
                ]
            )
        """
        then:
        logger.debug(result.logs)
        assert result.outcome != 'error'
        def environment = dsl "getEnvironment(projectName: '$projectName', environmentName: 'Spec Kubernetes Created Env')"
        assert environment.environment
        cleanup:
        cleanupService(serviceName)
        dsl "deleteEnvironment(projectName: '$projectName', environmentName: 'Spec Kubernetes Created Env')"
    }

    def "discover sample"() {
        given:
        def sampleName = 'nginx-spec'
        cleanupService(sampleName)
        deploySample(sampleName)
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Discover',
                    actualParameter: [
                        clusterName: '$clusterName',
                        namespace: 'default',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        projName: '$projectName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def service = getService(
            projectName,
            sampleName,
            clusterName,
            envName
        )
        logger.debug(objectToJson(service))
        assert result.outcome != 'error'
        assert service.service
        def containers = service.service.container
        assert containers.size() == 1
        assert containers[0].containerName == 'nginx'
        assert containers[0].imageName == 'nginx'
        assert containers[0].imageVersion == '1.10'
        def port = containers[0].port[0]
        assert port
        assert service.service.defaultCapacity == '1'
        assert port.containerPort == '80'
        assert service.service.port[0].listenerPort == '80'
        def env = service.service.container[0].environmentVariable
        assert env.size() == 1
        assert env[0].value == 'TEST'
        assert env[0].environmentVariableName == 'TEST_ENV'

        def volumesJson = service.service.volumes
        def volumes = new JsonSlurper().parseText(volumesJson)
        assert volumes[0].name == 'my-volume'
        assert volumes[0].hostPath

        def volumeMountsJson = service.service.container[0].volumeMounts
        def volumeMounts = new JsonSlurper().parseText(volumeMountsJson)
        assert volumeMounts[0].name == 'my-volume'
        assert volumeMounts[0].mountPath
        cleanup:
        cleanupService(serviceName)
    }

    def "discover load balancer IP"() {
        given:
        def serviceName = 'kube-spec-load-balancer-ip'
        cleanupService(serviceName)
        deployWithLoadBalancer(serviceName)
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Discover',
                    actualParameter: [
                        clusterName: '$clusterName',
                        namespace: 'default',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        projName: '$projectName'
                    ]
                )
            """
        then:
        def service = getService(
            projectName,
            serviceName,
            clusterName,
            envName
        )
        logger.debug(objectToJson(service))
        def kubeService = getService(serviceName)
        logger.debug(objectToJson(kubeService))
        assert getParameterDetail(service.service, 'loadBalancerIP').parameterValue == '35.224.8.81'
        assert getParameterDetail(service.service, 'serviceType').parameterValue == 'LoadBalancer'
        cleanup:
        cleanupService(serviceName)
    }

    def "Liveness/readiness probe"() {
        given:
        def serviceName = 'kube-spec-liveness'
        cleanupService(serviceName)
        deployLiveness(serviceName)
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Discover',
                    actualParameter: [
                        clusterName: '$clusterName',
                        namespace: 'default',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        projName: '$projectName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def service = getService(
            projectName,
            serviceName,
            clusterName,
            envName
        )
        logger.debug(objectToJson(service))

        def container = service.service.container.first()
        assert getParameterDetail(container, 'livenessHttpProbeHttpHeaderName')
        assert getParameterDetail(container, 'livenessHttpProbeHttpHeaderValue')
        assert getParameterDetail(container, 'livenessHttpProbePath')
        assert getParameterDetail(container, 'livenessHttpProbePort')
        assert getParameterDetail(container, 'livenessInitialDelay')
        assert getParameterDetail(container, 'livenessPeriod')
        assert getParameterDetail(container, 'readinessInitialDelay')
        assert getParameterDetail(container, 'readinessPeriod')
        assert getParameterDetail(container, 'readinessCommand')
        assert container.command
        cleanup:
        cleanupService(serviceName)
    }

    def "Discover secrets"() {
        given:
        cleanupService(serviceName)
        secretName = deployWithSecret(serviceName)
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Discover',
                    actualParameter: [
                        clusterName: '$clusterName',
                        namespace: 'default',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        projName: '$projectName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def service = getService(
            projectName,
            serviceName,
            clusterName, envName
        )
        logger.debug(objectToJson(service))
        assert service.service.container.size() == 1
        assert service.service.container[0].imageName == 'imagostorm/hello-world'
        assert service.service.container[0].credentialName
        cleanup:
        cleanupService(serviceName)
        deleteSecret(secretName)
    }

    def "Percentage in surge/maxUnavailable"() {
        given:
        def serviceName = 'kube-spec-service-percentage'
        cleanupService(serviceName)
        deployWithPercentage(serviceName)
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Discover',
                    actualParameter: [
                        clusterName: '$clusterName',
                        namespace: 'default',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        projName: '$projectName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def service = getService(
            projectName,
            serviceName,
            clusterName,
            envName
        )
        logger.debug(objectToJson(service))
        assert getParameterDetail(service.service, 'deploymentStrategy').parameterValue == 'rollingDeployment'
        assert getParameterDetail(service.service, 'maxRunningPercentage').parameterValue == '125'
        assert getParameterDetail(service.service, 'minAvailabilityPercentage').parameterValue == '75'
        cleanup:
        cleanupService(serviceName)
    }

    def "Two containers"() {
        given:
        def serviceName = 'two-containers-kube-discover-spec'
        cleanupService(serviceName)
        deployTwoContainers(serviceName)
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Discover',
                    actualParameter: [
                        clusterName: '$clusterName',
                        namespace: 'default',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        projName: '$projectName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def service = getService(
            projectName,
            serviceName,
            clusterName,
            envName
        )
        logger.debug(objectToJson(service))

        def containers = service.service.container
        assert containers.size() == 2
        def first = containers.find {
            it.imageVersion == '1.0'
        }
        def second = containers.find {
            it.imageVersion == '2.0'
        }
        assert first
        assert second
        assert first.port[0].containerPort == '8080'
        assert second.port[0].containerPort == '8080'
        assert service.service.port.size() == 2
        def firstPort = service.service.port.find { it.subcontainer == 'hello' }
        def secondPort = service.service.port.find { it.subcontainer == 'hello-2' }
        assert firstPort.listenerPort == '80'
        assert secondPort.listenerPort == '81'
        cleanup:
        cleanupService(serviceName)
    }

    def deployWithPercentage(serviceName) {
        def deployment = [
            kind    : 'Deployment',
            metadata: [
                name: serviceName,
            ],
            spec    : [
                replicas: 3,
                strategy: [
                    rollingUpdate: [
                        maxSurge      : '25%',
                        maxUnavailable: '25%'
                    ]
                ],
                template: [
                    spec    : [
                        containers: [
                            [name: 'nginx', image: 'nginx:1.10', ports: [
                                [containerPort: 80]
                            ]]
                        ],
                    ],
                    metadata: [labels: [app: 'nginx_test_spec']]
                ]
            ]
        ]

        def service = [
            kind      : 'Service',
            apiVersion: 'v1',
            metadata  : [name: serviceName],
            spec      : [
                selector: [app: 'nginx_test_spec'],
                ports   : [[protocol: 'TCP', port: 80, targetPort: 80]]
            ]
        ]
        deploy(service, deployment)
    }


    def deploySample(serviceName) {
        def deployment = [
            kind    : 'Deployment',
            metadata: [
                name: serviceName,
            ],
            spec    : [
                replicas: 1,
                template: [
                    spec    : [
                        containers: [
                            [
                                name        : 'nginx',
                                image       : 'nginx:1.10',
                                ports       : [[containerPort: 80]],
                                env         : [
                                    [name: "TEST_ENV", "value": "TEST"]
                                ],
                                volumeMounts: [
                                    [name: 'my-volume', mountPath: '/tmp/path_in_container']
                                ]
                            ]
                        ],
                        volumes   : [
                            [hostPath: [path: '/tmp/path'], name: 'my-volume']
                        ]
                    ],
                    metadata: [labels: [app: 'nginx_test_spec']],

                ]
            ]
        ]

        def service = [
            kind      : 'Service',
            apiVersion: 'v1',
            metadata  : [name: serviceName],
            spec      : [
                selector: [app: 'nginx_test_spec'],
                ports   : [[protocol: 'TCP', port: 80, targetPort: 80]],
            ]
        ]
        deploy(service, deployment)
    }


    def deployWithLoadBalancer(serviceName) {
        def service = [
            kind      : 'Service',
            apiVersion: 'v1',
            metadata  : [name: serviceName],
            spec      : [
                selector      : [app: 'nginx_test_spec'],
                type          : 'LoadBalancer',
                loadBalancerIP: '35.224.8.81',
                ports         : [
                    [port: 80, targetPort: 80]
                ]
            ]
        ]
        def deployment = [
            kind    : 'Deployment',
            metadata: [
                name: serviceName,
            ],
            spec    : [
                replicas: 1,
                template: [
                    spec    : [
                        containers: [
                            [
                                name        : 'nginx',
                                image       : 'nginx:1.10',
                                ports       : [[containerPort: 80]],
                                env         : [
                                    [name: "TEST_ENV", "value": "TEST"]
                                ],
                                volumeMounts: [
                                    [name: 'my-volume', mountPath: '/tmp/path_in_container']
                                ]
                            ]
                        ],
                        volumes   : [
                            [hostPath: [path: '/tmp/path'], name: 'my-volume']
                        ]
                    ],
                    metadata: [labels: [app: 'nginx_test_spec']],

                ]
            ]
        ]

        deploy(service, deployment)
    }

    def deployWithSecret(serviceName) {
        def secretName = randomize('spec-secret')
        secretName = secretName.replaceAll('_', '-')
        createSecret(secretName, 'registry.hub.docker.com', 'ecplugintest', 'qweqweqwe')
        def deployment = [
            kind    : 'Deployment',
            metadata: [
                name: serviceName,
            ],
            spec    : [
                replicas: 1,
                template: [
                    spec    : [
                        containers      : [
                            [name: 'hello', image: 'registry.hub.docker.com/imagostorm/hello-world:1.0', ports: [
                                [containerPort: 80]
                            ]]
                        ],
                        imagePullSecrets: [
                            [name: secretName]
                        ]
                    ],
                    metadata: [
                        labels: [
                            app: 'nginx_test_spec'
                        ]
                    ]
                ]
            ]
        ]

        def service = [
            kind      : 'Service',
            apiVersion: 'v1',
            metadata  : [name: serviceName],
            spec      : [
                selector: [app: 'nginx_test_spec'],
                ports   : [[protocol: 'TCP', port: 80, targetPort: 80]]
            ]
        ]
        deploy(service, deployment)
        secretName
    }

    def deployLiveness(serviceName) {
        def container = [
            args          : ['/server'],
            image         : 'k8s.gcr.io/liveness',
            livenessProbe : [
                httpGet            : [
                    path       : '/healthz',
                    port       : 8080,
                    httpHeaders: [
                        [name: 'X-Custom-Header', value: 'Awesome']
                    ]
                ],
                initialDelaySeconds: 15,
                timeoutSeconds     : 1
            ],
            readinessProbe: [
                exec               : [
                    command: [
                        'cat',
                        '/tmp/healthy'
                    ]
                ],
                initialDelaySeconds: 5,
                periodSeconds      : 5,
            ],
            name          : 'liveness-readiness'
        ]
        def deployment = [
            kind    : 'Deployment',
            metadata: [
                name: serviceName,
            ],
            spec    : [
                replicas: 1,
                template: [
                    spec    : [
                        containers: [
                            container
                        ],
                    ],
                    metadata: [
                        labels: [
                            app: 'liveness-probe'
                        ]
                    ]
                ]
            ]
        ]

        def service = [
            kind      : 'Service',
            apiVersion: 'v1',
            metadata  : [name: serviceName],
            spec      : [
                selector: [app: 'liveness-probe'],
                ports   : [[protocol: 'TCP', port: 80, targetPort: 8080]]
            ]
        ]

        deploy(service, deployment)

    }


    def deployTwoContainers(serviceName) {
        def deployment = [
            kind    : 'Deployment',
            metadata: [
                name: serviceName,
            ],
            spec    : [
                replicas: 2,
                template: [
                    spec    : [
                        containers: [
                            [name: 'hello', image: 'imagostorm/hello-world:1.0', ports: [
                                [containerPort: 8080, name: 'first']
                            ]],
                            [name: 'hello-2', 'image': 'imagostorm/hello-world:2.0', ports: [
                                [containerPort: 8080, name: 'second']
                            ]]
                        ]
                    ],
                    metadata: [
                        labels: [
                            app: 'hello-app'
                        ]
                    ]
                ]
            ]
        ]
        def service = [
            kind      : 'Service',
            apiVersion: 'v1',
            metadata  : [name: serviceName],
            spec      : [
                type    : 'LoadBalancer',
                selector: [app: 'hello-app'],
                ports   : [
                    [protocol: 'TCP', port: 80, targetPort: 'first', name: 'first'],
                    [protocol: 'TCP', port: 81, targetPort: 'second', name: 'second']
                ]
            ]
        ]

        deploy(service, deployment)
    }

    def getParameterDetail(struct, name) {
        return struct.parameterDetail.find {
            it.parameterName == name
        }
    }


}
