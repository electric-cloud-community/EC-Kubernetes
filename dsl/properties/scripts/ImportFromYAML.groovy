@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

public class ImportFromYAML extends ServiceFactory {
	static final String CREATED_DESCRIPTION = "Created by ImportFromYAML"
	static final String DELIMITER = "---"
	static final File SERVICE_FILE = "service_test.yaml" as File
	static final File DEPLOY_FILE = "deploy_test.yaml" as File
	static final File MULT_FILE = "mult_test.yaml" as File
	Yaml parser = new Yaml()

	def simpleImport(namespace){
		def readedService = parser.load(SERVICE_FILE.text)
		def readedDeploy = parser.load(DEPLOY_FILE.text)
		def efServices = []
		def efService = buildServiceDefinition(readedService, readedDeploy, namespace)
		efServices.push(efService)
		efServices
	}

	def readMultipleYAML(){
		String fileContents = MULT_FILE.text
		def configList = fileContents.split(DELIMITER)
		def parsedConfigList = []

		configList.each { config ->
			def parsedConfig = parser.load(config)
			// prettyPrint(parsedConfig)
			parsedConfigList.push(parsedConfig)
		}

		def services = getServices(parsedConfigList)
		def deployments = getDeployments(parsedConfigList)

		services.each { service ->
			println("SERVICES")
			prettyPrint(service)
		}
		deployments.each { deployment ->
			println("DEPLOYMENTS")
			prettyPrint(deployment)
		}

		services.each { kubeService ->
			//TBD match services to deployments and create a model
        }

		println("DONE")
	}
	
	def getServices(parsedConfigList){
		def services = []
		parsedConfigList.each { config ->
			if (config.kind == "Service")
			services.push(config)
		}
		services
	}

	def getDeployments(parsedConfigList){
		def deployments = []
		parsedConfigList.each { config ->
			if (config.kind == "Deployment")
		 deployments.push(config)
		}
	 	deployments
	}

	def getDeploymentsBySelector(deployments, key, value){
		def queryDeployments = []
		deployments.each { deployment -> 
			deployment.spec.selector.matchLabels.collect{ k, v ->
				if ((k == key) && (v == value)){
					queryDeployments.push(deployment)
				}
			}
		}
		queryDeployments
	}
}