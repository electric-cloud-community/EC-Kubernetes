@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

public class ImportFromYAML extends ServiceFactory {
	static final String CREATED_DESCRIPTION = "Created by ImportFromYAML"
	static final String DELIMITER = "---"
	Yaml parser = new Yaml()

	def importFromYAML(namespace, fileYAML){

		def efServices = []
//		String fileContents = fileYAML
		def configList = fileYAML.split(DELIMITER)
		def parsedConfigList = []

		configList.each { config ->
			def parsedConfig = parser.load(config)
			parsedConfigList.push(parsedConfig)
		}
		def services
		try {
			services = getParsedServices(parsedConfigList)
		}
		catch(Exception e) {
			println "Failed to find any services in the YAML file. Cause: ${e.message}"
			System.exit(-1)
		}

		def deployments
		try {
			deployments = getParsedDeployments(parsedConfigList)
		}
		catch(Exception e) {
			println "Failed to find any deployment configurations in the YAML file. Cause: ${e.message}"
			System.exit(-1)
		}

		services.each { kubeService ->
            if (!isSystemService(kubeService)) {
                def allQueryDeployments = []
                def i = 0
                kubeService.spec.selector.each{ k, v ->
                	i = i + 1
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
	
	def getParsedServices(parsedConfigList){
		def services = []
		parsedConfigList.each { config ->
			if (config.kind == "Service"){
				services.push(config)
			}
		}
		services
	}

	def getParsedDeployments(parsedConfigList){
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
		def i = 0
		deployments.each { deployment -> 
			deployment.metadata.labels.each{ k, v ->
				i = i + 1
				if ((k == key) && (v == value)){
					queryDeployments.push(deployment)
				}
			}
		}
		queryDeployments
	}

	// def readMap(map, stringPath, level = 0){
	// 	map.each{ k, v ->
	// 		if (v instanceof LinkedHashMap){
	// 			readMap(v, stringPath + k.toString() + ".", level + 1 )
	// 		}
	// 	}
	// }

}