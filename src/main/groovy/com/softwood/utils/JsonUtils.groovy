package com.softwood.utils

import com.sun.xml.internal.fastinfoset.util.CharArray
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

import javax.inject.Inject
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.security.InvalidParameterException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.Temporal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

enum JsonEncodingStyle {
    tmf, softwood, jsonApi
}

class JsonUtils {

    List jsonEncodableStandardTypes = [Integer, Long, Double, Float, byte[], Object, String, Boolean, Instant, JsonArray, JsonObject, CharSequence, Enum]
    //UUID, URI, URL, Date, LocalDateTime, LocalDate, LocalTime, Temporal, BigDecimal, BigInteger,
    List simpleAttributeTypes = [Number, Integer, Long, Float, Double, byte[], String, GString, Boolean, Instant, Character, CharSequence, Enum, UUID, URI, URL, Date, LocalDateTime, LocalDate, LocalTime, Temporal, BigDecimal, BigInteger]

    Map classForSimpleTypesLookup = ['Number': Number, 'Enum':Enum, 'Temporal': Temporal,
                                     'Date': Date, 'Calendar': Calendar, 'Instant':Instant,
                                     'LocalDateTime': LocalDateTime, 'LocalDate': LocalDate, 'LocalTime': LocalTime,
                                     'UUID':UUID, 'URI':URI, 'URL':URL,
                                     'String': String, 'GString':GString,
                                     'byte[]': Byte[], 'Byte': Byte, 'CharSequence': CharSequence, 'Character': Character,
                                     'Boolean':Boolean, 'Integer':Integer, 'Long':Long, 'Float':Float, 'Double':Double,
                                     'BigDecimal': BigDecimal, 'BigInteger':BigInteger]

    Map classInstanceHasBeenEncodedOnce = new LinkedHashMap()
    ThreadLocal<Integer> iterLevel = ThreadLocal.withInitial{0}

    List defaultGroovyClassFields = ['$staticClassInfo', '__$stMC', 'metaClass', '$callSiteArray']

    protected Options options
    private JsonUtils (Options options) {
        JsonObject.metaClass.getAt = {String s -> delegate.getValue(s)}
        this.options = options
    }

    /**
     * inner class to set options in fluent api form and then build the
     * generator with the options provided
     */
    class Options {

        @Inject String host = "localhost" //assumed default
        @Inject int port = 8080 //assumed default
        boolean optionalLinks = false
        boolean includeVersion = false
        boolean compoundDocument = false
        boolean excludeNulls = true
        boolean excludeClass = true
        boolean summaryEnabled = false
        boolean excludePrivateFields = false
        boolean excludeStaticFields = true
        List excludedFieldNames = []
        List excludedFieldTypes = []
        int expandLevels = 1
        JsonEncodingStyle jsonStyle = JsonEncodingStyle.softwood  // default full encoding

        Map converters = new HashMap<Class, Closure>()
        //todo too hard codes still
        Closure linkNamingStrategy = { String linkType, Class type, String attributeName, String id ->
            if (linkType == "self") {
                return "http://$host:$port/api/${type.simpleName}/$id"
            }
            if (linkType == "parent") {
                return "http://$host:$port/api/${type.simpleName}/$id/relationships/$attributeName"
            }
            if (linkType == "related") {
                return "http://$host:$port/api/${type.simpleName}/$id/$attributeName"

            }
        }

        //methods use method chainimg style

        Options () {
            converters.put(Date, {it.toString()})
            converters.put(Calendar, {it.toString()})
            converters.put(Temporal, {it.toString()})
            converters.put(URI, {it.toString()})
            converters.put(UUID, {it.toString()})
            this
        }

        Options setJsonEncodingStyle (JsonEncodingStyle style){
            jsonStyle = style
            this
        }

        Options setExpandLevels (level){
            expandLevels = level
            this
        }

        Options setHost (String hostname) {
            host = hostname
            this
        }

        Options setPort (int portNumber) {
            port = portNumber
            this
        }

        Options includeVersion  (boolean value = false) {
            includeVersion = value
            this
        }

        Options jsonApiIncludeVersion  (boolean value = false) {
            includeVersion = value
            this
        }

        Options jsonApiOptionalLinks  (boolean value = false) {
            optionalLinks = value
            this
        }

        Options jsonApiCompoundDocument  (boolean value = false) {
            compoundDocument = value
            this
        }

        Options summaryClassFormEnabled  (boolean value = false) {
            summaryEnabled = value
            this
        }

        Options excludePrivateFields (boolean value = false){
            excludePrivateFields = value
            this
        }

        Options excludeStaticFields (boolean value = true){
            excludeStaticFields = value
            this
        }

        Options excludeNulls (boolean value = true){
            excludeNulls = value
            this
        }

        Options excludeClass (boolean value = true) {
            excludeClass = value
            this
        }

        Options excludeFieldByNames(String name, args=null){
            excludedFieldNames << name
            if (args) {
                args.each {excludedFieldNames << it}
            }
            this
        }

        Options excludeFieldByNames(List<String> names){
            excludedFieldNames.addAll(names)
            this
        }

        Options excludeFieldByTypes(Class clazz, args=null){
            excludedFieldTypes << clazz
            if (args) {
                args.each {excludedFieldTypes << it}
            }
            this
        }

        Options excludeFieldByTypes(List<Class> clazzes){
            excludedFieldTypes.addAll(clazzes)
            this
        }

        Options registerConverter (Class clazz, Closure converter){
            converters.put (clazz, converter)
            this
        }

        JsonUtils build () {
            def generator = JsonUtils.newInstance(this)
            generator
        }
    }

    //only want the real fields, and not getXXX property access methods
    private Map getDeclaredFields (pogo) {
        Class clazz = pogo.getClass()
        List thisFields = []

        //get all the fields all way up hiererachy
        while (clazz) {
            thisFields.addAll (clazz.declaredFields)
            clazz = clazz.getSuperclass()
        }

        Map props = [:]

        //only collect non synthetic and non private
        thisFields.each { Field f ->
            def synthetic = f.isSynthetic()
            def privateField = Modifier.isPrivate(f.modifiers)  //test to see if private is set
            if (options.excludePrivateFields)   //will encode private fields by default
                return
            def transientField = Modifier.isTransient(f.modifiers)
            if (transientField) //skip transient fields
                return
            def staticField = Modifier.isStatic(f.modifiers)
            if (staticField && options.excludeStaticFields) //skip static fields
                return
            if(!synthetic) {
                    def accessible = f.isAccessible()
                    if (!accessible)
                        f.setAccessible(true)
                    //check its not in excludes category before adding to list of props to encode
                    if (isInExcludesCategory(f?.name, pogo.getClass())) {
                        if (!accessible)
                            f.setAccessible(accessible)  //reset to orig
                        return
                    }

                    props << ["$f.name": f.get(pogo)]
                    if (!accessible)
                        f.setAccessible(accessible)  //reset to orig


            }
        }
        props
    }


