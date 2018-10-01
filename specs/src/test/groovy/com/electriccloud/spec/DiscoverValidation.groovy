package com.electriccloud.spec

import spock.lang.*
import com.electriccloud.spec.*

@Ignore
// EF Image lacks EC-Docker installed
class DiscoverValidation extends KubeHelper {
    static def projectName = 'EC-Kubernetes Validate Spec'

    def doSetupSpec() {
        dslFile 'dsl/Discover.dsl', [
            projectName: projectName,
            params: [
                envName: '',
                envProjectName: '',
                clusterName: '',
                namespace: '',
                projName: ''
            ]
        ]

    }


    def "Wrong cluster type"() {
        given:
            def envName = 'Kube Cluster'
            def clusterName = 'Docker'
            def configName = 'EC-Kubernetes Spec'

            dsl """
            project "$projectName", {
                environment "$envName", {
                    cluster "$clusterName", {
                        pluginKey = 'EC-Docker'
                        provisionProcedure = 'Check Cluster'
                        provisionParameter = [
                            config: 'config'
                        ]
                    }
                }
            }
            """
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Discover',
                    actualParameter: [
                        clusterName: '$clusterName',
                        namespace: 'default',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        projName: '$projectName'
                    ]
                )
            """
        then:
            // And now it should appear anew
            logger.debug(result.logs)
            assert result.outcome == 'error'
            assert result.logs =~ /ElectricFlow cluster 'Docker' in environment 'Kube Cluster' is not backed by a Kubernetes-based cluster/
        cleanup:
            dsl """
                deleteProject(projectName: '$projectName')
            """
    }

}
