import spock.lang.*
import com.electriccloud.spec.*

class Discover extends KubeHelper {
    static def projectName = 'EC-Kubernetes Specs Discover'
    static def configName = 'Kube Spec'
    static def clusterName = 'Kube Spec Cluster'
    static def envName = 'Kube Spec Env'
    static def serviceName = 'Kube Spec Discover'

    def doSetupSpec() {
        createCluster(projectName, envName, clusterName, configName)
        dslFile 'dsl/BoilerplateForDiscover.dsl', [
            projName: projectName,
            envName: envName,
            clusterName: clusterName,
            serviceName: serviceName,
            params: [
                imageName: '',
                imageVersion: '1.0',
                defaultCapacity: '1',
                maxCapacity: '3',
                minCapacity: '1',
                memoryLimit: '100',
                memorySize: '50',
            ]
        ]
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
        if (!System.getenv('NO_CLEANUP')) {
            dsl "deleteProject '$projectName'"
            deleteConfig(configName)
        }
    }

    @Unroll
    def "Discover #imageName, #imageVersion"() {
        given:
            // Deploying service to Kube
            def res = deployService(projectName, serviceName, [
                imageName: imageName,
                imageVersion: imageVersion,
            ])
            // And deleting it from our env
            deleteService(projectName, serviceName)
        when: 'discovery procedure runs'
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
            // And now it should appear anew
            logger.debug(result.logs)
            assert result.outcome != 'error'
            def discoveredServiceName = serviceName.replaceAll(/\s/, '-').toLowerCase()
            def service = getService(projectName, discoveredServiceName, clusterName, envName)
            logger.debug(objectToJson(service))

            assert service.service.defaultCapacity == '1'
            assert service.service.maxCapacity == '3'
            assert service.service.container.size() == 1
            def container = service.service.container[0]
            assert container.imageName == imageName
            assert container.memoryLimit == '100'
            assert container.memorySize == '50'
        cleanup:
            // deleteService(projectName, discoveredServiceName)
        where:
            imageName                | imageVersion | defaultCapacity
            'imagostorm/hello-world' | '1.0'        | '1'
    }
}
