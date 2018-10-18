package com.electriccloud.client


import java.nio.file.Paths
import java.util.concurrent.TimeoutException
import static groovyx.net.http.Method.*
import static com.electriccloud.helpers.config.ConfigHelper.message


class APIClient extends HttpClient {

    def sessionId
    def baseUri = "${System.getenv("COMMANDER_HOST")}/rest/v1.0"
    def ecUsername = System.getenv("COMMANDER_LOGIN")
    def ecPassword = System.getenv("COMMANDER_PASSWORD")
    def ecWorkspace = System.getenv("COMMANDER_WORKSPACE")

    def encode = {text -> URLEncoder.encode(text, "UTF-8") }
    def defaultHeaders() { [Cookie: "sessionId=$sessionId;", Accept: "application/json"] }


    APIClient() {
        log.info("Connected to '$baseUri'")
        this.sessionId   = login(ecUsername, ecPassword).json.sessionId
    }


    // Login

    def login(username, password) {
        log.info("Logged in as '$username'")
        def query = [ password: "$password", userName: "$username" ]
        request(baseUri,"sessions", POST, null, defaultHeaders(), query, true)
    }

    // DSL

    def dsl(dslText) {
        dsl(dslText, null)
    }

    def dsl(dslText, params) {
        def _params = params ? "&parameters=$params" : ''
        request(baseUri,"server/dsl?dsl=${encode(dslText)}$_params", POST, null, defaultHeaders(), null, false)
    }

    def dslFile(dslFilePath) {
        dsl(new File(dslFilePath).text)
    }

    def dslFile(dslFilePath, params) {
        dsl(new File(dslFilePath).text, params)
    }

    // Job

    def getJobStatus(jobId) {
        request(baseUri,"jobs/$jobId?request=getJobStatus", GET, null , defaultHeaders(), null, false)
    }

    def getJobDetails(jobId) {
        request(baseUri,"jobs/$jobId?request=getJobDetails", GET, null , defaultHeaders(), null, false)
    }

    def getJobSummary(jobId) {
        request(baseUri,"jobs/$jobId?request=getJobSummary", GET, null , defaultHeaders(), null, false)
    }

    def writeJobLog = { job -> log.info("\n${".".multiply(80)} \nJOB LOG: \n${getJobLogs(job)}${".".multiply(80)}") }


    def waitForJobToComplete(jobId, seconds = 10, periodSec = 1, message = "Job status: COMPLETED.") {
        def step = 0
        def periodTime = periodSec
        while (getJobStatus(jobId).json.status != "completed") {
            sleep(periodTime * 1000)
            step++

            log.info("Job status: pending; waiting for: ${periodTime * (step)} sec.")

            if (getJobStatus(jobId).json.outcome == "error") {
                writeJobLog(jobId)
                log.info("JOB STATUS: FAILED!")
                throw new RuntimeException("Job status: FAILED: \n ${getJobStatus(jobId).json} \n JOB ERROR LOG: \n${getJobLogs(jobId)}",
                        new Throwable("${getJobStatus(jobId).json.jobId}"))
            }

            if (seconds <= step * periodSec) {
                throw new TimeoutException("Time Out: \n The job status: ${getJobStatus(jobId).json.status} " +
                        "\n Summary: \n Job is not finnished in $seconds seconds!",)
            }

        }
        sleep(periodTime * 1000)
        writeJobLog(jobId)
        log.info(message)
    }



    // Project

    def deleteProject(projectName) {
        message('removing project')
        request(baseUri, "projects/$projectName", DELETE, null, defaultHeaders(), null, false)
        log.info("Project: ${projectName} is successfully removed!")
        return this
    }

    def createCredential(projectName, cred){
        message('creating credential')
        def uri = "projects/${projectName}/credentials?credentialName=${cred.credName}&description=${cred.description}&password=${cred.password}&passwordRecoveryAllowed=true&userName=${cred.userName}"
        request(baseUri, uri , POST, null, defaultHeaders(), null , false)
        log.info("Credential: ${cred.credName} is successfully created!")
    }

    def deleteCredential(projectName, credName){
        message('creating credential')
        def uri = "projects/${projectName}/credentials/${credName}"
        request(baseUri, uri , DELETE, null, defaultHeaders(), null , false)
        log.info("Credential: ${credName} is successfully removed!")
        return this
    }

    def deleteArtifact(artifactName){
        message('removing artifact')
        request(baseUri, "artifacts/${artifactName}", DELETE, null, defaultHeaders(), null, false)
        log.info("Artifact: ${artifactName} is successfully removed!")
        return this
    }

    // Services


    def deleteService(projectName, serviceName) {
        message('removing service')
        request(baseUri, "projects/${projectName}/services/${serviceName}", DELETE, null, defaultHeaders(), null, false)
        log.info("Service: ${serviceName} is successfully removed!")
        return this
    }


    def getService(projectName, serviceName) {
        message('getting service')
        log.info("Got Service: ${serviceName}")
        def response = request(baseUri, "projects/${projectName}/services/${serviceName}", GET, null, defaultHeaders(), null, true)
        response
    }

    def getServices(projectName) {
        message('getting services')
        log.info("Got Services from project: ${projectName}")
        def response = request(baseUri, "projects/${projectName}/services", GET, null, defaultHeaders(), null, true)
        response
    }

