package com.softwood.utils

import groovy.json.JsonSlurper
import io.vertx.core.json.JsonObject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDateTime

class JsonUtilsTestsSpecification extends Specification {

    @Shared
            summaryJsonGenerator
    @Shared
            jsonGenerator

    def setupSpec() {
        JsonUtils.Options sumOptions = new JsonUtils.Options()
        sumOptions.registerTypeEncodingConverter(LocalDateTime) { it.toString() }
        sumOptions.excludeFieldByNames("ci")
        sumOptions.excludeNulls(true)
        sumOptions.summaryClassFormEnabled(true)

        summaryJsonGenerator = sumOptions.build()

        JsonUtils.Options options = new JsonUtils.Options()
        options.setExpandLevels(2)
        options.registerTypeEncodingConverter(LocalDateTime) { it.toString() }
        options.excludeFieldByNames("ci")
        options.excludeNulls(true)
        options.summaryClassFormEnabled(false)
        options.includeVersion(true)

        jsonGenerator = options.build()

    }

    @Unroll
    def "encodeBasicTypes"() {
        expect:
        jsonGenerator.toSoftwoodJson(simpleType).encode() == result

        where:
        simpleType || result
        //list
        [1, 2]     || /{"softwoodEncoded":{"version":"1.0"},"iterable":[1,2]}/
        //map
        [a: 2]     || /{"softwoodEncoded":{"version":"1.0"},"map":{"withMapEntries":[{"key":"a","value":2}]}}/
    }


    def "encodeThenDecodeListAndMapTypes"() {
        when: "we set encode a list and a map "
        def numjson = jsonGenerator.toSoftwoodJson(1)
        def numdec = jsonGenerator.toObject(Integer, numjson, JsonEncodingStyle.softwood)

        def strjson = jsonGenerator.toSoftwoodJson("hello")
        def strdec = jsonGenerator.toObject(String, strjson, JsonEncodingStyle.softwood)

        def ljson = jsonGenerator.toSoftwoodJson([1, 2])
        def ldec = jsonGenerator.toObject(List, ljson, JsonEncodingStyle.softwood)
        def mjson = jsonGenerator.toSoftwoodJson([a: 2, b: true, c: "name"])
        def mdec = jsonGenerator.toObject(Map, mjson, JsonEncodingStyle.softwood)

        then: "decoded object should contain same values "
        ldec == [1, 2]
        mdec == [a: 2, b: true, c: "name"]
        numdec == 1
        strdec == "hello"
    }

    def "simple class with fields"() {
        given: "simple class"
        Simple s = new Simple(name: "will", age: 55)

        when: "we encode as json "
        JsonObject json = jsonGenerator.toSoftwoodJson(s)

        then: "we expect "
        json.encode() == /{"softwoodEncoded":{"version":"1.0"},"entityData":{"entityType":"com.com.softwood.utils.Simple","name":"will","attributes":{"name":{"type":"String","value":"will"},"age":{"type":"Integer","value":55}}}}/
    }

    def "encode from parent -> child encode "() {
        given: "parent with two children "
        Parent p = new Parent(name: "Dad", children: [])
        Child c1 = new Child(name: "Jack", parent: p)
        Child c2 = new Child(name: "Jill", parent: p)
        p.children << c1
        p.children << c2

        when: "we encode parent as json "
        JsonObject json = jsonGenerator.toSoftwoodJson(p)
        Parent decodedObject = jsonGenerator.toObject(Parent, json, JsonEncodingStyle.softwood)

        JsonSlurper slurper = new JsonSlurper()
        Map result = slurper.parseText(json.encode())
        def children = result.entityData.collectionAttributes.children
        println "encoded parent: " + json.encodePrettily()
        def secondLevelChildRefToParent = children[1].entityData.attributes.parent.value

        then: "we expect "
        result.softwoodEncoded.version == "1.0"
        result.entityData.name == 'Dad'
        result.entityData.attributes.name.value == 'Dad'
        result.entityData.collectionAttributes.children.size == 2
        children[0].entityData.name == 'Jack'
        children[1].entityData.name == 'Jill'
        secondLevelChildRefToParent.entityData.isPreviouslyEncoded == true
        secondLevelChildRefToParent.entityData.shortForm == "Parent (name:Dad) "
        decodedObject.name == p.name
        decodedObject.children.size() == p.children.size()
        decodedObject.children[0].parent.name == p.name
        decodedObject.children[0].name == p.children[0].name
        decodedObject.children[1].name == p.children[1].name
        decodedObject.children[0].parent == decodedObject.children[1].parent

    }

    def "encode from second child of parent  encoded "() {
        given: "parent with two children "
        Parent p = new Parent(name: "Dad", children: [])
        Child c1 = new Child(name: "Jack", parent: p)
        Child c2 = new Child(name: "Jill", parent: p)
        p.children << c1
        p.children << c2

        when: "we encode parent as json "
        JsonObject json = jsonGenerator.toSoftwoodJson(c2)
        JsonSlurper slurper = new JsonSlurper()
        Map result = slurper.parseText(json.encode())
        def parent = result.entityData.attributes.parent.value
        //def children = result.entityData.collectionAttributes.children
        println "encoded child of parent: " + json.encodePrettily()
        def secondLevelParentRefToSecondChild = parent.entityData.collectionAttributes.children[1]
        Child decodedObject = jsonGenerator.toObject(Child, json, JsonEncodingStyle.softwood)


        then: "we expect "
        result.softwoodEncoded.version == "1.0"
        result.entityData.name == 'Jill'
        result.entityData.attributes.name.value == 'Jill'
        parent.entityData.name == 'Dad'
        //secondLevelParentRefToSecondChild.entityData.isPreviouslyEncoded == true
        //secondLevelParentRefToSecondChild.entityData.shortForm == "Child (name:Jill) "
    }

    def "encode class with private and transient fields"() {
        given: "class with number of real and imaginary accessors "
        Demo demo = new Demo(transientInt: 1, publicIntField: 2, privateIntField: 3)

        when: " we encode  the class "
        JsonObject json = jsonGenerator.toSoftwoodJson(demo)
        println "encoded Demo as : " + json.encodePrettily()
        JsonSlurper slurper = new JsonSlurper()
        Map result = slurper.parseText(json.encode())

        //should throw  exception
        result.entityData.attributes.getImaginaryFieldAccessor.value


        then: " expect, no transient and no false accessors  "
        result.entityData.attributes.publicIntField.value == 2
        result.entityData.attributes.privateIntField.value == 3 //if options.excludePrivateFields is true this wont show
        thrown(java.lang.NullPointerException)

    }


}

