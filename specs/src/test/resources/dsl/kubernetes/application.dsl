package dsl.kubernetes

def names = args.names,
    replicas = names.replicas.toString(),
    sourceVolume = names.sourceVolume,
    targetVolume = names.targetVolume,
    isCanary = names.isCanary.toString(),
    serviceType = names.serviceType,
    namespace = names.namespace,
    deploymentTimeout = names.deploymentTimeout

project 'k8sProj', {

    application 'nginx-application', {
        description = ''

        service 'nginx-service', {
            applicationName = 'nginx-application'
            defaultCapacity = replicas.toString()
            maxCapacity = (replicas + 1).toString()
            minCapacity = '1'
            volume = sourceVolume

            container 'nginx-container', {
                description = ''
                applicationName = 'nginx-application'
                cpuCount = '0.1'
                cpuLimit = '2'
                imageName = 'tomaskral/nonroot-nginx'
                imageVersion = 'latest'
                memoryLimit = '255'
                memorySize = '128'
                serviceName = 'nginx-service'
                volumeMount = targetVolume

                environmentVariable 'NGINX_PORT', {
                    type = 'string'
                    value = '8080'
                }

                port 'http', {
                    applicationName = 'nginx-application'
                    containerName = 'nginx-container'
                    containerPort = '8080'
                    projectName = 'k8sProj'
                    serviceName = 'nginx-service'
                }
            }

            port '_servicehttpnginx-container01529487635489', {
                applicationName = 'nginx-application'
                listenerPort = '81'
                projectName = 'k8sProj'
                serviceName = 'nginx-service'
                subcontainer = 'nginx-container'
                subport = 'http'
            }

            process 'Deploy', {
                processType = 'DEPLOY'
                serviceName = 'nginx-service'

                processStep 'deployService', {
                    alwaysRun = '0'
                    errorHandling = 'failProcedure'
                    processStepType = 'service'
                    useUtilityResource = '0'
                }
            }

            process 'Undeploy', {
                processType = 'UNDEPLOY'
                serviceName = 'nginx-service'

                processStep 'Undeploy', {
                    alwaysRun = '0'
                    dependencyJoinType = 'and'
                    errorHandling = 'abortJob'
                    processStepType = 'service'
                    subservice = 'nginx-service'
                    useUtilityResource = '0'
                }
            }
        }

        process 'Deploy', {
            applicationName = 'nginx-application'
            processType = 'OTHER'

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                required = '0'
                type = 'checkbox'
            }

            processStep 'Deploy', {
                alwaysRun = '0'
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                processStepType = 'process'
                subcomponentApplicationName = 'nginx-application'
                subservice = 'nginx-service'
                subserviceProcess = 'Deploy'
                useUtilityResource = '0'

                // Custom properties

                property 'ec_deploy', {

                    // Custom properties
                    ec_notifierStatus = '0'
                }
            }

            property 'ec_deploy', {
                ec_notifierStatus = '0'
            }
        }

        process 'Undeploy', {
            applicationName = 'nginx-application'
            processType = 'OTHER'

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                required = '0'
                type = 'checkbox'
            }

            processStep 'Undeploy', {
                alwaysRun = '0'
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                processStepType = 'process'
                subcomponentApplicationName = 'nginx-application'
                subservice = 'nginx-service'
                subserviceProcess = 'Undeploy'
                useUtilityResource = '0'

                property 'ec_deploy', {

                    // Custom properties
                    ec_notifierStatus = '0'
                }
            }

            property 'ec_deploy', {

                // Custom properties
                ec_notifierStatus = '0'
            }
        }

        tierMap 'nginx-tier-mapping', {
            applicationName = 'nginx-application'
            environmentName = 'k8s-environment'
            environmentProjectName = 'k8sProj'
            projectName = 'k8sProj'

            serviceClusterMapping 'nginx-mapping', {
                actualParameter = [
                        'canaryDeployment': isCanary,
                        'numberOfCanaryReplicas': replicas.toString(),
                        'deploymentTimeoutInSec': deploymentTimeout,
                        'namespace': namespace,
                        'serviceType': serviceType
                ]
                clusterName = 'k8s-cluster'
                serviceName = 'nginx-service'
                tierMapName = 'nginx-tier-mapping'

                serviceMapDetail 'nginx-container', {
                    serviceMapDetailName = '3aa3d573-746e-11e8-a2a9-00505696e27a'
                    serviceClusterMappingName = 'nginx-mapping'
                }
            }
        }

        property 'ec_deploy', {
            ec_notifierStatus = '0'
        }
    }
}