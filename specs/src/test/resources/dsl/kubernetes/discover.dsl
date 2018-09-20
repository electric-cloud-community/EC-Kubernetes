package dsl.kubernetes

def names = args.names,
        pluginName = 'EC-Kubernetes',
        projectName = names.projectName,
        envProjectName = names.envProjectName,
        environmentName = names.environmentName,
        namespace = names.namespace,
        clusterName = names.clusterName,
        clusterEndpoint = names.clusterEndpoint,
        clusterApiToken = names.clusterApiToken,
        applicationScoped = names.applicationScoped
        applicationName = names.applicationName

// Create plugin configuration

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Discover',
        actualParameter: [
                envProjectName: envProjectName,
                envName: environmentName,
                clusterName: clusterName,
                ecp_kubernetes_apiEndpoint: clusterEndpoint,
                ecp_kubernetes_apiToken: clusterApiToken,
                namespace: namespace,
                projName: projectName,
                ecp_kubernetes_applicationScoped: applicationScoped,
                ecp_kubernetes_applicationName: applicationName
        ]
)