    //using softwood encoding at mo
    def toObject (Class<?> clazz, JsonObject json, JsonEncodingStyle style = options.jsonStyle) {
        int level = iterLevel.get()

        iterLevel.set (++level)
        if (clazz.isInterface()) {
            println "must be class not an, interface : $clazz.simpleName"
            return
        }

        def  instance = clazz.newInstance()
        Map result = json.getMap()

        if (json instanceof JsonArray) {
            //convert to List of
        } else if (json instanceof JsonObject)  {
            switch (style) {
                case JsonEncodingStyle.softwood:
                    if (json['iterable']) {
                        //just sent a simple Iterable to decode
                        def collectionAttributes = json['iterable']
                        for (jsonAtt in collectionAttributes)
                            decodeCollectionAttribute (instance, jsonAtt, style)
                    }
                    else if (json.getAt('map')?.getAt ('withMapEntries') ) {
                        //json is just a single jsonArray of map.entries
                        def mapAttributes = json['map']['withMapEntries']
                        for (jsonAtt in mapAttributes) {
                            def entry = decodeMapAttribute (instance, jsonAtt, style)
                            instance.putAll (entry)

                        }
                    } else if (json['value'] instanceof JsonArray) {
                        //json is just a single list
                        def collectionAttributes = entity['iterable']
                        for (jsonAtt in collectionAttributes) {
                            decodeCollectionAttribute (instance, jsonAtt, style)

                        }
                    } else if (json['entityData']){
                        def entity = json['entityData']

                        def jsonAttributes = entity["attributes"]
                        for (jsonAtt in jsonAttributes) {
                            decodeFieldAttribute (instance, jsonAtt, style)
                        }
                        //process any collections attributes
                        def collectionAttributes = entity['collectionAttributes']
                        for (jsonAtt in collectionAttributes) {
                            decodeCollectionAttribute (instance, jsonAtt, style)
                        }

                        def mapAttributes = entity['mapAttributes']
                        for (jsonAtt in mapAttributes) {
                            decodeMapAttribute (instance, jsonAtt, style)

                        }
                    }

                    instance
                    break
            }
        }
        else  {
            iterLevel.set (--level)
            throw new InvalidParameterException (message: "parameter should be of type JsonObject or JsonArray, found ${json.getClass()}")
        }
        iterLevel.set (--level)
        instance

    }

    private def decodeSimpleAttribute (attType, attValue, JsonEncodingStyle style) {
        switch (style) {
            case JsonEncodingStyle.softwood:
                if (Number.isAssignableFrom (attType) || attType == Enum || attType == Boolean)
                    return attValue
                else if (attType == String || attType == CharArray)
                    return attValue
                else if (attType == byte[])
                    return attValue
                else if (attType == Date) {
                    return  new SimpleDateFormat('EEE MMM dd HH:mm:ss Z yyyy').parse(attValue)
                }
                else if (attType == LocalDateTime || attType == LocalDate || attType == LocalTime) {
                    return attType.parse (attValue)
                }
                else if (attType == URI) {
                    return new URI (attValue)
                }
                else if (attType == URL) {
                    return new URI (attValue)
                }
                else if (attType == UUID) {
                    return UUID.fromString (attValue)
                }

                break
        }
    }

    private def decodeFieldAttribute (instance, jsonAtt, JsonEncodingStyle style) {
        switch (style) {
            case JsonEncodingStyle.softwood:
                String attName = jsonAtt['key']
                String camelCasedAttName = attName.substring (0,1).toUpperCase() + attName.substring (1)
                //def typeStr = jsonAtt['value']['type']
                def attType = classForSimpleTypesLookup[ jsonAtt['value']['type'] ]
                def attValue = jsonAtt['value']['value']
                if (attType !=null && isSimpleAttribute(attType)) {
                    //just set prop value in corresponding field in the instance
                    //test if respondsTo?...
                    boolean supports = instance.respondsTo("set$camelCasedAttName", attValue)
                    if (supports)
                        //if setter exists for AttValue then use it
                        instance["$jsonAtt.key"] = attValue
                    else {
                        /*check to see if Class offers a converter */
                        instance["$jsonAtt.key"] = decodeSimpleAttribute(attType, attValue, style)
                    }
                } else {
                    //todo have to decode complex attribute
                }
                break  //end softwood encoded field

            case JsonEncodingStyle.tmf:
                break //end tmf encoded field

        }

    }

    private def decodeCollectionAttribute (instance, collectionAtt, JsonEncodingStyle style) {
        switch (style ) {
            case JsonEncodingStyle.softwood:
                if (instance instanceof Collection ) {
                    //we are building a list of items, decode the colelctionAtt and add to 'instance' collection
                    if (isSimpleAttribute(collectionAtt)) {
                        instance.add collectionAtt
                        return instance
                    } else if (collectionAtt['entityData']) {
                        //json attribute is an complex entity expression so decode and all to instance 

                    }

                }

                String attName = collectionAtt['key']
                def iterableAttValue = collectionAtt['value']

                //use reflection to get field type
                Field field = instance.getClass().getDeclaredField ("$attName")
                switch (field.type) {
                    case ArrayList:
                    case List:
                        instance["$attName"] = new ArrayList()
                        break
                    case Collection:
                    case Iterable:
                    case ConcurrentLinkedQueue:
                    case Queue:
                        instance["$attName"] = new ConcurrentLinkedQueue<>()
                        break
                    case ConcurrentLinkedDeque:
                    case Deque:
                        instance["$attName"] = new ConcurrentLinkedDeque<>()
                        break
                    case HashSet:
                    case Set:
                        instance["$attName"] = new HashSet<>()
                        break

                }
                def instCollAtt = instance["$attName"]
                for (item in iterableAttValue ) {
                    def clazz, value, proxy, iterableInstance, isEntity = false
                    String clazzName
                    if (item instanceof JsonObject) {
                        //either a map object - or could be an encoded jsonObject as map
                        if (item['type']) {
                            clazz = classForSimpleTypesLookup[ (item['type'])]
                            value = item['value']
                        } else if (item['entityData'] ) {
                            isEntity = true
                            clazzName = item['entityData']['entityType']
                        }

                    } else {
                        clazz = item.getClass()
                        value = item
                    }
                    if (!isEntity && isSimpleAttribute(clazz))
                        instance["$attName"] << decodeSimpleAttribute(clazz, value, style)
                    else {
                        try {
                            clazz = Class.forName(clazzName)
                            iterableInstance = toObject(clazz, item, style)
                            instance["$attName"].add (iterableInstance)
                        } catch (RuntimeException ex) {
                            //encoded iterable entity element doesn't exist in current vm - build a proxy and add that
                            println "class $clazzName not in current vm - build an expando proxy instead"
                            proxy = new Expando()
                            proxy.setProperty('isProxy':true)
                            proxy.setProperty ('proxiedClass', clazzName)
                            if (item.id)
                                proxy.setProperty('id', item['id'])
                            if (item.name)
                                proxy.setProperty('id', item['name'])
                            def attributes = (item['entityData']['attributes'] ?: [])
                            for (jsonAtt in attributes) {
                                proxy.setProperty(jsonAtt['key'], jsonAtt['value'])
                            }
                            instance["$attName"].add (proxy)

                        }
                    }
                }
                break
            case JsonEncodingStyle.tmf:
                break

        }
        instance

    }

