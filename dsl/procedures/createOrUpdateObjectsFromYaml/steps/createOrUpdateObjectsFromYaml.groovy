$[/myProject/scripts/preamble]

@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

//// Input parameters
String clusterName = '$[clusterName]'
String clusterOrEnvProjectName = '$[clusterOrEnvProjectName]'
String environmentName = '$[environmentName]'
String yamlContent = '''$[yamlContent]'''.trim()

EFClient efClient = new EFClient()

KubernetesClient client = new KubernetesClient()
def pluginConfig = client.getPluginConfig(efClient, clusterName, clusterOrEnvProjectName, environmentName)
String accessToken = client.retrieveAccessToken(pluginConfig)
def clusterEndpoint = pluginConfig.clusterEndpoint

def objects = YamlUtils.getYamlObjects(yamlContent)

try {
    objects.each { object ->
        client.createOrUpdateObject(object, clusterEndpoint, accessToken)
    }
} catch (PluginException e) {
    efClient.handleProcedureError(e.getMessage())
}

class YamlUtils {
    private static final String DELIMITER = "---"
    private static Yaml yamlParser = new Yaml()

    static def getYamlObjects(def yamlContent) {
        def configList = yamlContent.split(DELIMITER)
        def parsedConfigList = []

        configList.each { config ->
            if (config) {
                def parsedConfig = yamlParser.load(config)
                parsedConfigList.push(parsedConfig)
            }
        }
        return parsedConfigList
    }
}