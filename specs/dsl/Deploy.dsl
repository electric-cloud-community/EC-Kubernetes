def projName = args.projectName
def clusName = args.clusterName
def envName = args.envName
def servName = args.serviceName


project projName, {
    service servName, {
      applicationName = null
      defaultCapacity = args.defaultCapacity
      maxCapacity = args.maxCapacity
      minCapacity = args.minCapacity
      volume = args.volume

      container 'Spec', {
        description = ''
        applicationName = null
        command = args.command
        cpuCount = args.cpuCount
        cpuLimit = args.cpuLimit
        entryPoint = args.entryPoint
        imageName = args.imageName
        imageVersion = args.imageVersion
        memoryLimit = args.memoryLimit
        memorySize = args.memorySize
        registryUri = args.registryUri
        volumeMount = args.volumeMount

        port 'http', {
          applicationName = null
          containerPort = args.containerPort
        }
      }

      environmentMap '775bebca-11ac-11e8-a673-024246ad73e2', {
        environmentName = envName
        environmentProjectName = projName

        serviceClusterMapping '77ffcd57-11ac-11e8-be1e-024246ad73e2', {
          actualParameter = args.serviceMappingParameters
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
        listenerPort = args.listenerPort
        subcontainer = 'Spec'
        subport = 'http'
      }

      process 'Undeploy', {
        processType = 'UNDEPLOY'
        serviceName = servName
        processStep 'undeployService', {
          processStepType = 'service'
        }

      }

      process 'Deploy', {
        applicationName = null
        processType = 'DEPLOY'
        serviceName = servName
        smartUndeployEnabled = null
        timeLimitUnits = null
        workingDirectory = null
        workspaceName = null

        processStep 'deployService', {
          afterLastRetry = null
          alwaysRun = '0'
          errorHandling = 'failProcedure'
          processStepType = 'service'
        }


      }

    }

}
