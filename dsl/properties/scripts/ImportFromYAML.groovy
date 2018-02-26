@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

public class ImportFromYAML extends ServiceFactory {
	static final String CREATED_DESCRIPTION = "Created by ImportFromYAML"
	static final String DELIMITER = "---"

	Yaml parser = new Yaml()

	// def simpleImport(namespace){
	// 	def readedService = parser.load(SERVICE_FILE.text)
	// 	def readedDeploy = parser.load(DEPLOY_FILE.text)
	// 	def efServices = []
	// 	def efService = buildServiceDefinition(readedService, readedDeploy, namespace)
	// 	efServices.push(efService)
	// 	efServices
	// }

	def importFromYAML(namespace, fileYAML){

		def efServices = []
		String fileContents = fileYAML.text
		def configList = fileContents.split(DELIMITER)
		def parsedConfigList = []

		configList.each { config ->
			def parsedConfig = parser.load(config)
			parsedConfigList.push(parsedConfig)
		}

		def services = getServices(parsedConfigList)
		def deployments = getDeployments(parsedConfigList)

		services.each { kubeService ->
            if (!isSystemService(kubeService)) {
                def allQueryDeployments = []
                kubeService.spec.selector.each{ k, v ->
                	def queryDeployments = getDeploymentsBySelector(deployments, k, v)
                    if (queryDeployments != null){
                    	queryDeployments.each{ deployment->
                    		allQueryDeployments.push(deployment)
                    	}
                    }
                }
                allQueryDeployments.each { deploy ->
                    def efService = buildServiceDefinition(kubeService, deploy, namespace)
                    efServices.push(efService)
                }
            }
        }

		efServices
	}
	
	def getServices(parsedConfigList){
		def services = []
		parsedConfigList.each { config ->
			if (config.kind == "Service"){
				services.push(config)
			}
		}
		services
	}

	def getDeployments(parsedConfigList){
		def deployments = []
		parsedConfigList.each { config ->
			if (config.kind == "Deployment"){
				deployments.push(config)
			}
		}
	 	deployments
	}

	def getDeploymentsBySelector(deployments, key, value){
		def queryDeployments = []
		deployments.each { deployment -> 
			deployment.spec.template.metadata.labels.each{ k, v ->
				if ((k == key) && (v == value)){
					queryDeployments.push(deployment)
				}
			}
		}
		queryDeployments
	}
}