import spock.lang.*
import com.electriccloud.spec.*

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
    // Broken for now
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
        assert container.image == "${imageName}${imageVersion ? ':' + imageVersion : 'latest'}"
        assert container.ports[0].containerPort.toString() == containerPort
//
//            def strategy = deployment.spec.strategy.rollingUpdate
//            assert strategy
//            def expectedMaxSurge = maxCapacity ? maxCapacity - expectedDefaultCapacity : 1
//            def expectedMaxUnavailable = minCapacity ? expectedDefaultCapacity - minCapacity : 1
//            assert strategy.maxSurge == expectedMaxSurge
//            assert strategy.maxUnavailable == expectedMaxUnavailable
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
