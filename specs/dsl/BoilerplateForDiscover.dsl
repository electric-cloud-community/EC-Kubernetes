def projName = args.projName
def clusName = args.clusterName
def envName = args.envName
def servName = args.serviceName

def parameters = args.params

project projName, {
    service servName, {
      applicationName = null
      defaultCapacity = '$[defaultCapacity]'
      maxCapacity = '$[maxCapacity]'
      minCapacity = '$[minCapacity]'
      volume = null

      container 'Spec', {
        description = ''
        applicationName = null
        command = null
        cpuCount = null
        cpuLimit = null
        entryPoint = null
        imageName = '$[imageName]'
        imageVersion = '$[imageVersion]'
        memoryLimit = '$[memoryLimit]'
        memorySize = '$[memorySize]'
        registryUri = null
        volumeMount = null

        port 'http', {
          applicationName = null
          containerPort = '80'
        }
      }

      environmentMap '775bebca-11ac-11e8-a673-024246ad73e2', {
        environmentName = envName
        environmentProjectName = projName

        serviceClusterMapping '77ffcd57-11ac-11e8-be1e-024246ad73e2', {
          actualParameter = [
            'deploymentTimeoutInSec': '300',
          ]
          clusterName = clusName
          clusterProjectName = null
          defaultCapacity = null
          environmentMapName = '775bebca-11ac-11e8-a673-024246ad73e2'
          maxCapacity = null
          minCapacity = null
          serviceName = servName
          tierMapName = null

          volume = null

          serviceMapDetail 'Spec', {
            serviceMapDetailName = '95a07c68-11ac-11e8-a59f-024246ad73e2'
            command = null
            cpuCount = null
            cpuLimit = null
            entryPoint = null
            imageName = null
            imageVersion = null
            memoryLimit = null
            memorySize = null
            registryUri = null
            serviceClusterMappingName = '77ffcd57-11ac-11e8-be1e-024246ad73e2'
            volumeMount = null
          }

          serviceMapDetail 'Spec1', {
            serviceMapDetailName = '95d17855-11ac-11e8-89b6-024246ad73e2'
            command = null
            cpuCount = null
            cpuLimit = null
            entryPoint = null
            imageName = null
            imageVersion = null
            memoryLimit = null
            memorySize = null
            registryUri = null
            serviceClusterMappingName = '77ffcd57-11ac-11e8-be1e-024246ad73e2'
            volumeMount = null
          }
        }
      }

      port '_servicehttpSpec01518629277101', {
        applicationName = null
        listenerPort = '8080'
        subcontainer = 'Spec'
        subport = 'http'
      }

      process 'Deploy', {
        applicationName = null
        processType = 'DEPLOY'
        serviceName = servName
        smartUndeployEnabled = null
        timeLimitUnits = null
        workingDirectory = null
        workspaceName = null

        parameters.each { name, defaultValue ->
            formalParameter name, defaultValue: defaultValue ?: null, {
                type = 'textarea'
            }
        }

        // formalParameter 'ec_enforceDependencies', defaultValue: '0', {
        //   expansionDeferred = '1'
        //   label = null
        //   orderIndex = null
        //   required = '0'
        //   type = 'checkbox'
        // }

        processStep 'deployService', {
          afterLastRetry = null
          alwaysRun = '0'
          errorHandling = 'failProcedure'
          processStepType = 'service'
        }


      }

    }

}
