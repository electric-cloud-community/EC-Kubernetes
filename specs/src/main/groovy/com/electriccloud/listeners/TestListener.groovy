package com.electriccloud.listeners


import io.qameta.allure.Attachment
import org.apache.log4j.Logger
import org.testng.ITestContext
import org.testng.ITestResult
import org.testng.reporters.ExitCodeListener
import static com.google.common.io.Files.toByteArray
import static com.electriccloud.helpers.config.ConfigHelper.message


class TestListener extends ExitCodeListener {

    public static Logger log = Logger.getLogger('appLogger')


    static def logDir = "src/main/resources/logs"
    static def logFileName = "allureLog"

    @Override
    void onStart(ITestContext context) {
        super.onStart(context)
        //message("${context.getAllTestMethods().getMetaClass().toString()} test is failed", '==', '*')
    }

    @Override
    void onFinish(ITestContext context) {
        super.onFinish(context)
    }

    @Override
    void onTestFailure(ITestResult result) {
        super.onTestFailure(result)
        //def testNameColl = result.getMethod().getMethodName().split("(?=\\p{Upper})")


        message("${result.getMethod().getMethodName().replaceAll(/(?=[A-Z][a-z])|(?<=[a-z])(?=[A-Z])/) { ' ' + it }} test is failed", '==', '**')
        attachLog()
    }

    @Override
    void onTestSkipped(ITestResult result) {
        super.onTestSkipped(result)
        message("${result.getMethod().getMethodName().replaceAll(/(?=[A-Z][a-z])|(?<=[a-z])(?=[A-Z])/) { ' ' + it }} test is skipped", '==', '**')
        attachLog()

    }

    @Override
    void onTestSuccess(ITestResult result) {
        super.onTestSuccess(result)
        message("${result.getMethod().getMethodName().replaceAll(/(?=[A-Z][a-z])|(?<=[a-z])(?=[A-Z])/) { ' ' + it }} test is passed", '==', '**')
        attachLog()
    }


    @Override
    void onTestStart(ITestResult result) {
        super.onTestStart(result)
        message("${result.getMethod().getMethodName().replaceAll(/(?=[A-Z][a-z])|(?<=[a-z])(?=[A-Z])/) { ' ' + it }} test is started", '==', '**')
        cleanUpLog(logFileName)
    }


    static void cleanUpLog(fileName) {
        new File("${logDir}/${fileName}.log").withWriter { writer -> writer.write('') }
    }

    @Attachment(value = 'TestLog')
    static def attachLog(){
        log.info("Taking log to allure report")
        def file = new File("${logDir}/${logFileName}.log")
        toByteArray(file)
    }




}
