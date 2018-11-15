package dsl.flow

def names = args.params,
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