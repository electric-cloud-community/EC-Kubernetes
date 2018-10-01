package dsl.kubernetes

def names = args.names,
        configName = names.configName
        projectNamespace = names.projectNamespace

runProcedure(
        projectName: '/plugins/EC-Kubernetes/project',
        procedureName: "Cleanup Cluster - Experimental",
        actualParameter: [
                namespace: projectNamespace,
                config: configName
        ]
)