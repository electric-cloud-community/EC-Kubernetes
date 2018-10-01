package dsl.kubernetes

def names = args.names,
    projectName = names.projectName,
    serviceName = names.serviceName

getTierMaps(
        projectName: projectName,
        serviceName: serviceName
)