    private def decodeMapAttribute (instance, mapAtt, JsonEncodingStyle style) {
        switch (style ) {
            case JsonEncodingStyle.softwood:
                String attName = mapAtt['key']
                def attValue = mapAtt['value']

                if (isSimpleAttribute(attValue)) {
                    instance.put (attName, attValue)
                    return instance
                }

                //rewrite this
                def listOfMapEntries = mapAtt['value']['withMapEntries']

                //use reflection to get field type
                Field field = instance.getClass().getDeclaredField ("$attName")
                switch (field.type) {
                    case HashMap:
                        instance["$attName"] = new HashMap()
                        break
                    case ConcurrentHashMap:
                    case Map:
                        instance["$attName"] = new ConcurrentHashMap<>()
                        break
                }
                def instMapAtt = instance["$attName"]
                for (item in listOfMapEntries ) {
                    def clazz, clazzName, value, key, mapInstance, proxy
                    boolean isEntity=false

                    if (item['value']) {
                        value = item['value']
                        clazz = value.getClass()
                        //clazz = classForSimpleTypesLookup[ (item['type'])]
                        value = item['value']
                    } else if (item['value']['entityData'] ) {
                        isEntity = true
                        clazzName = item['entityData']['entityType']
                    }

                    if (!isEntity && isSimpleAttribute(clazz))
                        instance["$attName"].put (key,  decodeSimpleAttribute(clazz, value, style))
                    else {
                      try {
                          clazz = Class.forName(clazzName)
                          mapInstance = toObject(clazz, item, style)
                          instance["$attName"].add (mapInstance)
                      } catch (RuntimeException ex) {
                          //encoded iterable entity element doesn't exist in current vm - build a proxy and add that
                          println "class $clazzName not in current vm - build an expando proxy instead"
                          proxy = new Expando()
                          proxy.setProperty('isProxy':true)
                          proxy.setProperty ('proxiedClass', clazzName)
                          if (item.id)
                              proxy.setProperty('id', item['id'])
                          if (item.name)
                              proxy.setProperty('id', item['name'])
                          def attributes = (item['entityData']['attributes'] ?: [])
                          for (jsonAtt in attributes) {
                              proxy.setProperty(jsonAtt['key'], jsonAtt['value'])
                          }
                          instance["$attName"].add (proxy)

                      }
                    }
                }
                break
            case JsonEncodingStyle.tmf:
                break

        }
        instance
    }

    //wrapper method just invoke correct method based on default jsonStyle
    def toJson (def pogo, String named= null, JsonEncodingStyle style = options.jsonStyle){
        def encodedResult
        switch (style) {
            case JsonEncodingStyle.softwood :
                encodedResult = toSoftwoodJson(pogo, named)
                break
            case JsonEncodingStyle.tmf :
                encodedResult = toTmfJson(pogo, named)
                break
            case JsonEncodingStyle.jsonApi :
                encodedResult = toJsonApi (pogo) //need extra attribute ?
                break

        }
        encodedResult
    }

    @CompileStatic
    def toTmfJson (def pogo, String named= null) {
        def json = new JsonObject()

        int level = iterLevel.get()
        if (iterLevel.get() == 0) {
            if (options.includeVersion) {
                JsonObject metaData = new JsonObject()
                metaData.put("version", "1.0")
                json.put("tmfEncoded", metaData)
            }
        }

        iterLevel.set (++level)

        if (pogo == null){
            iterLevel.set (--level)
            return pogo
        } else if (isSimpleAttribute(pogo.getClass())) {
            if ( named && isJsonStandardEncodableAttribute(pogo.getClass()) ) {
                json.put("$named", pogo)
            } else {
                iterLevel.set (--level)
                return encodeSimpleType(pogo, JsonEncodingStyle.tmf)
            }
        } else if (Iterable.isAssignableFrom(pogo.getClass()) )
                 if (named) {
                     JsonArray encIterable = encodeIterableType(pogo as Iterable, JsonEncodingStyle.tmf)
                     (json as JsonObject).put("$named".toString(), encIterable)
                 }
                 else
                     json = encodeIterableType(pogo as Iterable, JsonEncodingStyle.tmf)
        else if (Map.isAssignableFrom(pogo.getClass()))
                 if (named) {
                     JsonObject encMap = encodeMapType(pogo as Map, JsonEncodingStyle.tmf)
                     (json as JsonObject).put("$named".toString(), encMap )
                 }
                 else
                     json = encodeMapType(pogo as Map, JsonEncodingStyle.tmf)
        else {
            //json = new JsonObject()
            if (classInstanceHasBeenEncodedOnce[(pogo)]) {
                //println "already encoded pogo $pogo so just put toString summary and stop recursing"

                JsonObject wrapper = new JsonObject()
                JsonObject jsonObj = new JsonObject()

                jsonObj.put ("isPreviouslyEncoded", true)
                jsonObj.put ("@type", pogo.getClass().canonicalName)
                if (pogo.hasProperty ("id"))
                    jsonObj.put ("id", (pogo as GroovyObject).getProperty ("id").toString())
                jsonObj.put ("shortForm", pogo.toString())
                wrapper.put ("entity", jsonObj)
                iterLevel.set (--level)
                return wrapper // pogo.toString()
            }

            if (!classInstanceHasBeenEncodedOnce.containsKey((pogo))) {
                classInstanceHasBeenEncodedOnce.putAll([(pogo): new Boolean(true)])
            }

            Map props = getDeclaredFields (pogo)

             def jsonFields = new JsonObject()
            def jsonEntityReferences = new JsonObject()

           def type = pogo.getClass().canonicalName
            jsonFields.put ("@type", type)

            for ( prop in props) {

                if (Iterable.isAssignableFrom(prop.value.getClass())) {
                    def arrayResult = encodeIterableType ( prop.value as Iterable , JsonEncodingStyle.tmf)
                    if (arrayResult) {
                        jsonFields.put(prop.key.toString(), arrayResult)
                    } else {
                        if (!options.excludeNulls)
                            jsonFields.putNull(prop.key.toString())
                    }
                } else if (Map.isAssignableFrom(prop.value.getClass())) {
                    def result = encodeMapType ( prop.value as Map, JsonEncodingStyle.tmf)
                    if (result) {
                        jsonFields.put(prop.key.toString(), result)
                    } else {
                        if (!options.excludeNulls)
                            jsonFields.putNull(prop.key.toString())
                    }
                } else {
                    //must be an ordinary field
                    def field = encodeFieldType(prop, JsonEncodingStyle.tmf)
                    if (field ) {
                        def wrapper = new JsonObject()
                        jsonFields.put (prop.key.toString(), field )
                   } else {
                        if (!options.excludeNulls)
                            jsonFields.putNull(prop.key.toString())

                    }
                }
                json = jsonFields
            }
        }

        iterLevel.set (--level)
        if (iterLevel.get() == 0) {
            classInstanceHasBeenEncodedOnce.clear()
        }
        json


    }


