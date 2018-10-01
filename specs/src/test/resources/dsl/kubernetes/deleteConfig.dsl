package dsl.kubernetes

def names = args.names,
        pluginName = 'EC-Kubernetes',
        configName = names.configName

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: "/plugins/${pluginProjectName}/project",
        procedureName: "DeleteConfiguration",
        actualParameter: [
                config: configName
        ]
)