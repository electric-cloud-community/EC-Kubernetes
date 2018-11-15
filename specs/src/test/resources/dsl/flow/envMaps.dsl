package dsl.flow

def names = args.params,
    projectName = names.projectName,
    serviceName = names.serviceName

getEnvironmentMaps(
        projectName: projectName,
        serviceName: serviceName
)