    /**
     * encode pogo and if name is build jsonObject of name and encoded value
     * @param pogo
     * @param name optional name, if set used as key for encoded basic type value
     * @return
     */
    @CompileStatic
    def toSoftwoodJson(def pogo, String named= null) {

        def json = new JsonObject()

        int level = iterLevel.get()
        if (iterLevel.get() == 0) {
            if (options.includeVersion) {
                JsonObject metaData = new JsonObject()
                metaData.put("version", "1.0")
                json.put("softwoodEncoded", metaData)
            }
        }

        iterLevel.set (++level)

        if (pogo == null){
            iterLevel.set (--level)
            return pogo
        }
        else if (Iterable.isAssignableFrom(pogo.getClass()) )
            if (named) {
                json.put ("$named".toString(),  encodeIterableType(pogo as Iterable))
            } else
                json.put ("iterable",  encodeIterableType(pogo as Iterable))
        else if (Map.isAssignableFrom(pogo.getClass()))
            if (named)
                json.put ("$named".toString(),  encodeMapType(pogo as Map))
            else
                json.put ("map",  encodeMapType(pogo ))
        else if ( isSimpleAttribute(pogo.getClass()) ){
            if ( named && isJsonStandardEncodableAttribute(pogo.getClass()) ) {
                json.put("$named", pogo)
            } else {
                iterLevel.set (--level)
                return encodeSimpleType(pogo, JsonEncodingStyle.softwood)
            }
        }
        else {
            //json = new JsonObject()
            if (classInstanceHasBeenEncodedOnce[(pogo)]) {
                //println "already encoded pogo $pogo so just put toString summary and stop recursing"

                JsonObject wrapper = new JsonObject()
                JsonObject jsonObj = new JsonObject()

                jsonObj.put ("entityType", pogo.getClass().canonicalName)
                jsonObj.put ("isPreviouslyEncoded", true)
                if (pogo.hasProperty ("id"))
                    jsonObj.put ("id", (pogo as GroovyObject).getProperty ("id").toString())
                jsonObj.put ("shortForm", pogo.toString())
                wrapper.put ("entityData", jsonObj)
                iterLevel.set (--level)
                return wrapper // pogo.toString()
            }

            if (!classInstanceHasBeenEncodedOnce.containsKey((pogo))) {
                classInstanceHasBeenEncodedOnce.putAll([(pogo): new Boolean(true)])
                //println "iterLev $iterLevel: adding pogo $pogo encoded once list"
            }


            //Map props = pogo.properties
            Map props = getDeclaredFields (pogo)
            def iterableFields = props.findAll { def clazz = it?.value?.getClass()
                if (clazz)
                    Iterable.isAssignableFrom(clazz)
                else
                    false
            }
            def mapFields = props.findAll { def clazz = it?.value?.getClass()
                if (clazz)
                    Map.isAssignableFrom(clazz)
                else
                    false
            }
            def nonIterableFields = props - iterableFields - mapFields

            //println "toSoftwoodJson: pogo ($pogo) has nonIterableFields $nonIterableFields at  iterLev ($iterLevel) "

            def jsonFields = new JsonObject()
            def jsonAttributes = new JsonObject()
            def jsonEntityReferences = new JsonObject()
            def jsonCollections = new JsonObject()
            def jsonMaps = new JsonObject()

            for (prop in nonIterableFields) {

                def field = encodeFieldType(prop, JsonEncodingStyle.softwood)
                if (field ) {
                    def wrapper = new JsonObject()
                    wrapper.put ("type", prop.value.getClass().simpleName)
                    if (!isSimpleAttribute(prop.value.getClass())) {
                        def id = (prop.value.hasProperty("id")) ? (prop.value as GroovyObject).getProperty("id").toString() : "<not defined>"
                        def name = (prop.value.hasProperty("name")) ? (prop.value as GroovyObject).getProperty("name") : "<not defined>"
                        if (id != "<not defined>")
                            wrapper.put("id", id)
                        if (name != "<not defined>")
                            wrapper.put("name", name)
                    }
                    wrapper.put ("value", field )
                    jsonAttributes.put (prop.key.toString(), wrapper )
                }
            }
            for (prop in iterableFields){
                def arrayResult = encodeIterableType ( prop.value as Iterable, JsonEncodingStyle.softwood)
                if (arrayResult) {
                    //jsonFields.put(prop.key, arrayResult)
                    jsonCollections.put(prop.key.toString(), arrayResult)
                }
            }
            for (prop in mapFields){
                def result = encodeMapType ( prop.value as Map, JsonEncodingStyle.softwood)
                if (result) {
                    jsonMaps.put(prop.key.toString(), result)
                }
            }

            def id = (pogo.hasProperty("id")) ? (pogo as GroovyObject).getProperty("id").toString() : "<not defined>"
            def name = (pogo.hasProperty("name")) ? (pogo as GroovyObject).getProperty("name") : "<not defined>"
            def type = pogo.getClass().name
            jsonFields.put ("entityType", type)
            if (!isSimpleAttribute(pogo.getClass())) {
                if (id != "<not defined>")
                    jsonFields.put("id", id)
                if (name != "<not defined>" )
                    jsonFields.put("name", name)
            }
            jsonFields.put ("attributes", jsonAttributes)
            if (options.excludeNulls == true ) {
                if (jsonCollections.size() > 0 )
                    jsonFields.put("collectionAttributes", jsonCollections)
                if (jsonMaps.size() > 0 )
                    jsonFields.put("mapAttributes", jsonMaps)

            } else {
                jsonFields.put("collectionAttributes", jsonCollections)
                jsonFields.put("mapAttributes", jsonMaps)

            }

            json.put ("entityData", jsonFields)
        }
        iterLevel.set (--level)
        if (iterLevel.get() == 0) {
            classInstanceHasBeenEncodedOnce.clear()
        }
        json

    }

