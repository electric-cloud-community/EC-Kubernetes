import spock.lang.*
import com.electriccloud.spec.*

class Import extends KubeHelper {
    static def projectName = 'EC-Kubernetes Specs Import'
    static def clusterName = 'Kube Spec Cluster'
    static def envName = 'Kube Spec Env'
    static def serviceName = 'kube-spec-import-test'
    static def kubeYAMLFile = """
kind: Service
apiVersion: v1
metadata:
  name: my-service
spec:
  selector:
    app: nginx
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376

 ---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
spec:         
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        ports:
        - containerPort: 80
 """

    def doSetupSpec() {
        configName = 'Kube Spec Config'
        dslFile 'dsl/Import.dsl', [
                projectName: projectName,
                params: [
                        kubeYAMLFile: '',
                        projName: '',
                        envProjectName: '',
                        envName: '',
                        clusterName: '',
                ]
        ]

    }

    def doCleanupSpec() {
//        cleanupCluster(configName)
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }


    def "Import sample"() {
        given:

        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import',
                    actualParameter: [
                        kubeYAMLFile: '$kubeYAMLFile'
                        projectName: '$projectName',
                        clusterName: '$clusterName',
                        envProjectName: '$projectName',
                        envName: '$envName',
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
        assert containers[0].imageVersion == '1.7.9'
        def port = containers[0].port[0]
        assert port
        assert service.service.defaultCapacity == '1'
        assert port.containerPort == '80'
    }
}