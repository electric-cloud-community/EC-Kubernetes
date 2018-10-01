package com.electriccloud.spec

import spock.lang.*
import com.electriccloud.spec.*

class ContainerHelper extends PluginSpockTestSupport {

    def deployService(projectName, serviceName) {
        def processName = 'Deploy'
        def map = getEnvMap(projectName, serviceName)
        def envName = map.environmentName
        def envProjectName = map.environmentProjectName
        assert envName
        assert envProjectName

        def result = dsl """
            runServiceProcess(
                projectName: '$projectName',
                serviceName: '$serviceName',
                environmentName: '$envName',
                environmentProjectName: '$envProjectName',
                processName: '$processName',
            )
        """
        logger.debug(objectToJson(result))
        logger.debug("Polling job")
        def timeout = 400
        def time = 0
        def delay = 30
        while(jobStatus(result.jobId).status != 'completed' && time < timeout) {
            sleep(delay * 1000)
            time += delay
        }

        jobCompleted(result)
        def logs = readJobLogs(result.jobId)

        [jobId: result.jobId, logs: logs, outcome: jobStatus(result.jobId).outcome]
    }

    def undeployService(projectName, serviceName) {
        def processName = 'Undeploy'
        def map = getEnvMap(projectName, serviceName)
        def envName = map.environmentName
        def envProjectName = map.environmentProjectName
        assert envName
        assert envProjectName

        def result = dsl """
            runServiceProcess(
                projectName: '$projectName',
                serviceName: '$serviceName',
                environmentName: '$envName',
                environmentProjectName: '$envProjectName',
                processName: '$processName',
            )
        """
        pollJob(result.jobId)
        def logs = readJobLogs(result.jobId)

        [jobId: result.jobId, logs: logs, outcome: jobStatus(result.jobId).outcome]

    }


    def pollJob(jobId, timeout = 300) {
        def time = 0
        def delay = 30
        while(jobStatus(jobId).status != 'completed' && time < timeout) {
            sleep(delay * 1000)
            time += delay
        }

    }

    def readJobLogs(jobId) {
        def details = dsl """
            getJobDetails(jobId: '$jobId')
        """
        def workspace = details?.job?.workspace
        logger.debug(objectToJson(workspace))
        def path = workspace.unix[0]
        logger.debug(objectToJson(path))

        dsl """
            project 'Container Spec Helper', {
                procedure 'Read Logs', {
                    step 'Read Logs', {
                        command = '''
                        use strict;
                        use warnings;
                        use ElectricCommander;

                        my \$path = '\$[path]';
                        print "\$path\n";
                        opendir my \$dh, "\$path" or die \$!;
                        my @files = readdir \$dh;
                        close \$dh;

                        my @lines = ();
                        for my \$file (sort @files) {
                            print "\$file\n";
                            if (\$file =~ /checkCluster|createOrUpdateDeploy|discover/) {
                                push @lines, read_file("\$path/\$file");
                            }
                        }

                        my \$ec = ElectricCommander->new;
                        \$ec->setProperty('/myJob/ec_logs', join('', @lines));

                        sub read_file {
                            my (\$file) = @_;
                            print "\$file\n";
                            open my \$fh, "\$file";
                            my \$content = join('', <\$fh>);
                            close \$fh;
                            print \$content;
                            \$content = "\$file\n\$content";
                            return \$content;
                        }
                        '''
                        shell = 'ec-perl'
                    }
                    formalParameter 'path', defaultValue: '', {
                        type = 'entry'
                    }
                }
            }
        """

        def result = dsl """
            runProcedure(
                projectName: 'Container Spec Helper',
                procedureName: 'Read Logs',
                actualParameter: [
                    path: '$path'
                ]
            )
        """
        pollJob(result.jobId)

        assert jobStatus(result.jobId).outcome != 'error'

        def logs = dsl """
            getProperty(
                propertyName: '/myJob/ec_logs',
                jobId: '${result.jobId}',
                expand: 0
            )
        """
        logger.debug(objectToJson(logs))
        logs?.property?.value
    }

    def deleteService(projectName, serviceName) {
        dsl """
            deleteService(
                projectName: '$projectName',
                serviceName: '$serviceName'
            )
        """
    }

    def getEnvMap(projectName, serviceName) {
        def maps = dsl """
            getEnvironmentMaps(projectName: '$projectName', serviceName: '$serviceName')
        """
        assert maps?.environmentMap
        assert maps.environmentMap.size() == 1
        def map = maps.environmentMap[0]
        map
    }

    def runProcedureDsl(dslString) {
        assert dslString

        def result = dsl(dslString)
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
        def logs = readJobLogs(result.jobId)
        def outcome = jobStatus(result.jobId).outcome
        logger.debug("DSL: $dslString")
        logger.debug("Logs: $logs")
        logger.debug("Outcome: $outcome")
        [logs: logs, outcome: outcome, jobId: result.jobId]
    }

    def getAppScopedService(projectName, serviceName, applicationName, clusterName, envName) {
        def result = dsl """
            getServiceDeploymentDetails(
                projectName: '$projectName',
                serviceName: '$serviceName',
                applicationName: '$applicationName',
                clusterName: '$clusterName',
                environmentName: '$envName'
            )
        """
        result
    }
    def getService(projectName, serviceName, clusterName, envName) {
        def result = dsl """
            getServiceDeploymentDetails(
                projectName: '$projectName',
                serviceName: '$serviceName',
                clusterName: '$clusterName',
                environmentName: '$envName'
            )
        """
        result
    }

}