    Closure getLinkNamingStrategy () {
        options.linkNamingStrategy
    }

    @CompileStatic
    def toJsonApi (def pogo, JsonArray includedArray = null) {

        int level = iterLevel.get()
        iterLevel.set (++level)
        JsonArray compoundDocumentIncludedArray
        def jsonApiEncoded = true


        def jsonApiObject = new JsonObject()
        JsonObject jsonAttributes = new JsonObject()
        JsonObject jsonRelationships = new JsonObject()

        if (iterLevel == 1 && options.compoundDocument){
            compoundDocumentIncludedArray = new JsonArray()
            includedArray = compoundDocumentIncludedArray
        }

        if (Iterable.isAssignableFrom(pogo.getClass()) )
            jsonApiObject =  encodeIterableType( pogo as Iterable, JsonEncodingStyle.jsonApi)
        else if (Map.isAssignableFrom(pogo.getClass()))
            jsonApiObject =  encodeMapType ( pogo as Map, JsonEncodingStyle.jsonApi, includedArray )
        else {
            def json = new JsonObject()
            if (classInstanceHasBeenEncodedOnce[(pogo)]) {
                println "already encoded pogo $pogo so just stop recursing"

                iterLevel.set (--level)
                return
            }

            if (!classInstanceHasBeenEncodedOnce.containsKey((pogo))) {
                classInstanceHasBeenEncodedOnce.putAll([(pogo): new Boolean(true)])
                //println "iterLev $iterLevel: adding pogo $pogo encoded once list"
            }

            Map props = pogo.properties
            def iterableFields = props.findAll { def clazz = it?.value?.getClass()
                if (clazz)
                    Iterable.isAssignableFrom(clazz)
                else
                    false
            }
            def mapFields = props.findAll { def clazz = it?.value?.getClass()
                if (clazz)
                    Map.isAssignableFrom(clazz)
                else
                    false
            }
            def nonIterableFields = props - iterableFields - mapFields

            jsonAttributes = new JsonObject()
            //do attributes
            for (prop in nonIterableFields) {

                def field = encodeFieldType(prop, JsonEncodingStyle.jsonApi, includedArray)
                if (field ) {
                    //if field is itself a JsonObject add to relationhips
                    if (field instanceof JsonObject) {
                        def id = (prop.value.hasProperty("id")) ? (pogo as GroovyObject).getProperty("id").toString() : "unknown"
                        def altId = (prop.value.hasProperty("name")) ? (pogo as GroovyObject).getProperty("name") : "unknown"
                        if (id == "unknown" && altId != "unknown")
                            id = altId
                        def type = prop.value.getClass().simpleName
                        JsonObject container = new JsonObject()
                        JsonObject data = new JsonObject()
                        data.put("type", type)
                        data.put("id", id)
                        if (options.optionalLinks) {
                            JsonObject links = new JsonObject()
                            Closure linkNames = this.getLinkNamingStrategy()
                            links.put("self", linkNames ("parent", pogo.getClass(), (String)prop.key, "$id" ))
                            links.put("related", linkNames ("related", pogo.getClass(), (String)prop.key, "$id" ))
                            container.put("links", links)
                        }
                        container.put("data", data)
                        jsonRelationships.put ((String)prop.key, container)
                    }
                    else
                        //if basic field type - add to attributes
                        jsonAttributes.put((String)prop.key, field)
                }

            }

            //do list type relationships
            for (prop in iterableFields){
                def arrayResult = encodeIterableType ((Iterable)prop.value, JsonEncodingStyle.jsonApi, includedArray)
                if (arrayResult) {
                    JsonObject container = new JsonObject ()
                    if (options.optionalLinks) {
                        //then we need to reference the links from parent pogo to its property we have just encoded
                        JsonObject links = new JsonObject()
                        def id = (pogo.hasProperty("id")) ? (pogo as GroovyObject).getProperty("id").toString() : "unknown"
                        def altId = (pogo.hasProperty("name")) ? (pogo as GroovyObject).getProperty("name") : "unknown"
                        if (id == "unknown" && altId != "unknown")
                            id = altId
                        def idStr = "$id".toString()
                        Closure linkNames = this.getLinkNamingStrategy()

                        links.put ("self", linkNames ("parent", pogo.getClass(), (String)prop.key, idStr))
                        links.put ("related", linkNames ("related", pogo.getClass(), (String)prop.key, idStr))
                        container.put("links", links)
                    }
                    container.put("data", arrayResult)
                    jsonRelationships.put ("$prop.key", container)
                }
            }

            //do map type relationships
            for (prop in mapFields){
                //prop is mapEntry to key = attribute name, and value is the real map
                def arrayResult = encodeMapType ((prop.value as Map), JsonEncodingStyle.jsonApi, includedArray)
                if (arrayResult) {
                    JsonObject container = new JsonObject ()
                    if (options.optionalLinks) {
                        JsonObject links = new JsonObject()
                        def id = (pogo.hasProperty("id")) ? (pogo as GroovyObject).getProperty("id").toString() : "unknown"
                        def altId = (pogo.hasProperty("name")) ? (pogo as GroovyObject).getProperty("name") : "unknown"
                        if (id == "unknown" && altId != "unknown")
                            id = altId
                        def idStr = "$id".toString()
                        Closure linkNames = this.getLinkNamingStrategy()

                        links.put ("self", linkNames ("parent", pogo.getClass(), (String)prop.key, idStr))
                        links.put ("related", linkNames ("related", pogo.getClass(), (String)prop.key, idStr))
                        container.put("links", links)
                    }
                    container.put("data", arrayResult)
                    jsonRelationships.put ("$prop.key", container)
                }
            }

            if (options.compoundDocument) {
                if (iterLevel.get() > 1) {
                    //construct this sublevels object for compoundDoc included section
                    //ensures we dont encode object as well as put in included section
                    JsonObject container = new JsonObject()

                    String type  = pogo.getClass().simpleName
                    String id = pogo.hasProperty("id") ? (pogo as GroovyObject)?.getProperty("id") : "1"
                    container.put("type", type)
                    container.put("id", id)
                    if (jsonAttributes && jsonAttributes.size() != 0)
                        container.put("attributes", jsonAttributes)
                    if (jsonRelationships && jsonRelationships.size() != 0)
                        container.put("relationships", jsonRelationships)
                    if (options.optionalLinks) {
                        JsonObject links = new JsonObject()
                        Closure linkNames = getLinkNamingStrategy()
                        links.put("self", linkNames ("self", pogo.getClass(), "", id ))
                        container.put("links", links)
                    }

                    (includedArray as JsonArray).add(container)
                }
            }

        }

        //for non sub level object
        if (iterLevel.get() > 1) {
            if (jsonRelationships && jsonRelationships.size() >0)
                (jsonApiObject as JsonObject).put("relationships", jsonRelationships)

        }

        iterLevel.set (--level)
        if (iterLevel.get() == 0) {
            //format the final document to back to the client
            JsonObject container = new JsonObject()
            JsonObject version = new JsonObject ()
            version.put ("version", "1.0")
            if (options.includeVersion)
                container.put ("jsonapi", version)
            String type = pogo.getClass().simpleName
            def  id = pogo.hasProperty("id") ? (pogo as GroovyObject)?.getProperty("id").toString() : "unknown"
            def  altId = pogo.hasProperty("name") ? (pogo as GroovyObject)?.getProperty("name") : "unknown"
            if (id == "unknown && altid != unknown")
                id = altId
            container.put("type", type)
            container.put("id", id )
            if (jsonAttributes && jsonAttributes.size() != 0)
                container.put("attributes", jsonAttributes)
            if (options.optionalLinks) {
                JsonObject links = new JsonObject ()
                Closure linkNames = options.getLinkNamingStrategy()
                links.put ("self", linkNames ("self", pogo.getClass(), "", "$id"))
                container.put ("links", links)
            }
            if (jsonRelationships && jsonRelationships.size() != 0)
                container.put("relationships", jsonRelationships)
            if (options.compoundDocument) {
                 container.put ("included", compoundDocumentIncludedArray)
            }
            (jsonApiObject as JsonObject).put("data", container)

            classInstanceHasBeenEncodedOnce.clear()
        }
        jsonApiObject

    }

