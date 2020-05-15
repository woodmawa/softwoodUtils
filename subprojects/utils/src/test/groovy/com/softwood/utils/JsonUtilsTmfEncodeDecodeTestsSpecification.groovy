package com.softwood.utils

import io.vertx.core.json.JsonObject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDateTime


public class JsonUtilsTmfEncodeDecodeTestsSpecification extends Specification {

    @Shared summaryJsonGenerator
    @Shared jsonGenerator

    def setupSpec () {
        JsonUtils.Options sumOptions = new JsonUtils.Options()
        sumOptions.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
        sumOptions.excludeFieldByNames("ci")
        sumOptions.excludeNulls(true)
        sumOptions.summaryClassFormEnabled(true)

        summaryJsonGenerator = sumOptions.build()

        JsonUtils.Options options = new JsonUtils.Options()
        options.setExpandLevels (2)
        options.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
        options.excludeFieldByNames("ci")
        options.excludeNulls(true)
        options.summaryClassFormEnabled(false)
        options.includeVersion(true)

        jsonGenerator = options.build()

    }

    @Unroll
    def "encodeBasicTypes" () {
        expect :
        jsonGenerator.toTmfJson (simpleType).encode() == result

        where   :
        //use parameterised spock test here - table of type and expected result
        simpleType || result
        //list
        [1,2,true,"ok"]      || /[1,2,true,"ok"]/
        //map
        [a:2, b:true]      || /{"a":2,"b":true}/
    }

    def "encodeThenDecodeListAndMapTypes" () {
        when : "we set up and encode a list and a map "
        def numjson = jsonGenerator.toTmfJson (1 )
        def numdec = jsonGenerator.toObject (Integer, numjson, JsonEncodingStyle.tmf)

        def strjson = jsonGenerator.toTmfJson ("hello" )
        def strdec = jsonGenerator.toObject (String, strjson, JsonEncodingStyle.tmf)

        def ljson = jsonGenerator.toTmfJson ([1,true,"ok"] )
        def ldec = jsonGenerator.toObject (List, ljson, JsonEncodingStyle.tmf)
        def mjson = jsonGenerator.toTmfJson ([a:2, b:true, c:"name"] )
        def mdec = jsonGenerator.toObject (Map, mjson, JsonEncodingStyle.tmf)

        then   : "decoded object should contain same values "
        ldec == [1,true,"ok"]
        mdec == [a:2, b:true, c:"name"]
        numdec == 1
        strdec == "hello"
    }

    def "simple class with fields" (){
        given : "simple class"
        Simple s = new Simple (name: "will", age:55)

        when: "we encode as json "
        JsonObject json = jsonGenerator.toTmfJson ( s)
        println json

        then : "we expect "
        json.encode() == /{"tmfEncoded":{"version":"1.0"},"@type":"com.com.softwood.utils.Simple","name":"will","age":55}/
    }
}
