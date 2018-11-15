package com.electriccloud.models.enums


class LogLevels {

    enum LogLevel {
        DEBUG("1"),
        INFO("2"),
        WARNING("3"),
        ERROR("4")

        String value

        LogLevel(String value){
            this.value = value
        }

    }


}