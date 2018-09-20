package dsl.kubernetes

def names = args.names,
        configName = names.configName,
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
        procedureName: 'CreateConfiguration',
        actualParameter: [
                config: configName,
                credential: 'credential',
                desc: desc,
                logLevel: logLevel,
                clusterEndpoint: endpoint,
                kubernetesVersion: version,
                testConnection: testConnection,
                uriToCheckCluster: uriToCheckCluster
        ],
        credential: [
                credentialName: 'credential',
                userName: userName,
                password: token
        ]
)