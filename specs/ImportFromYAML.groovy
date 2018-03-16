import spock.lang.*
import com.electriccloud.spec.*

class ImportFromYAML extends KubeHelper {
    static def kubeYAMLFile
    static def projectName = 'EC-Kubernetes Specs Import'
    def applicationScoped = true
    static def applicationName = 'Kube Spec App'
    static def envName = 'Kube Spec Env'
    static def serviceName = 'kube-spec-import-test'
    static def clusterName = 'Kube Spec Cluster'
    static def configName

    def doSetupSpec() {
        configName = 'Kube Spec Config'
        createCluster(projectName, envName, clusterName, configName)
        dslFile 'dsl/ImportFromYAML.dsl', [
            projectName: projectName,
            params: [
                kubeYAMLFile:       '',
                projName:           '',
                application_scoped: '',
                application_name:   '',
                envProjectName:     '',
                envName:            '',
                clusterName:        '',
            ]
        ]

    }

    def doCleanupSpec() {
        cleanupCluster(configName)
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }

    def "one service to one deploy"() {
        given:
            def sampleName = 'my-service-nginx-deployment'
            cleanupService(sampleName)
            kubeYAMLFile = 
'''
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
        resources:
          limits:
            memory: 256
            cpu: 0.5
          requests:
            memory: 128
            cpu: 0.25
        ports:
        - containerPort: 80
'''.trim()
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'ImportFromYAML',
                    actualParameter: [
                        kubeYAMLFile: '$kubeYAMLFile',
                        projName: '$projectName',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
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
            assert service.service.defaultCapacity == '3'
            assert port.containerPort == '80'
            assert service.service.port[0].listenerPort == '80'
    }


    def "one service to many deploy"() {
        given:
            def sampleOneName = 'my-service-nginx-deployment'
            def sampleTwoName = 'my-service-nginx-deployment2'
            cleanupService(sampleOneName)
            cleanupService(sampleTwoName)
            kubeYAMLFile = 
'''
kind: Service
apiVersion: v1
metadata:
  name: my-service
spec:
  selector:
    app: nginx
    serv: nginx3
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
        command: ["test_command"]
        args: ["TEST_ARG1", "TEST_ARG2"]
        resources:
          limits:
            memory: 256
            cpu: 0.5
          requests:
            memory: 128
            cpu: 0.25
        ports:
        - containerPort: 80
        volumeMounts:
         - name: testConfig-vol
           mountPath: /etc/testConfig
        env:
         - name: DEMO
           value: "for DEMO"
      containers:
      - name: nginx2
        image: nginx:1.7.9
        command: ["test_command"]
        args: ["TEST_ARG1", "TEST_ARG2"]
        resources:
          limits:
            memory: 256
            cpu: 0.5
          requests:
            memory: 128
            cpu: 0.25
        ports:
        - containerPort: 80
        volumeMounts:
         - name: testConfig-vol
           mountPath: /etc/testConfig
        env:
         - name: DEMO
           value: "for DEMO"

---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment2
  labels:
    serv: nginx3
spec:         
  replicas: 3
  selector:
    matchLabels:
      serv: nginx
  template:
    metadata:
      labels:
        serv: nginx3
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        command: ["test_command"]
        args: ["TEST_ARG1", "TEST_ARG2"]
        resources:
          limits:
            memory: 256
            cpu: 0.5
          requests:
            memory: 128
            cpu: 0.25
        ports:
        - containerPort: 80
        volumeMounts:
         - name: testConfig-vol
           mountPath: /etc/testConfig
        env:
         - name: DEMO
           value: "for DEMO"
      containers:
      - name: nginx4
        image: nginx:1.7.9
        command: ["test_command"]
        args: ["TEST_ARG1", "TEST_ARG2"]
        resources:
          limits:
            memory: 256
            cpu: 0.5
          requests:
            memory: 128
            cpu: 0.25
        ports:
        - containerPort: 80
        volumeMounts:
         - name: testConfig-vol
           mountPath: /etc/testConfig
        env:
         - name: DEMO
           value: "for DEMO"
'''.trim()
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'ImportFromYAML',
                    actualParameter: [
                        kubeYAMLFile: '$kubeYAMLFile',
                        projName: '$projectName',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                                            ]
                )
            """
        then:
            logger.debug(result.logs)
            def serviceOne = getService(
                projectName,
                sampleOneName,
                clusterName,
                envName
            )
            def serviceTwo = getService(
                projectName,
                sampleTwoName,
                clusterName,
                envName
            )
            
            assert result.outcome != 'error'
            assert serviceOne.service
            assert serviceOne.service.defaultCapacity == '3'
            assert serviceOne.service.port[0].listenerPort == '80'
            assert serviceTwo.service
            assert serviceTwo.service.defaultCapacity == '3'
            assert serviceTwo.service.port[0].listenerPort == '80'
            def containersOne = serviceOne.service.container
            assert containersOne.size() == 1
            assert containersOne[0].containerName == 'nginx'
            assert containersOne[0].imageName == 'nginx'
            assert containersOne[0].imageVersion == '1.7.9'
            def portOne = containersOne[0].port[0]
            assert portOne
            assert portOne.containerPort == '80'
            def containersTwo = serviceTwo.service.container
            assert containersTwo.size() == 1
            assert containersTwo[0].containerName == 'nginx'
            assert containersTwo[0].imageName == 'nginx'
            assert containersTwo[0].imageVersion == '1.7.9'
            def portTwo = containersTwo[0].port[0]
            assert portTwo
            assert portTwo.containerPort == '80'

    }

    def "one service to one deploy with two containers"() {
        given:
            def sampleName = 'my-service-nginx-deployment'
            cleanupService(sampleName)
            kubeYAMLFile = 
'''
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
        resources:
          limits:
            memory: 4Gi
            cpu: 50
          requests:
            memory: 204800Ki
            cpu: 500m
        ports:
        - containerPort: 80
      - name: nginx2
        image: nginx:1.7.10
        resources:
          limits:
            memory: 100
            cpu: 10
          requests:
            memory: 100
            cpu: 100m
        ports:
        - containerPort: 80
'''.trim()
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'ImportFromYAML',
                    actualParameter: [
                        kubeYAMLFile: '$kubeYAMLFile',
                        projName: '$projectName',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
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
            assert service.service.defaultCapacity == '3'
            assert service.service.port[0].listenerPort == '80'
            def containers = service.service.container
            assert containers.size() == 2
            assert containers[0].containerName == 'nginx'
            assert containers[0].imageName == 'nginx'
            assert containers[0].imageVersion == '1.7.9'
            assert containers[1].containerName == 'nginx2'
            assert containers[1].imageName == 'nginx'
            assert containers[1].imageVersion == '1.7.10'
            def portOne = containers[0].port[0]
            assert portOne
            assert portOne.containerPort == '80'
            def portTwo = containers[1].port[1]
            assert portTwo
            assert portTwo.containerPort == '80'
    }


    def "many services to many deploys"() {
        given:
            def sampleOneName = 'my-service1-nginx-deployment1'
            def sampleTwoName = 'my-service2-nginx-deployment2'
            cleanupService(sampleOneName)
            cleanupService(sampleTwoName)
            kubeYAMLFile = 
'''
kind: Service1
apiVersion: v1
metadata:
  name: my-service1
spec:
  selector:
    app: nginx1
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376

    ---

kind: Service2
apiVersion: v1
metadata:
  name: my-service2
spec:
  selector:
    app: nginx2
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376

    ---

apiVersion: apps/v1
kind: Deployment1
metadata:
  name: nginx-deployment1
  labels:
    app: nginx1
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
        resources:
          limits:
            memory: 20480k
            cpu: 500
          requests:
            memory: 0.5g
            cpu: 5000m
        ports:
        - containerPort: 80
      - name: nginx2
        image: nginx:1.7.10
        resources:
          limits:
            memory: 0.01t
            cpu: 1000
          requests:
            memory: 0.001p
            cpu: 10000m
        ports:
        - containerPort: 90
---

apiVersion: apps/v1
kind: Deployment2
metadata:
  name: nginx-deployment2
  labels:
    app: nginx2
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
        resources:
          limits:
            memory: 20480k
            cpu: 500
          requests:
            memory: 0.5g
            cpu: 5000m
        ports:
        - containerPort: 80
'''.trim()
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'ImportFromYAML',
                    actualParameter: [
                        kubeYAMLFile: '$kubeYAMLFile',
                        projName: '$projectName',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                                            ]
                )
            """
        then:
            logger.debug(result.logs)
            def serviceOne = getService(
                projectName,
                sampleOneName,
                clusterName,
                envName
            )
            def serviceTwo = getService(
                projectName,
                sampleTwoName,
                clusterName,
                envName
            )

            
            assert result.outcome != 'error'
            assert serviceOne.service
            assert serviceOne.service.defaultCapacity == '3'
            assert serviceOne.service.port[0].listenerPort == '80'
            assert serviceTwo.service
            assert serviceTwo.service.defaultCapacity == '3'
            assert serviceTwo.service.port[0].listenerPort == '80'
            def containersOne = serviceOne.service.container
            assert containersOne.size() == 2
            assert containersOne[0].containerName == 'nginx'
            assert containersOne[0].imageName == 'nginx'
            assert containersOne[0].imageVersion == '1.7.9'
            assert containersOne[1].containerName == 'nginx2'
            assert containersOne[1].imageName == 'nginx'
            assert containersOne[1].imageVersion == '1.7.10'
            def portOne1 = containersOne[0].port[0]
            assert portOne
            assert portOne.containerPort == '80'
            def portOne2 = containersOne[1].port[0]
            assert portOne
            assert portOne.containerPort == '80'
            def containersTwo = serviceTwo.service.container
            assert containersTwo.size() == 1
            assert containersTwo[0].containerName == 'nginx'
            assert containersTwo[0].imageName == 'nginx'
            assert containersTwo[0].imageVersion == '1.7.9'
            def portTwo = containersTwo[0].port[0]
            assert portTwo
            assert portTwo.containerPort == '80'

    }

}
