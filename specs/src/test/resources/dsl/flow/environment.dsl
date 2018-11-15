package dsl.flow

def names = args.params,
    configName = names.configName

project 'k8sProj', {
    resourceName = null
    workspaceName = null

    environment 'k8s-environment', {
        environmentEnabled = '1'
        projectName = 'k8sProj'
        reservationRequired = '0'
        rollingDeployEnabled = null
        rollingDeployType = null

        cluster 'k8s-cluster', {
            environmentName = 'k8s-environment'
            pluginKey = 'EC-Kubernetes'
            pluginProjectName = null
            providerClusterName = null
            providerProjectName = null
            provisionParameter = [
                    'config': configName,
            ]
            provisionProcedure = 'Check Cluster'

            // Custom properties

            property 'ec_provision_parameter', {

                // Custom properties
                dsl.kubernetes.config = configName
            }
        }
    }


}
