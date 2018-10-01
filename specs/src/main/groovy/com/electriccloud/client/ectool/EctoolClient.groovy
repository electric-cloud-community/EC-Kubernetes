package com.electriccloud.client.ectool


import org.apache.log4j.Logger
import org.json.JSONObject

import java.nio.file.Paths

class EctoolClient {

    static Logger log = Logger.getLogger("appLogger")

    def ectoolPath
    def electricFlowServerUrl
    def ecUsername
    def ecPassword

    def efHost
    def out

    EctoolClient() {

        ectoolPath             = System.getenv("ECTOOL_HOME") as String
        electricFlowServerUrl  = System.getenv("COMMANDER_HOST") as String
        ecUsername             = System.getenv("COMMANDER_LOGIN") as String
        ecPassword             = System.getenv("COMMANDER_PASSWORD") as String
    }

    // Execute commands

    String run(Object ... commandStr) {
        commandStr = commandStr as String[]
        if (commandStr[0].toString().toLowerCase().contains("ectool")) {
            return runEctool(commandStr)
        } else {
            StringBuilder builder = new StringBuilder()
            Process p
            try {
                p = Runtime.getRuntime().exec(commandStr)
                p.waitFor()
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(p.getInputStream())
                )
                BufferedReader error = new BufferedReader(
                        new InputStreamReader(p.getErrorStream())
                )
                String line
                int exitVal = p.waitFor()
                if(exitVal != 0) {
                    while((line = error.readLine()) != null) {
                        builder.append(line)
//                        log.info(line)
                    }
                } else {
                    while((line = input.readLine()) != null) {
                        builder.append(line)
//                        log.info(line)
                    }
                }
                out = builder.toString()
                return out
            } catch(Exception e) {
                out = e.toString()
//                log.info(out)
                e.printStackTrace()
                return null
            }
        }
    }

    String runEctool(Object ... commandStr) {
        commandStr = commandStr as String[]
//        log.info(arrayToStringConverter(commandStr))
        Process p
        String[] command
        try {
            efHost = new URL(electricFlowServerUrl).getHost()
            command = new String[commandStr.length + 2]
            command[0] = Paths.get(ectoolPath, "ectool").toString()
            command[1] = "--server"
            command[2] = efHost
            for (int i = 3; i < commandStr.length + 2; i++) {
                command[i]=commandStr[i-2]
            }
            p = Runtime.getRuntime().exec(command)
            EctoolProcessHandler inputStream = new EctoolProcessHandler(
                    p.getInputStream(), "INPUT")
            EctoolProcessHandler errorStream = new EctoolProcessHandler(
                    p.getErrorStream(), "ERROR")
            inputStream.run()
            errorStream.run()
            out = inputStream.getInput()
            if(out.empty)
                out = errorStream.getInput()
            return out
        } catch(Exception e) {
            out = e.toString()
//            log.info(out)
            e.printStackTrace()
            return out
        }
    }

    // String Processing

    String arrayToStringConverter(String ... array) {
        StringBuilder builder = new StringBuilder()
        for (int i = 0 ; i < array.length; i++) {
            if (array[i].contains(" ")) {
                builder.append(" \'" + array[i] + "\'")
            } else {
                builder.append(" " + array[i])
            }
        }
        return builder.toString()
    }

    String buildJsonForWin(Map<String, Object> data) {
        JSONObject json = new JSONObject()
        for (Map.Entry entry : data.entrySet()) {
            json.put((String) entry.getKey(), entry.getValue())
        }
        return "\"" +  json.toString().replaceAll("\"", "\\\\\"") + "\""
    }

    String buildJsonForLin(Map<String, Object> data) {
        JSONObject json = new JSONObject()
        for (Map.Entry entry : data.entrySet()) {
            json.put((String) entry.getKey(), entry.getValue())
        }
        return json.toString()
    }

    // Process Handling for Threads

    private class EctoolProcessHandler extends Thread {

        static Logger log = Logger.getLogger("appLogger")

        private InputStream inputStream
        private String streamType
        private StringBuilder input

        String getInput() {
            return this.input.toString()
        }

        EctoolProcessHandler(InputStream inputStream, String streamType) {
            this.inputStream = inputStream
            this.streamType = streamType
        }

        void run() {
            input = new StringBuilder()
            try {
                InputStreamReader inpStrd = new InputStreamReader(inputStream)
                BufferedReader buffRd = new BufferedReader(inpStrd)
                String line = null
                while ((line = buffRd.readLine()) != null) {
                    input.append(line)
//                    log.info(line)
                }
                buffRd.close()
            } catch (Exception e) {
//                log.info(e)
            }

        }
    }
}
