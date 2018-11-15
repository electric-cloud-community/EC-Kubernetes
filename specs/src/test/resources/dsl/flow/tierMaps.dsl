package dsl.flow

def names = args.params,
    projectName = names.projectName,
    serviceName = names.serviceName

getTierMaps(
        projectName: projectName,
        serviceName: serviceName
)