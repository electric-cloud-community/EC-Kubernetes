package com.electriccloud.models.objects

class Credential {

    String credName
    String userName
    String password
    String description

    Credential(credName, userName, password, description){
        this.credName = credName
        this.userName = userName
        this.password = password
        this.description = description
    }

}
