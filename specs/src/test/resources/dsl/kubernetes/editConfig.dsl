package dsl.kubernetes

def names = args.names,
    pluginName = 'EC-Kubernetes',
    endpoint = names.endpoint,
    logLevel = names.logLevel,
    desc = 'kubernetes Config',
    userName = names.userName,
    token = names.token,
    version = names.version,
    testConnection = names.testConnection.toString(),
    uriToCheckCluster = names.uriToCheckCluster


// Create plugin configuration

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'EditConfiguration',
        actualParameter: [
                clusterEndpoint: endpoint,
                credential: 'credential',
                desc: desc,
                kubernetesVersion: version,
                logLevel: logLevel,
                testConnection: testConnection,
                uriToCheckCluster: uriToCheckCluster

        ],
        credential: [
                credentialName: 'credential',
                userName: userName,
                password: token
        ]
)