package com.electriccloud.models.enums

class RepoTypes {


    enum RepoType {
        MAVEN("Maven"),
        NUGET("NuGet"),
        GENERIC("Generic")

        String name

        RepoType(name) {
            this.name = name
        }

    }



}