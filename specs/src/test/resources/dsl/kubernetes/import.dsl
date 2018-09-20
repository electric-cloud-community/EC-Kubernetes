package dsl.kubernetes

def names = args.names,
        templateYaml = names.templateYaml,
        projectName = names.projectName,
        applicationScoped = names.applicationScoped,
        applicationName = names.applicationName,
        envProjectName = names.envProjectName,
        environmentName = names.environmentName,
        clusterName = names.clusterName

// Create plugin configuration

def pluginProjectName = getPlugin(pluginName: 'EC-Kubernetes').projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Import Microservices',
        actualParameter: [
                kubeYAMLFile: templateYaml,
                projName: projectName,
                application_scoped: applicationScoped,
                application_name: applicationName,
                envProjectName: envProjectName,
                envName: environmentName,
                clusterName: clusterName

        ]
)