    def getApplicationServices(projectName, applicationName) {
        message('getting services')
        log.info("Got Services from project: ${projectName}")
        def response = request(baseUri, "projects/${projectName}/services?applicationName=${applicationName}", GET, null, defaultHeaders(), null, true)
        response
    }


    def getServiceContainer(projectName, serviceName, containerName){
        message('getting container')
        log.info("Got Services from project: ${projectName}")
        def response = request(baseUri, "projects/${projectName}/containers/${containerName}?serviceName=${serviceName}&includeDetails=true", GET, null, defaultHeaders(), null, true)
        response
    }

    def getServiceContainers(projectName, serviceName, withDetails){
        message('getting containers')
        log.info("Got Services from project: ${projectName}")
        def response = request(baseUri, "projects/${projectName}/services/${serviceName}/containers?includeDetails=true", GET, null, defaultHeaders(), null, true)
        response
    }

    // Applications


    def deleteApplication(projectName, applicationName) {
        message('removing application')
        request(baseUri, "projects/${projectName}/applications/${applicationName}", DELETE, null, defaultHeaders(), null, true)
        log.info("Service: ${applicationName} is successfully removed!")
        return this
    }


    def getApplication(projectName, applicationName) {
        message('getting application')
        log.info("Got Service: ${applicationName}")
        def response = request(baseUri, "projects/${projectName}/applications/${applicationName}", GET, null, defaultHeaders(), null, true)
        response
    }

    def getApplications(projectName) {
        message('getting applications')
        log.info("Got Services from project: ${projectName}")
        def response = request(baseUri, "projects/${projectName}/applications", GET, null, defaultHeaders(), null, true)
        response
    }

    def getApplicationService(projectName, applicationName, serviceName){
        message('getting application service')
        log.info("Got Services from application: ${applicationName}")
        def response = request(baseUri, "projects/${projectName}/applications/${applicationName}/services/${serviceName}", GET, null, defaultHeaders(), null, true)
        response
    }

    def getApplicationContainer(projectName, applicationName, serviceName, containerName){
        message('getting container')
        log.info("Got Services from project: ${projectName}")
        def response = request(baseUri, "projects/${projectName}/applications/${applicationName}/services/${serviceName}/containers/${containerName}?includeDetails=true", GET, null, defaultHeaders(), null, true)
        response
    }

    def getApplicationContainers(projectName, applicationName, serviceName){
        message('getting containers')
        log.info("Got Services from project: ${projectName}")
        def response = request(baseUri, "projects/${projectName}/applications/${applicationName}/services/${serviceName}/containers?includeDetails=true", GET, null, defaultHeaders(), null, true)
        response
    }

    def tierMappings(projectName, applicationName){
        def response = request(baseUri, "projects/${projectName}/applications/${applicationName}/tierMaps", GET, null, defaultHeaders(), null, true)
        response
    }


    // Environments

    def deleteEnvironment(projectName, environmentName) {
        message('removing environment')
        def response = request(baseUri, "projects/${projectName}/environments/${environmentName}", DELETE, null, defaultHeaders(), null, false)
        log.info("Service: ${environmentName} is successfully removed!")
        response
    }


    def getEnvironment(projectName, environmentName) {
        message('getting environment')
        log.info("Got Environment: ${environmentName}")
        def response = request(baseUri, "projects/${projectName}/environments/${environmentName}", GET, null, defaultHeaders(), null, true)
        response
    }

    def getEnvironments(environmentName) {
        message('getting environments')
        log.info("Got Environments from project: ${environmentName}")
        def response = request(baseUri, "projects/${environmentName}/environments", GET, null, defaultHeaders(), null, true)
        response
    }

    def getEnvCluster(projectName, environmentName, clusterName){
        message('getting environment cluster')
        log.info("Got Cluster from environment: ${environmentName}")
        def response = request(baseUri, "projects/${projectName}/clusters/${clusterName}?environmentName=${environmentName}", GET, null, defaultHeaders(), null, true)
        response
    }


    def getJobLogs(jobId, workspace) {
        def job = getJobSummary(jobId).json.job.jobStep.last()
        dsl("""new File("${Paths.get(workspace, job.jobName, job.logFileName).toString()}").text""").json.value.toString()
    }

    def getJobLogs(jobId) {
        def job = getJobSummary(jobId).json.job.jobStep.last()
        dsl("""
                import groovy.io.FileType

                def logs = []

                def dir = new File("${ecWorkspace}/$job.jobName")
                dir.eachFileMatch (FileType.FILES, ~/.*log/) { file ->
                    logs << file.text
                }
                
                StringBuilder stringBuilder = new StringBuilder()
                logs.each {
                    stringBuilder.append(it)
                }
                stringBuilder.toString()         
                                
        """).json.value
    }


    def getJobSteps(jobId){
        message('job steps')
        def resp = request(baseUri, "jobSteps?request=findJobSteps&jobId=${jobId}", GET, null, defaultHeaders(), null, true)
        resp
    }


    // Artifacts

    def getArtifactVersions(artifactName){
        def resp = request(baseUri, "artifactVersions?artifactName=$artifactName", GET, null, defaultHeaders(), null, true)
        resp
    }



}
