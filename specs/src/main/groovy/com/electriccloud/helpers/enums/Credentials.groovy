package com.electriccloud.helpers.enums


class Credentials {

    enum Credential {
        DOCKER_HUB('DockerHub', 'ecplugins', 'ecloud4321', 'Test')

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


}