    @CompileStatic
    private def encodeSimpleType (value, style) {
        JsonObject json = new JsonObject()

        if (value instanceof Number)
            return value
        else if (value instanceof Boolean)
            return value
        else if (value instanceof byte[])
            return value
        else if (value instanceof CharSequence)
            return value as CharSequence
        else if (value instanceof Character)
            return value
        else if (value instanceof Enum)
            return value
        else if (value instanceof Date) { //date, Time, Timestamp
            json.put ("type", value.getClass().simpleName)
            json.put ("value", value.toString())
        }
        else if (value instanceof LocalDateTime || value instanceof LocalDate || value instanceof LocalTime) {
            json.put ("type", value.getClass().simpleName)
            json.put ("value", value.toString())
        }
        else if (value instanceof URL || value instanceof URI) {
            json.put ("type", value.getClass().simpleName)
            json.put ("value", value.toString())
        }
        else if (value instanceof UUID ) {
            json.put ("type", value.getClass().simpleName)
            json.put ("value", value.toString())
        } else {
            json.put("type", value.getClass().simpleName)
            json.put("unrecognised simple type", true)
            json.put("value", value.toString())
        }
        json
    }


    @CompileStatic
    private def encodeFieldType (Map.Entry prop, JsonEncodingStyle style = JsonEncodingStyle.softwood, JsonArray includedArray = null) {
        def json = new JsonObject()
        Closure converter

        if (prop.value == null) {
            if (options.excludeNulls == true)
                return
            else
                return json.putNull((String)prop.key)
        }
        else if (prop.value instanceof Class && options.excludedFieldTypes.contains(prop.value))
            return
        else if (options.excludedFieldNames.contains (prop.key) )
            return
        else if ( (converter = classImplementsConverterType (prop.value.getClass())) ) {
            converter.delegate = prop.value
            return converter(prop.value)
        }
        else if (prop.value.getClass() == Optional ) {
            def value
            def result
            if ((prop.value as Optional).isPresent()) {
                value = (prop.value as Optional).get()
                Closure valueConverter = options.converters.get (prop.value.getClass())
                if (valueConverter)
                    result = valueConverter (value)  //will break for unsupported types
                else
                    result = prop.value
            }
            return result
        }
        else if (prop.value.respondsTo("toSoftwoodJson")) {
            //type already has existing method to get JsonObject so use this
            println "prop '${prop?.key}', with type '${prop?.value?.getClass()}' supports toSoftwoodJson(), prop.value methods " + prop.value?.metaClass?.methods.collect {it.name}

            def retJson = prop.value.invokeMethod("toSoftwoodJson", null)
            return retJson
        }
        else if (prop.key == "class" && prop.value instanceof Class ) {
            def name
            if (!options.excludeClass) {
                name = (prop.value as Class).canonicalName
            }
            return name
        } else if (prop.value instanceof Enum ) {
            return prop.value.toString()
        }
        else {
            if (jsonEncodableStandardTypes.contains (prop.value.getClass())) {
                return prop.value
            } else {
                def jsonEncClass

                if (!options.summaryEnabled) {
                    switch (style) {
                        case JsonEncodingStyle.softwood :
                            //recursive call to expand on this object
                            if (iterLevel.get() <= options.expandLevels)
                                jsonEncClass = this?.toSoftwoodJson(prop.value)
                            else {
                                //println "iter level $iterLevel exeeded default $options.expandLevels, just provide summary encoding for object   "
                                JsonObject wrapper = new JsonObject()
                                if (classInstanceHasBeenEncodedOnce.get(prop.value)) {
                                    wrapper.put("entityType", prop.value.getClass().simpleName)
                                    wrapper.put ("isPreviouslyEncoded", true)
                                    if (prop?.value.hasProperty ("id")) {
                                        wrapper.put("id", (prop?.value as GroovyObject).getProperty("id").toString())
                                    }
                                    wrapper.put ("shortForm", prop.value.toString())

                                } else {
                                    //not seen before but too many levels in - just summarise
                                    wrapper.put("entityType", prop.value.getClass().canonicalName)
                                    wrapper.put("isSummarised", true)
                                    if (prop?.value.hasProperty("id"))
                                        wrapper.put("id", (prop?.value as GroovyObject).getProperty("id").toString())
                                    wrapper.put("shortForm", prop.value.toString())
                                }
                                return wrapper
                            }
                            break
                        case JsonEncodingStyle.jsonApi:
                            jsonEncClass = this?.toJsonApi(prop.value, includedArray)
                            break
                        case JsonEncodingStyle.tmf :
                            //recursive call to expand on this object
                            if (iterLevel.get() <= options.expandLevels)
                                jsonEncClass = this?.toTmfJson(prop.value)
                            else {
                                //check first if object has already been encoded, if so make declare it
                                JsonObject wrapper = new JsonObject()
                                if (classInstanceHasBeenEncodedOnce.get(prop.value)) {
                                    wrapper.put ("isPreviouslyEncoded", true)
                                    if (prop?.value.hasProperty ("id")) {
                                        wrapper.put("@type", prop.value.getClass().simpleName)
                                        wrapper.put("id", (prop?.value as GroovyObject).getProperty("id").toString())
                                    }
                                    wrapper.put ("shortForm", prop.value.toString())

                                } else {
                                    //else just report we are now summarising at this level and beyond
                                    wrapper.put("isSummarised", true)
                                    if (prop?.value.hasProperty("id")) {
                                        wrapper.put("@type", prop.value.getClass().simpleName)
                                        wrapper.put("id", (prop?.value as GroovyObject).getProperty("id").toString())
                                    }
                                     wrapper.put("shortForm", prop.value.toString())
                                }
                                return wrapper
                            }

                            break
                    }

                    if (jsonEncClass)
                        return jsonEncClass
                }else {
                    //if summaryEnabled just put the field and toString form
                    switch (style) {
                        case JsonEncodingStyle.softwood:
                            if (options.excludeClass == false) {
                                def wrapper = new JsonObject ()
                                wrapper.put("entityType", prop.value.getClass().canonicalName)
                                wrapper.put ("isSummarised" , true )
                                if (prop?.value.hasProperty ("id"))
                                    wrapper.put ("id", (prop?.value as GroovyObject).getProperty ("id").toString())

                                wrapper.put ("shortForm", prop.value.toString())
                                return wrapper
                            } else
                                return prop.value.toString()

                        case JsonEncodingStyle.jsonApi:
                            if (options.excludeClass == false) {
                                def wrapper = new JsonObject ()
                                wrapper.put("entityType", prop.value.getClass().canonicalName)
                                wrapper.put ("isSummarised" , true )
                                if (prop?.value.hasProperty ("id"))
                                    wrapper.put ("id", (prop?.value as GroovyObject).getProperty ("id").toString())

                                wrapper.put ("shortForm", prop.value.toString())
                                return wrapper
                            } else
                                return prop.value.toString()


                        case JsonEncodingStyle.tmf :
                            if (options.excludeClass == false) {
                                def wrapper = new JsonObject ()
                                wrapper.put ("isSummarised" , true )
                                if (prop?.value.hasProperty ("id")) {
                                    wrapper.put("@type", prop.value.getClass().canonicalName)
                                    wrapper.put("id", (prop?.value as GroovyObject).getProperty("id").toString())
                                }

                                wrapper.put ("shortForm", prop.value.toString())
                                return wrapper
                            } else
                                return prop.value.toString()


                    }

                }
            }
        }
    }


