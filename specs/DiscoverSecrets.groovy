import spock.lang.*
import com.electriccloud.spec.*

class DiscoverSecrets extends KubeHelper {
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
            assert service.service.defaultCapacity == '1'
            assert service.service.container.size() == 1
            assert service.service.container[0].imageName == 'imagostorm/hello-world'
            assert service.service.container[0].credentialName
        cleanup:
            cleanupService(serviceName)
            deleteSecret(secretName)
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



}
