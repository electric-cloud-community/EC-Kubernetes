package dsl.flow

def names = args.params,
    project = names.project,
    appName = names.appName,
    tierMapName = names.tierMapName

runProcess(
        projectName: project,
        applicationName: appName,
        processName: 'Undeploy',
        tierMapName: tierMapName,
        rollingDeployEnabled: false
)