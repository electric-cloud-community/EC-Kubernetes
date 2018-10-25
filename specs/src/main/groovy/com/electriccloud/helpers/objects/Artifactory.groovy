package com.electriccloud.helpers.objects


class Artifactory {

    def config
    def repoType
    def repoKey
    def orgPath
    def artifactName
    def artifactVersion
    def artifactExtension

    Artifactory(config, repoType, repoKey, orgPath, artifactName, artifactVersion, artifactExtension) {
        this.config = config
        this.repoType = repoType.getName()
        this.repoKey = repoKey
        this.orgPath = orgPath
        this.artifactName = artifactName
        this.artifactVersion = artifactVersion
        this.artifactExtension = artifactExtension
    }
}

