package dsl.kubernetes

def names = args.names,
        projectName = names.projectName,
        environmentName = names.environmentName,
        cluster = names.cluster

provisionCluster(
        projectName: projectName,
        environmentName: environmentName,
        cluster:[
                cluster
        ]
)