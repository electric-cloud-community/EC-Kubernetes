package dsl.kubernetes

def names = args.names,
    project = names.project,
    environment = names.environment,
    envProject = names.envProject,
    service = names.service

runServiceProcess(
        projectName: project,
        serviceName: service,
        environmentName: environment,
        environmentProjectName: envProject,
        processName: 'Undeploy',
)