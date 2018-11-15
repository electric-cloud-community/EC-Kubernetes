package com.electriccloud.procedures



import com.electriccloud.models.config.ConfigHelper
import org.apache.commons.lang.RandomStringUtils
import org.apache.log4j.Logger

import java.text.SimpleDateFormat
import java.util.regex.Pattern

abstract class TopologyMatcher extends NamingTestBase {

    def topologyOutcome

    // Parametrized names

    // Naming Helpers

    String unique(objectName) {
//        new SimpleDateFormat("${objectName}yyyyMMddHHmmssSSS".toString()).format(new Date())
        objectName + (new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()))
    }

    String characters(objectName, num) {
        num = num as Integer
        def _num
        if(num != 0) {
            _num = RandomStringUtils.random(num).next()
            return "${objectName}${_num}".toString()
        } else {
            return ''
        }
    }

    String characters(num) {
        characters('', num)
    }












    public static Logger log = Logger.getLogger("appLogger")





    def _link(Map fields) {

        log.info """
= Link =
$fields
"""

        def checkedNode = false

        def response = ConfigHelper.xml(topologyOutcome)

        def topologyResponse

        switch (fields.topologyType) {
            case 'clusterNode'    : topologyResponse = response.clusterNode.links.link; break
            case 'deployTopology' : topologyResponse = response.deployTopology.links.link; break
            default               : topologyResponse = response.clusterTopology.links.link
        }

        def xn = topologyResponse.findAll { link ->

            checkedNode = (
                    link.source == fields.source
                            &&
                            link.target == fields.target
            )
            checkedNode
        }
        xn
    }






    def _attribute(Map fields) {

        log.info """
= Attribute =
$fields
"""

        def response = ConfigHelper.xml(topologyOutcome)

        def checkedNode = false

        def xn = response.clusterNode.attributes.attribute.findAll { attribute ->

            checkedNode = (
                    attribute.name  == fields.name &&
                            attribute.type  == fields.type
            )

            if(fields.value != null) {
                checkedNode = (fields.value instanceof Pattern) ? checkedNode && (attribute.value ==~ fields.value) :
                        checkedNode && (attribute.value == fields.value)
            }

            def attName  = false
            def attValue = false

            if (fields.itemName != null)  {

                if(fields.index == null) {
                    attName = (fields.itemName instanceof Pattern) ? (attribute.value.items.name ==~ fields.itemName) :
                            (attribute.value.items.name == fields.itemName)
                    attValue = (fields.itemValue instanceof Pattern) ? (attribute.value.items.value ==~ fields.itemValue) :
                            (attribute.value.items.value == fields.itemValue)
                } else {
                    attName = (fields.itemName instanceof Pattern) ? (attribute.value.items.name[fields.index] ==~ fields.itemName) :
                            (attribute.value.items.name[fields.index] == fields.itemName)
                    attValue = (fields.itemValue instanceof Pattern) ? (attribute.value.items.value[fields.index] ==~ fields.itemValue) :
                            (attribute.value.items.value[fields.index] == fields.itemValue)
                }

                checkedNode = checkedNode && attName && attValue
            }

            checkedNode
        }
        xn
    }







    def _node(Map fields) {

        log.info """
= Node =   
$fields
"""

        def response = ConfigHelper.xml(topologyOutcome)

        def checkedNode = false

        def topologyResponse

        switch (fields.topologyType) {
            case 'clusterNode'    : topologyResponse = response.clusterNode; break
            case 'deployTopology' : topologyResponse = response.deployTopology; break
            default               : topologyResponse = response.clusterTopology.nodes.node
        }

        def xn = topologyResponse.findAll { node ->

            checkedNode = (
                    node.id   == fields.id &&
                            node.name == fields.name &&
                            node.type == fields.type
            )

            checkedNode = (fields.efRef == true) ?: checkedNode && (
                    node.electricFlowClusterName     == clusterName &&
                            node.electricFlowEnvironmentName == environmentName &&
                            node.electricFlowProjectName     == environmentProjectName
            )

            checkedNode = (fields.displayType != null) ?: checkedNode && (node.displayType == fields.displayType)
            checkedNode = (fields.efId != false) ?: checkedNode && (node.electricFlowIdentifier == fields.electricFlowIdentifier)
            checkedNode
        }
        xn
    }







    def _action(Map fields) {

        log.info """
= Action =
$fields
"""

        def response = ConfigHelper.xml(topologyOutcome)

        def checkedNode = false

        def xn = response.clusterNode.actions.action.findAll { action ->

            checkedNode = (
                    action.name         == fields.name &&
                            action.actionType   == fields.actionType &&
                            action.responseType == fields.responseType
            )
            checkedNode
        }
        xn
    }










}

