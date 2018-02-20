import spock.lang.*
import com.electriccloud.spec.*

class KubeHelper extends ContainerHelper {

    def createCluster(projectName, envName, clusterName, configName) {
        createConfig(configName)
        dsl """
            project '$projectName', {
                environment '$envName', {
                    cluster '$clusterName', {
                        pluginKey = 'EC-Kubernetes'
                        provisionParameter = [
                            config: '$configName'
                        ]
                        provisionProcedure = 'Check Cluster'
                    }
                }
            }
        """
    }


    def deleteConfig(configName) {
        deleteConfiguration('EC-Kubernetes', configName)
    }

    def createConfig(configName) {
        def token = System.getenv('KUBE_TOKEN')
        assert token
        def endpoint = System.getenv('KUBE_ENDPOINT')
        assert endpoint
        def pluginConfig = [
            kubernetesVersion: '1.7',
            clusterEndpoint: endpoint,
            testConnection: 'false',
            logLevel: '1'
        ]
        def props = [:]
        if (System.getenv('RECREATE_CONFIG')) {
            props.recreate = true
        }
        createPluginConfiguration(
            'EC-Kubernetes',
            configName,
            pluginConfig,
            'test',
            token,
            props
        )
    }

    def cleanupCluster(configName) {
        def procName = 'Cleanup Cluster - Experimental'
        def result = dsl """
            runProcedure(
                projectName: '/plugins/EC-Kubernetes/project',
                procedureName: "$procName",
                actualParameter: [
                    namespace: 'default',
                    config: '$configName'
                ]
            )
        """
        assert result.jobId

        def time = 0
        def timeout = 300
        def delay = 50
        while(jobStatus(result.jobId).status != 'completed' && time < timeout) {
            sleep(delay * 1000)
            time += delay
        }

        jobCompleted(result)
    }

}
