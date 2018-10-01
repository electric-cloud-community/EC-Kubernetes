package com.electriccloud.procedures.installation

import com.electriccloud.procedures.KubernetesTestBase
import io.qameta.allure.Story
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test


class InstallationTests extends KubernetesTestBase {


    @AfterClass
    void tearDownClass(){
        def plugin = ectoolApi.installPlugin(pluginPath, pluginName).plugin
        ectoolApi.promotePlugin(plugin.pluginName).plugin
    }


    @BeforeMethod
    void stUpTest(){
        ectoolApi.deletePlugin(pluginName, pluginVersion)
    }


    @AfterMethod
    void tearDownTest(){
        ectoolApi.deletePlugin(pluginName, pluginVersion)
    }


    @Test
    @Story('Install plugin')
    void installPlugin(){
        def r = ectoolApi.installPlugin(pluginPath, pluginName).plugin
        assert r.pluginName == "${pluginName}-${pluginVersion}"
        assert r.pluginKey == pluginName
        assert r.pluginVersion == pluginVersion
        assert r.lastModifiedBy == "admin"
    }

    @Test
    @Story('Promote plugin')
    void promotePlugin(){
        def plugin = ectoolApi.installPlugin(pluginPath, pluginName).plugin
        def r = ectoolApi.promotePlugin(plugin.pluginName).plugin
        assert r.pluginName == "${pluginName}-${pluginVersion}"
        assert r.pluginKey == pluginName
        assert r.pluginVersion == pluginVersion
        assert r.lastModifiedBy == "admin"
    }

    @Test
    @Story('Uninstall plugin')
    void uninstallPlugin(){
        def plugin = ectoolApi.installPlugin(pluginPath, pluginName).plugin
        ectoolApi.promotePlugin(plugin.pluginName)
        def r = ectoolApi.uninstallPlugin(plugin.pluginName).property
        assert r.counter == "0"
        assert r.propertyName == "Default"
        assert r.lastModifiedBy == "admin"
        assert r.owner == "admin"
    }






}
