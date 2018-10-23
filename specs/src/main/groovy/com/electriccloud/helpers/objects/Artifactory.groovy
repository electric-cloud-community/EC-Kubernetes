package com.electriccloud.helpers.objects


import static com.electriccloud.helpers.enums.RepoTypes.RepoType

class Artifactory {

    def artifactoryConfig
    def artifactoryRepoType
    def artifactoryRepoKey
    def artifactoryOrgPath
    def artifactoryArtifactName
    def artifactoryArtifactVersion
    def artifactoryArtifactExtension

    Artifactory(artifactoryConfig, RepoType repoType, artifactoryRepoKey, artifactoryOrgPath, artifactoryArtifactName, artifactoryArtifactVersion, artifactoryArtifactExtension) {
        this.artifactoryConfig = artifactoryConfig
        this.artifactoryRepoType = repoType.getName()
        this.artifactoryRepoKey = artifactoryRepoKey
        this.artifactoryOrgPath = artifactoryOrgPath
        this.artifactoryArtifactName = artifactoryArtifactName
        this.artifactoryArtifactVersion = artifactoryArtifactVersion
        this.artifactoryArtifactExtension = artifactoryArtifactExtension
    }
}

