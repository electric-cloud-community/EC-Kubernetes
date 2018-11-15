package dsl.flow

def names = args.params,
    project = names.project,
    environment = names.environment,
    envProject = names.envProject,
    service = names.service

runServiceProcess(
        projectName: project,
        serviceName: service,
        environmentName: environment,
        environmentProjectName: envProject,
        processName: 'Deploy',
)