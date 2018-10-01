package dsl.kubernetes

def names = args.names,
    replicas = names.replicas.toString(),
    sourceVolume = names.sourceVolume,
    targetVolume = names.targetVolume,
    isCanary = names.isCanary.toString()

project 'k8sProj', {

    application 'nginx-application', {
        description = ''
        projectName = 'k8sProj'

        service 'nginx-service', {
            applicationName = 'nginx-application'
            defaultCapacity = null
            maxCapacity = null
            minCapacity = null
            volume = sourceVolume

            container 'nginx-container', {
                description = ''
                applicationName = 'nginx-application'
                command = null
                cpuCount = '0.1'
                cpuLimit = '2'
                entryPoint = null
                imageName = 'tomaskral/nonroot-nginx'
                imageVersion = 'latest'
                memoryLimit = '256'
                memorySize = '128'
                registryUri = null
                serviceName = 'nginx-service'
                volumeMount = targetVolume

                environmentVariable 'NGINX_HOST', {
                    type = 'string'
                    value = '8080'
                }

                port 'http', {
                    applicationName = 'nginx-application'
                    containerName = 'nginx-container'
                    containerPort = '8080'
                    serviceName = 'nginx-service'
                }
            }

            port '_servicehttpnginx-container01528389377055', {
                applicationName = 'nginx-application'
                listenerPort = '81'
                serviceName = 'nginx-service'
                subcontainer = 'nginx-container'
                subport = 'http'
            }

            process 'Deploy', {
                applicationName = null
                processType = 'DEPLOY'
                serviceName = 'nginx-service'
                smartUndeployEnabled = null
                timeLimitUnits = null
                workingDirectory = null
                workspaceName = null

                processStep 'Deploy', {
                    afterLastRetry = null
                    alwaysRun = '0'
                    applicationTierName = null
                    componentRollback = null
                    dependencyJoinType = 'and'
                    errorHandling = 'failProcedure'
                    instruction = null
                    notificationEnabled = null
                    notificationTemplate = null
                    processStepType = 'service'
                    retryCount = null
                    retryInterval = null
                    retryType = null
                    rollbackSnapshot = null
                    rollbackType = null
                    rollbackUndeployProcess = null
                    skipRollbackIfUndeployFails = null
                    smartRollback = null
                    subcomponent = null
                    subcomponentApplicationName = null
                    subcomponentProcess = null
                    subprocedure = null
                    subproject = null
                    subservice = 'nginx-service'
                    subserviceProcess = null
                    timeLimitUnits = null
                    useUtilityResource = '0'
                    utilityResourceName = null
                    workingDirectory = null
                    workspaceName = null
                }
            }

            process 'Undeploy', {
                applicationName = null
                processType = 'UNDEPLOY'
                serviceName = 'nginx-service'
                smartUndeployEnabled = null
                timeLimitUnits = null
                workingDirectory = null
                workspaceName = null

                processStep 'Undeploy', {
                    afterLastRetry = null
                    alwaysRun = '0'
                    applicationTierName = null
                    componentRollback = null
                    dependencyJoinType = 'and'
                    errorHandling = 'abortJob'
                    instruction = null
                    notificationEnabled = null
                    notificationTemplate = null
                    processStepType = 'service'
                    retryCount = null
                    retryInterval = null
                    retryType = null
                    rollbackSnapshot = null
                    rollbackType = null
                    rollbackUndeployProcess = null
                    skipRollbackIfUndeployFails = null
                    smartRollback = null
                    subcomponent = null
                    subcomponentApplicationName = null
                    subcomponentProcess = null
                    subprocedure = null
                    subproject = null
                    subservice = 'nginx-service'
                    subserviceProcess = null
                    timeLimitUnits = null
                    useUtilityResource = '0'
                    utilityResourceName = null
                    workingDirectory = null
                    workspaceName = null
                }
            }
        }

        process 'Deploy', {
            applicationName = 'nginx-application'
            processType = 'OTHER'
            serviceName = null
            smartUndeployEnabled = null
            timeLimitUnits = null
            workingDirectory = null
            workspaceName = null

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                label = null
                orderIndex = null
                required = '0'
                type = 'checkbox'
            }

            formalParameter 'ec_nginx-service-run', defaultValue: '1', {
                expansionDeferred = '1'
                label = null
                orderIndex = null
                required = '0'
                type = 'checkbox'
            }

            processStep 'Deploy', {
                afterLastRetry = null
                alwaysRun = '0'
                applicationTierName = null
                componentRollback = null
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                processStepType = 'process'
                retryCount = null
                retryInterval = null
                retryType = null
                rollbackSnapshot = null
                rollbackType = null
                rollbackUndeployProcess = null
                skipRollbackIfUndeployFails = null
                smartRollback = null
                subcomponent = null
                subcomponentApplicationName = 'nginx-application'
                subcomponentProcess = null
                subprocedure = null
                subproject = null
                subservice = 'nginx-service'
                subserviceProcess = 'Deploy'
                timeLimitUnits = null
                useUtilityResource = '0'
                utilityResourceName = null
                workingDirectory = null
                workspaceName = null

                // Custom properties

                property 'ec_deploy', {

                    // Custom properties
                    ec_notifierStatus = '0'
                }
            }

            // Custom properties

            property 'ec_deploy', {

                // Custom properties
                ec_notifierStatus = '0'
            }
        }

        process 'Undeploy', {
            applicationName = 'nginx-application'
            processType = 'OTHER'
            serviceName = null
            smartUndeployEnabled = null
            timeLimitUnits = null
            workingDirectory = null
            workspaceName = null

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                label = null
                orderIndex = null
                required = '0'
                type = 'checkbox'
            }

            formalParameter 'ec_nginx-service-run', defaultValue: '1', {
                expansionDeferred = '1'
                label = null
                orderIndex = null
                required = '0'
                type = 'checkbox'
            }

            processStep 'Undeploy', {
                afterLastRetry = null
                alwaysRun = '0'
                applicationTierName = null
                componentRollback = null
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                processStepType = 'process'
                retryCount = null
                retryInterval = null
                retryType = null
                rollbackSnapshot = null
                rollbackType = null
                rollbackUndeployProcess = null
                skipRollbackIfUndeployFails = null
                smartRollback = null
                subcomponent = null
                subcomponentApplicationName = 'nginx-application'
                subcomponentProcess = null
                subprocedure = null
                subproject = null
                subservice = 'nginx-service'
                subserviceProcess = 'Undeploy'
                timeLimitUnits = null
                useUtilityResource = '0'
                utilityResourceName = null
                workingDirectory = null
                workspaceName = null

                // Custom properties

                property 'ec_deploy', {

                    // Custom properties
                    ec_notifierStatus = '0'
                }
            }

            // Custom properties

            property 'ec_deploy', {

                // Custom properties
                ec_notifierStatus = '0'
            }
        }

        tierMap 'ec7c7213-6a70-11e8-ab9b-0050569620f8', {
            applicationName = 'nginx-application'
            environmentName = 'k8s-environment'
            environmentProjectName = 'k8sProj'

            serviceClusterMapping 'ee853a67-6a70-11e8-ad83-0050569620f8', {
                actualParameter = [
                        'canaryDeployment': isCanary,
                        'numberOfCanaryReplicas': replicas,
                        'serviceType': 'LoadBalancer',
                ]
                clusterName = 'k8s-cluster'
                clusterProjectName = null
                defaultCapacity = '2'
                environmentMapName = null
                maxCapacity = '2'
                minCapacity = '1'
                serviceName = 'nginx-service'
                tierMapName = 'ec7c7213-6a70-11e8-ab9b-0050569620f8'
                volume = sourceVolume

                serviceMapDetail 'nginx-container', {
                    serviceMapDetailName = '6a8fae5e-6a71-11e8-af87-0050569620f8'
                    command = null
                    cpuCount = null
                    cpuLimit = null
                    entryPoint = null
                    imageName = null
                    imageVersion = null
                    memoryLimit = null
                    memorySize = null
                    registryUri = null
                    serviceClusterMappingName = 'ee853a67-6a70-11e8-ad83-0050569620f8'
                    volumeMount = targetVolume
                }
            }
        }

        // Custom properties

        property 'ec_deploy', {

            // Custom properties
            ec_notifierStatus = '0'
        }
    }
}

