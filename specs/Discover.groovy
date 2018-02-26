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
            params: [
                envName: '',
                envProjectName: '',
                clusterName: '',
                namespace: '',
                projName: '',
            ]
        ]

    }

    def doCleanupSpec() {
        cleanupCluster(configName)
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }

    // TODO other fields
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

    def deployWithPercentage(serviceName) {
        def deployment = [
          kind: 'Deployment',
          metadata: [
            name: serviceName,
          ],
          spec: [
            replicas: 3,
            strategy: [
                rollingUpdate: [
                    maxSurge: '25%',
                    maxUnavailable: '25%'
                ]
            ],
            template: [
              spec: [
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
            kind: 'Service',
            apiVersion: 'v1',
            metadata: [name: serviceName],
            spec: [
                selector: [app: 'nginx_test_spec'],
                ports: [[protocol: 'TCP', port: 80, targetPort: 80]]
            ]
        ]
        deploy(service, deployment)
    }


    def deploySample(serviceName) {
        def deployment = [
          kind: 'Deployment',
          metadata: [
            name: serviceName,
          ],
          spec: [
            replicas: 1,
            template: [
              spec: [
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
            kind: 'Service',
            apiVersion: 'v1',
            metadata: [name: serviceName],
            spec: [
                selector: [app: 'nginx_test_spec'],
                ports: [[protocol: 'TCP', port: 80, targetPort: 80]]
            ]
        ]
        deploy(service, deployment)
    }

    def deployWithSecret(serviceName) {
        def secretName = randomize('spec-secret')
        secretName = secretName.replaceAll('_', '-')
        createSecret(secretName, 'registry.hub.docker.com', 'ecplugintest', 'qweqweqwe')
        def deployment = [
          kind: 'Deployment',
          metadata: [
            name: serviceName,
          ],
          spec: [
            replicas: 1,
            template: [
              spec: [
                containers: [
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
            kind: 'Service',
            apiVersion: 'v1',
            metadata: [name: serviceName],
            spec: [
                selector: [app: 'nginx_test_spec'],
                ports: [[protocol: 'TCP', port: 80, targetPort: 80] ]
            ]
        ]
        deploy(service, deployment)
        secretName
    }

    def deployLiveness(serviceName) {
        def container = [
            args: ['/server'],
            image: 'k8s.gcr.io/liveness',
            livenessProbe: [
                httpGet: [
                    path: '/healthz',
                    port: 8080,
                    httpHeaders: [
                        [name: 'X-Custom-Header', value: 'Awesome']
                    ]
                ],
                initialDelaySeconds: 15,
                timeoutSeconds: 1
            ],
            readinessProbe: [
                exec: [
                    command: [
                        'cat',
                        '/tmp/healthy'
                    ]
                ],
                initialDelaySeconds: 5,
                periodSeconds: 5,
            ],
            name: 'liveness-readiness'
        ]
        def deployment = [
          kind: 'Deployment',
          metadata: [
            name: serviceName,
          ],
          spec: [
            replicas: 1,
            template: [
              spec: [
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
            kind: 'Service',
            apiVersion: 'v1',
            metadata: [name: serviceName],
            spec: [
                selector: [app: 'liveness-probe'],
                ports: [[protocol: 'TCP', port: 80, targetPort: 8080] ]
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