    //@CompileStatic
    private JsonArray encodeIterableType (Iterable iterable, JsonEncodingStyle style = JsonEncodingStyle.softwood, JsonArray includedArray=null) {
        JsonObject json = new JsonObject()
        JsonArray jList = new JsonArray ()


        /* List || instanceof Queue || Map.Entry from any entrySet iterator )*/
        if (Iterable.isAssignableFrom(iterable.getClass())) {
            //println "process an list/queue type"
            if (iterable.size() == 0) {
                return jList
            }

            iterable.each {
                if (jsonEncodableStandardTypes.contains (it.getClass())) {
                    jList.add (it)
                } else {
                    def jItem
                    switch (style) {
                        case JsonEncodingStyle.jsonApi :
                            def id, altId, type
                            def pogo
                            pogo = it
                            id = (pogo.hasProperty("id")) ? (pogo as GroovyObject).getProperty("id") : "<not defined>"
                            altId = (pogo.hasProperty("name")) ? (pogo as GroovyObject).getProperty("name") : "<not defined>"
                            if (id == "<not defined>" && altId != "<not defined>")
                                id = altId
                            type = it.getClass().simpleName

                            jItem = new JsonObject()
                            jItem.put ("type", type)
                            jItem.put ("id", id.toString())

                            if (options.compoundDocument) {
                                //encode each iterable object, which will add and compoundDoc 'included' entries
                                def  encodedClassInstance = toJsonApi (it, includedArray )
                            }

                            break
                        case JsonEncodingStyle.softwood :
                            jItem = this.toSoftwoodJson(it)
                            break
                        case JsonEncodingStyle.tmf :
                            jItem = this.toTmfJson(it)

                            break
                        default :
                            if (jItem)
                                jList.add(jItem)
                            break
                    }
                    if (jItem)
                        jList.add(jItem)
                }
            }

        }
        return jList

    }

