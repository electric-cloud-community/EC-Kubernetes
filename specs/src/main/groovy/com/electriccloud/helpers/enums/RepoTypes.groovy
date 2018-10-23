package com.electriccloud.helpers.enums

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