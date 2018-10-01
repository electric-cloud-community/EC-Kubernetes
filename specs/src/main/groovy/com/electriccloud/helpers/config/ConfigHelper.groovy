package com.electriccloud.helpers.config


import groovy.json.JsonBuilder
import groovy.util.slurpersupport.GPathResult
import net.sf.json.util.JSONBuilder
import org.apache.log4j.Logger

import java.nio.file.Paths

class ConfigHelper {

    private static Logger log = Logger.getLogger("appLogger")
    ConfigObject config

    ConfigHelper() {}

    ConfigHelper(fileName) {
        config = conf(fileName)
    }

    // Logging

    static def delimeter = "=".multiply(70)
    static def delimeter2 = "-".multiply(20)
    static def commandOutput = {text -> " \n${delimeter}\n${delimeter2} ${text.toUpperCase()} ${delimeter2}\n${delimeter}"}

    static def message(text, delimeter, delimeter2) {
        delimeter = delimeter.multiply(70)
        delimeter2 = delimeter2.multiply(20)
        log.info(" \n${delimeter}\n${delimeter2} ${text.toUpperCase()} ${delimeter2}\n${delimeter}")
    }

    static def message(text) {
        log.info(commandOutput(text))
    }



    // Configuration

    static ConfigObject confStatic(String fileName) {
        new ConfigSlurper().parse(new File(confPath(fileName)).toURI().toURL())
    }

    ConfigObject conf(String fileName) {
        config = confStatic(fileName)
    }

    // Parsing

    static GPathResult parse(xmlText) {
        xml(xmlText)
    }

    static GPathResult xml(xmlText) {
        GPathResult gpath = null
        gpath = new XmlSlurper().parseText(xmlText)
        gpath
    }

    // JSON

    def prettyJson(json) {
        new JsonBuilder(json).toPrettyString()
    }

    def jsonAsSting(json) {
        new JSONBuilder(json).toString()
    }

    // Files and Directories

    static String confPath(fileName) {
        Paths.get(
                "src",
                "test",
                "resources",
                "config",
                (!fileName.contains(".groovy")) ? "${fileName}.groovy" : fileName
        ).toString()
    }

    static String dslPath(String dirName, fileName) {
        def dsl = Paths.get(
                "src",
                "test",
                "resources",
                "dsl",
                dirName,
                (!fileName.contains(".dsl")) ? "${fileName}.dsl" : fileName
        )
        dsl.toString()
    }

    static String yamlPath(String dirName, fileName) {
        Paths.get(
                "src",
                "test",
                "resources",
                "dsl",
                dirName,
                (!fileName.contains(".yaml")) ? "${fileName}.yaml" : fileName
        ).toString()
    }

    static String buildPath(String fileName, String ... fileNames) {
        Paths.get(fileName, fileNames).toString()
    }



}