    @CompileStatic
    private JsonObject encodeMapType (map, JsonEncodingStyle style = JsonEncodingStyle.softwood, JsonArray includedArray=null) {
        JsonArray mapEntries = new JsonArray ()
        JsonObject entry = new JsonObject ()
        JsonObject encMapEntries = new JsonObject ()
        boolean isSimpleKey = true  //default position, normally a string

        /* Map */
        if (map instanceof Map) {
            if (map.size() == 0) {
                return encMapEntries

            }

            map.each {Map.Entry it ->
                JsonObject json = new JsonObject()

                //the key in a map might be a complex object - if so we need an encoded summary for json entry
                def encodedKey
                if (isSimpleAttribute(it.key.getClass()))
                    encodedKey = it.key.toString() //just use basic type as key for json entry
                else {
                    //key is itself an an entity, so put key summary details of object details as key
                    def entityRef = it.key
                    isSimpleKey = false

                    JsonObject keyWrapper = new JsonObject ()

                    if (classInstanceHasBeenEncodedOnce [(entityRef)] ) {
                        switch (style) {
                            case JsonEncodingStyle.softwood:
                                //println "already encoded pogo $pogo so just put toString summary "
                                keyWrapper.put("entityType", entityRef?.getClass().canonicalName)
                                keyWrapper.put("isPreviouslyEncoded", true)
                                if (entityRef.hasProperty("id"))
                                    keyWrapper.put("id", (entityRef as GroovyObject).getProperty("id").toString())
                                keyWrapper.put("shortForm", entityRef.toString())
                                JsonObject mapEntityKey = new JsonObject ()
                                mapEntityKey.put ("entityData", keyWrapper)
                                encodedKey =  mapEntityKey
                                break

                            case JsonEncodingStyle.jsonApi:
                                //todo

                                break

                            case JsonEncodingStyle.tmf:
                                keyWrapper.put ("isPreviouslyEncoded", true)
                                if (entityRef.hasProperty ("id")) {
                                    keyWrapper.put("@type", entityRef.getClass().simpleName)
                                    keyWrapper.put("id", (entityRef as GroovyObject).getProperty("id").toString())
                                }
                                keyWrapper.put ("shortForm", entityRef.toString())
                                encodedKey = keyWrapper

                                break
                        }
                    } else {
                        switch (style) {
                            case JsonEncodingStyle.softwood:
                                //else just put summary of object as the key - dont fully encode here
                                keyWrapper.put("isEntityKey", true)
                                keyWrapper.put("entityType", entityRef?.getClass().canonicalName)
                                if (entityRef.hasProperty("id"))
                                    keyWrapper.put("id", (entityRef as GroovyObject).getProperty("id").toString())
                                if (entityRef.hasProperty("name"))
                                    keyWrapper.put("name", (entityRef as GroovyObject).getProperty("name").toString())
                                keyWrapper.put("shortForm", entityRef.toString())  //encode toString of entity as value
                                JsonObject mapEntityKey = new JsonObject()
                                mapEntityKey.put("entityData", keyWrapper)
                                encodedKey = mapEntityKey
                                break

                            case JsonEncodingStyle.jsonApi:
                                //todo
                                break

                            case JsonEncodingStyle.tmf:
                                //else just put summary of object as the key - dont fully encode here
                                keyWrapper.put("isSummarised", true)
                                keyWrapper.put("@type", entityRef?.getClass().canonicalName)
                                if (entityRef.hasProperty("id"))
                                    keyWrapper.put("id", (entityRef as GroovyObject).getProperty("id").toString())
                                if (entityRef.hasProperty("name"))
                                    keyWrapper.put("name", (entityRef as GroovyObject).getProperty("name").toString())
                                keyWrapper.put("shortForm", entityRef.toString())  //encode toString of entity as value
                                encodedKey = keyWrapper
                                break
                        }

                    }
                }


                if (jsonEncodableStandardTypes.contains (it.value.getClass())) {
                    //value is simple value, just write key and value
                    switch (style) {
                        case JsonEncodingStyle.softwood:
                            if (isSimpleKey) {
                                json.put("key", encodedKey as String)
                            } else {
                                json.put("key", encodedKey as JsonObject)
                            }
                            json.put("value", it.value)
                            mapEntries.add(json)
                            break

                        case JsonEncodingStyle.jsonApi:
                            break

                        case JsonEncodingStyle.tmf:
                            if (isSimpleKey) {
                                encMapEntries.put(encodedKey as String, it.value)
                            } else  {
                                //encMapEntries.put ((encodedKey as JsonObject).encode(), it.value)
                                //looks weird so just use toString as 'key' entry
                                encMapEntries.put (it.key.toString(), it.value )
                            }
                            break
                    }


                } else {
                    //else value is complex entity, so encode it and
                    def jItem
                    switch (style) {
                        case JsonEncodingStyle.softwood:
                            jItem = this.toSoftwoodJson(it.value)
                            if (jItem) {
                                json = new JsonObject ()
                                if (isSimpleKey)
                                    json.put ("key", encodedKey as String)
                                else
                                    json.put ("key", encodedKey as JsonObject)
                                json.put ("value", jItem )
                                mapEntries.add (json)
                            }

                            break
                        case JsonEncodingStyle.jsonApi:
                            def id, altId, type
                            def pogo

                            //in json api the entries in array get encoded as rows of 'data':
                            id = (it.value.hasProperty ("id")) ? (it.value as GroovyObject).getProperty("id") : "tba"
                            type = it.value.getClass().simpleName
                            jItem = new JsonObject()
                            jItem.put ("type", type)
                            jItem.put ("id", id)

                            //todo not guard enough? - breaks out to toJsonApi
                            if (options.compoundDocument) {
                                //double derefence to object if from map attribute
                                def encodedClassInstance = toJsonApi (it.value, includedArray )
                            }

                            break
                        case JsonEncodingStyle.tmf:
                            jItem = this.toTmfJson(it.value)
                            if (jItem) {
                                //just add each key, value to encodedMapEntries
                                encMapEntries.put (encodedKey.toString(), jItem)
                            }
                            break
                    }
                }
            }

            //completed iteration through map and building mapEntries, so now complete the json to return
            switch (style) {
                case JsonEncodingStyle.softwood:
                    //todo check how this looks its returning an array
                    encMapEntries.put("withMapEntries", mapEntries)
                    break
                case JsonEncodingStyle.jsonApi:
                    break
                case JsonEncodingStyle.tmf:
                    //was directly writing each kay/value pair to encmapEntries - just return it
                    break
                }

            return encMapEntries

        }
    }

    @CompileStatic
    private Closure classImplementsConverterType (Class<?> clazz ) {

        //eg. is Temporal assignable from LocalDateTime

        def entry = options.converters.find {Map.Entry rec ->
            Class key = rec.key
            key.isAssignableFrom(clazz)
        }
        entry?.value
    }

    @CompileStatic
    private boolean isSimpleAttribute (def item) {
        Class<?> clazz
        if (item instanceof Class)
            clazz = item
        else
            clazz = item.getClass()

        simpleAttributeTypes.find {(it as Class).isAssignableFrom (clazz)}
    }

    @CompileStatic
    private boolean isJsonStandardEncodableAttribute (def item) {

        Class<?> clazz
        if (item instanceof Class)
            clazz = item
        else
            clazz = item.getClass()

        jsonEncodableStandardTypes.find {
            boolean testRes = (it as Class).isAssignableFrom (clazz)
            testRes
        }
    }

    /*
     * checks if attribute field name is excluded names or types list
     */
    private boolean isInExcludesCategory (String field, Class<?> clazz) {
        boolean isExcludedName = options.excludedFieldNames.find {it == "${field}".toString()}
        if (isExcludedName)
            return true
        boolean isExcludedType = options.excludedFieldTypes.find {it == clazz}
        if (isExcludedType)
            return true
        else
            false

    }
}
