package com.softwood.utils

import com.sun.xml.internal.fastinfoset.util.CharArray
import groovy.transform.CompileStatic
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

import javax.inject.Inject
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.security.InvalidParameterException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

    //todo note these may need to be thread local ??
    Map classInstanceHasBeenEncodedOnce = new ConcurrentHashMap()
    Queue previouslyDecodedClassInstance = new ConcurrentLinkedQueue()
    ThreadLocal<Integer> iterLevel = ThreadLocal.withInitial{0}

    List defaultGroovyClassFields = ['$staticClassInfo', '__$stMC', 'metaClass', '$callSiteArray']

    protected Options options
    private JsonUtils (Options options) {
        //add getAt function for array index notation on JsonObject
        JsonObject.metaClass.getAt = {String s -> delegate.getValue(s)}
        this.options = options
    }

    /**
     * inner class to set options in fluent api form and then build the
     * generator with the options provided
     */
    class Options {

        ClassLoader defaultClassLoader = Thread.currentThread().getContextClassLoader()

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

        //encoders and decoders for 'normal types'
        Map typeEncodingConverters = new HashMap<Class, Closure>()
        Map typeDecodingConverters = new HashMap<Class, Closure>()

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
            //default type encoders to json text output
            typeEncodingConverters.put(Date, {it.toString()})
            typeEncodingConverters.put(Calendar, {it.toString()})
            typeEncodingConverters.put(Temporal, {it.toString()})
            typeEncodingConverters.put(URI, {it.toString()})
            typeEncodingConverters.put(UUID, {it.toString()})

            //default type decoders from text to target type
            typeDecodingConverters.put(Date, {
                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")
                Date date = sdf.parse (it)
                date}
            )
            typeDecodingConverters.put(Calendar, {
                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")
                Date date = sdf.parse (it)
                Calendar cal = Calendar.getInstance()
                cal.setTime (date)
                cal}
            )
            typeDecodingConverters.put(LocalDate, {
                //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
                LocalDate.parse(it)
            })
            typeDecodingConverters.put(LocalDateTime, {
                //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
                LocalDateTime.parse(it)
            })
            typeDecodingConverters.put(LocalTime, {
                //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
                LocalTime.parse(it)
            })
            typeDecodingConverters.put(URL, {new URL(it)})
            typeDecodingConverters.put(URI, {new URI(it)})
            typeDecodingConverters.put(UUID, {UUID.fromString(it)})

            this
        }

        Options setDefaultClassLoader (ClassLoader cl) {
            defaultClassLoader = cl
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

        Options registerTypeEncodingConverter(Class clazz, Closure converter){
            typeEncodingConverters.put (clazz, converter)
            this
        }

        Options registerTypeDecodingConverter(Class clazz, Closure converter){
            typeDecodingConverters.put (clazz, converter)
            this
        }

        JsonUtils build () {
            def generator = JsonUtils.newInstance(this)
            generator
        }
    }

    /*
     * use options.defaultClassLoader to load class and return new instance
     * if throws an execption returns a proxy Expando class
     */
    private def getNewInstanceFromClass (clazz, args=null) {
        def clazzName, instance

        try {
            if (clazz instanceof Class) {
                clazzName = (clazz as Class).getCanonicalName()
 /*               def defaultConstructor
                Constructor<?>[] constructors = clazz.getConstructors()//clazz.getDeclaredConstructor()
                for (constructor in constructors ) {
                    if (constructor.parameterCount == 0)
                        defaultConstructor = constructor
                }
                if (defaultConstructor) {
                    instance = clazz.newInstance()  //try default constructor
                } else {
                    instance = clazz.newInstance()  //try default constructor any way
                }
*/
            }
            else
                clazzName = clazz.toString()

            if (!args)
                instance = options.defaultClassLoader.loadClass(clazzName, true).newInstance()
            else
                instance = options.defaultClassLoader.loadClass(clazzName, true).newInstance(args)
            instance
        } catch (Throwable t) {
            println "getInstanceFromClass : cant resolve class $clazzName, returning an Expando proxy instead"
            def proxy = options.defaultClassLoader.loadClass (Expando.canonicalName).newInstance() //new Expando ()
            proxy.isProxy = true
            proxy.proxiedClassName =  clazzName
            proxy

        }

    }

    /*
     * use options.defaultClassLoader to load class and return the class
     * can throw an exception - todo should i catch it ?
     */
    private Class<?> getClassForName (String clazzName) {
        def clazz
        try {
            clazz = options.defaultClassLoader.loadClass(clazzName, true)
        } catch (Throwable t) {
            println "getClassForName: can't load class $clazzName, message " + t.message
        }

    }

    //only want the real fields, and not getXXX property access methods
    @CompileStatic
    private Map getDeclaredFields (pogo) {
        Class clazz
        if (pogo == Class)
            clazz = pogo
        else
            clazz = pogo.getClass()

        List<Field> thisFields = []

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

    //lookup through hierarchy for field with right name and return it
    @CompileStatic
    private Field getField (pogo, String attName) {
        Class clazz
        if (pogo == Class)
            clazz = pogo
        else
            clazz = pogo.getClass()

        Field field
        while (clazz) {
            try {
                field = clazz.getDeclaredField(attName)

                if (field)
                    break
            } catch (NoSuchFieldException nsf) {
                clazz = clazz.getSuperclass()
                continue
            }
        }
        return field

    }


    /*
     * takes a JsonObject or JsonArray and tries to rebuild this as a graph of groovy objects.
     * where an json entry has been summarised then partially filled object (where an isSummarised variable
     * is set to true in the metaClass) or a proxy can be returned.
     * if the indicated type of the json object cant be found in the receiving VM then the code tries to
     * build a proxy using an Expando, where the proxiedClassName will hold the string name of the remote class
     * as encoded into the json
     */
    def toObject (Class<?> clazz, json, JsonEncodingStyle style = options.jsonStyle) {
        int level = iterLevel.get()
        def  instance

        iterLevel.set (++level)
        if (clazz.isInterface()) {
            if (List.isAssignableFrom(clazz)) {
                // is bit older, and ConcurrentLinkedQueue doesnt implement List as an interface use ArrayList
                instance = new ArrayList<>()
            } else if (Deque.isAssignableFrom(clazz)) {
                instance = new ConcurrentLinkedDeque<>() //can add/remove at front and back
            }else if (Collection.isAssignableFrom(clazz)) { //otherwise if just a collection, queue, etc then use
                instance = new ConcurrentLinkedQueue<>()
            } else if (Map.isAssignableFrom(clazz)) {
                instance = new ConcurrentHashMap<>()
            } else {
                println "must provide explicit class not an, interface : $clazz.simpleName"
                return
            }
        } else {
            if (clazz == LocalDateTime || clazz ==  LocalTime || clazz == LocalDate )
                instance = clazz.now()
            else if (Number.isAssignableFrom(json.getClass()))
                instance = getNewInstanceFromClass(clazz, json)
            else {
                instance = getNewInstanceFromClass(clazz)
            }
        }

        if (isSimpleAttribute(json)) {
            if (clazz == json.getClass())
                instance = json
            else {
                //instance = clazz.newInstance(json)
                instance = getNewInstanceFromClass(clazz, json)
            }
        } else if (json instanceof JsonArray) {
            // json passed represents an array of objects+JsonObjects
            switch (style) {
                case JsonEncodingStyle.softwood:  //shouldn't get this as jsonArrays encoded as value of a key:'iterable' json object
                    break
                case JsonEncodingStyle.tmf:
                    //def itemList = (json as JsonArray).asList()
                    for (item in (json as Iterable))
                        decodeCollectionAttribute(instance, item, style)
                    break

            }
        } else if (json instanceof JsonObject)  {
            switch (style) {
                case JsonEncodingStyle.softwood:
                    if (json['iterable']) {
                        //just sent a simple softwood Iterable (like a list)  to decode
                        def collectionAttributes = json['iterable']
                        for (jsonAtt in collectionAttributes)
                            decodeCollectionAttribute (instance, jsonAtt, style)
                    }
                    else if (json.getAt('map')?.getAt ('withMapEntries') ) {
                        //json is just a single jsonObject of map.entries
                        def mapAttributes = json['map']['withMapEntries']
                        for (jsonAtt in mapAttributes) {
                            def entry = decodeMapAttribute (instance, jsonAtt, style)
                            //instance.putAll (entry)

                        }
                    }
                    else if (json['type']) {
                        //just a basic java type has been encoded with key and value entries in JsonObject
                        def typeName = json['type']
                        Class clazzType = classForSimpleTypesLookup["$typeName"]
                        def typeValue = json['value']
                        def decoder = classImplementsDecoderType(clazzType)
                        def value = decoder (typeValue)
                        if (value)
                            instance = value
                    }
                    else if (json['entityData']){
                        //json is an entity jsonObject - so decode it

                        def entity = json['entityData']
                        def entityType = entity['entityType']
                        def entityId = entity['id']
                        //if id was encoded as jsonObject with type/value - use the value for the id
                        if (entityId instanceof JsonObject)
                            entityId = entity['id']['value']
                        def entityName = entity['name']
                        if (entity['isPreviouslyEncoded'] == null) {
                            //need to add the instance early in case child refers to parent
                            if (instance.respondsTo ('setId', entityId))
                                instance.setId (entityId)
                            else {
                                //if id field got encoded as jsonObject with type/value - use the value
                                Field idField = getField(instance, 'id')
                                if (idField)
                                    instance.id = decodeSimpleAttribute(idField.type, entityId, style)
                            }
                            if (instance.hasProperty('name'))
                                instance.setName (entityName)
                            //todo : what about is summarised??
                            previouslyDecodedClassInstance.add(instance)  //add to decoded entity list
                        }
                        if (entity['isPreviouslyEncoded']) {
                            //if previously encode - find decode version and use it
                            instance = findPreviouslyDecodedObjectMatch(entityType, entityId, entityName)
                        } else if (entity['isSummarised']) {
                            //just check if summarised object was previously decoded - in which case just use that
                            def previouslyDecoded = findPreviouslyDecodedObjectMatch(entityType, entityId, entityName)
                            if (previouslyDecoded)
                                instance = previouslyDecoded
                            else {
                                //all we have is a summarised encoded json object - so just build minimal object
                                // dont save as decoded instance
                                instance.metaClass.isSummarised = true
                                if (instance.hasProperty ('id')) {
                                    if (instance.respondsTo ('setId', entityId))
                                        instance.setId (entityId)
                                    else {
                                        Field idField = getField(instance, 'id')
                                        if (idField)
                                            instance.id = decodeSimpleAttribute(idField.type, entityId, style)
                                    }
                                }
                                if (instance.hasProperty('name'))
                                    instance.name = entity['name']
                            }
                        }else {
                            //else just decode the entity
                            def jsonAttributes = entity["attributes"]
                            for (jsonAtt in jsonAttributes) {
                                decodeFieldAttribute(instance, jsonAtt, style)
                            }
                            //process any collections attributes
                            def collectionAttributes = entity['collectionAttributes']
                            for (jsonAtt in collectionAttributes) {
                                decodeCollectionAttribute(instance, jsonAtt, style)
                            }

                            def mapAttributes = entity['mapAttributes']
                            for (jsonAtt in mapAttributes) {
                                decodeMapAttribute(instance, jsonAtt, style)

                            }

                        }
                    }
                    break

                case JsonEncodingStyle.tmf:
                    //found jsonObject - test to see if its encoded entity
                    if (json['@type']) {
                        //top level entity object to decode, decode its fields
                        def entity = json
                        if (entity['isPreviouslyEncoded']) {
                            instance = findPreviouslyDecodedObjectMatch(entity['@type'], entity['id'])
                        } else if (entity['isSummarised']) {
                            def previouslyDecoded  = findPreviouslyDecodedObjectMatch(entity['@type'], entity['id'])
                            if (previouslyDecoded)
                                instance = previouslyDecoded
                            else {
                                instance.metaClass.isSummarised = true  //mark as summarised on metaClass
                                if (entity['id']) {
                                    if (instance.respondsTo('setId', entity['id']))
                                        instance.setId(entity['id'])
                                    else {
                                        Field field = getField(instance, 'id')
                                        Closure decoder = classImplementsDecoderType(field.type)
                                        def decodedId = decoder(entity['id'])
                                        if (decodedId) {
                                            instance.setId(decodedId)
                                        }
                                    }
                                }
                                if (entity['name']) {
                                    String name = entity['name']
                                    if (instance.respondsTo('setName', name) ) {
                                        instance.name = entity['name']
                                    }
                                }
                                //Don't add partially completed instance to decoded object list
                            }
                        } else {
                            //build the instance from each of its fields
                            //instance not yet been encoded - so save a ref
                            for (fieldAttribute in json) {
                                decodeFieldAttribute(instance, fieldAttribute, style)
                            }
                            previouslyDecodedClassInstance.add (instance)

                        }

                    } else {
                        //json is not typed as an entity, it  must a jsonMap to decode
                        for (mapAttribute in json)
                            decodeMapAttribute(instance, mapAttribute, style)
                    }
                    break
            }
        }
        else  {
            iterLevel.set (--level)
            throw new InvalidParameterException (message: "parameter should be of type JsonObject or JsonArray, found ${json.getClass()}")
        }
        iterLevel.set (--level)
        if (iterLevel.get () == 0) {
            previouslyDecodedClassInstance.clear()
        }
        instance

    }


    private def decodeToProxyInstance (proxyClazzTypeName, jsonEntity, JsonEncodingStyle style) {
        //encoded iterable entity element doesn't exist in current vm - build a proxy and add that
        println "class $proxyClazzTypeName not in current vm - build an expando proxy instead"
        Expando proxy = new Expando()
        proxy.isProxy = true
        proxy.proxiedClassName =  proxyClazzTypeName

        switch (style) {
            case JsonEncodingStyle.softwood:
                if (jsonEntity['id'])
                    proxy.id = jsonEntity['id']
                if (jsonEntity['name'])
                    proxy.name = jsonEntity['name']
                def attributes = (jsonEntity['entityData']['attributes'] ?: [])
                for (jsonAtt in attributes) {
                    //only encode basic attributes - ignore lists and maps for proxy
                    def jsonValueObject = jsonAtt['value']['value']
                    if (isSimpleAttribute(jsonValueObject?.getClass()))
                        proxy."${jsonAtt['key']}" =  jsonValueObject
                    else if (jsonValueObject['entityData']) {
                        def entity = jsonValueObject['entityData']
                        def entityType = entity['entityType']
                        if (entityType) {
                            def decodedEntity = toObject(entityType, jsonEntity, style)
                            if (decodedEntity)
                                proxy."${jsonAtt['key']}" = decodedEntity
                            else
                                proxy."${jsonAtt['key']}" = "cant decode $entityType for proxy "
                        }
                    }
                }

            case JsonEncodingStyle.tmf:
                def attributes = (jsonEntity.asList() ?: [])  //includes id, name etc
                for (jsonAtt in attributes) {
                    if (isSimpleAttribute(jsonAtt['value']?.getClass()))
                        proxy."${jsonAtt['key']}" = jsonAtt['value']
                    else if (jsonAtt['value']['@type']) {
                        def decodedEntity  = decodeToProxyInstance (jsonAtt['value']['@type'], jsonAtt['value'], style)
                        if (decodedEntity)
                            proxy."${jsonAtt['key']}" =  decodedEntity
                    }
                }

                break
        }
        proxy.toString = {"Proxy'$proxiedClassName' (name:${this.name}) [id:${this.id}) "}
        proxy

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
                def attTypeName = jsonAtt['value']['type']
                def attType = classForSimpleTypesLookup[ attTypeName ]
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
                        Field field = getField(instance, attName)
                        if (field)
                            instance["$jsonAtt.key"] = decodeSimpleAttribute(field.type, attValue, style)
                    }
                } else {
                    //todo have to decode complex attribute
                    def entity = attValue['entityData']
                    def clazzName = entity['entityType']    //type toBuild
                    def entityId = entity['id']
                    def entityName = entity['name']
                    if (entity['isPreviouslyEncoded']) {
                        //find and use this
                        def decodedInstance = findPreviouslyDecodedObjectMatch(clazzName, entityId, entityName)
                        if (decodedInstance) {
                            //instance["$jsonAtt.key"] = decodedInstance
                            //call the setter
                            instance."set${camelCasedAttName}" ( decodedInstance )
                            instance
                        }

                    } else {
                        try {
                            Class clazz = getClassForName(clazzName)  //instance of subclass to build
                            def decodedInstance = toObject(clazz, attValue, style)
                            if (decodedInstance) {
                                instance."set${camelCasedAttName}" ( decodedInstance)
                                instance
                            }
                        } catch (Throwable t) {
                            println "exception in getClassForName([$clazzName]): " + t.message
                            def proxyInstance = decodeToProxyInstance(clazzName, attValue, style)
                            //todo - this will fail if variable is explicit type coded  - proxy is essentially an expando
                            if (proxyInstance){
                                instance."set${camelCasedAttName}" ( proxyInstance)
                                instance
                            }


                        }
                    }
                }
                break  //end softwood encoded field

            case JsonEncodingStyle.tmf:

                def attName = jsonAtt.getKey()
                if (attName == "@type")
                    return   //skip this attribute when decoding

                String camelCasedAttName = attName.substring (0,1).toUpperCase() + attName.substring (1)
                def attValue = jsonAtt.getValue()
                def simpleAttType = classForSimpleTypesLookup[ attValue.getClass().simpleName ]

                //if simple test for setter and use if present
                Field field = getField (instance, attName)
                if (simpleAttType != null && isSimpleAttribute(attValue)) {
                    boolean supports = instance.respondsTo("set$camelCasedAttName", attValue)

                    if (supports) {
                        instance["$jsonAtt.key"] = attValue
                    } else {
                        //try and find decoder for value to class
                        Closure decoder = classImplementsDecoderType(field.type)
                        if (decoder) {
                            def decodedJsonValueToType = decoder(attValue)
                            if (decodedJsonValueToType)
                                instance["$jsonAtt.key"] = decodedJsonValueToType
                        }
                    }
                } else {
                    //attribute is itself a complex object
                    def decodedJsonFieldAttribute = toObject(field.type, attValue, style)
                    if (decodedJsonFieldAttribute) {
                        instance["$attName"] = decodedJsonFieldAttribute
                    }


                }
                break //end tmf encoded field

        }

    }

    private def decodeCollectionAttribute (instance, collectionAtt, JsonEncodingStyle style) {
        switch (style ) {
            case JsonEncodingStyle.softwood:
                //starting instance is a collection class
                if (Collection.isAssignableFrom(instance.getClass()) ) {
                    if (isSimpleAttribute(collectionAtt)) {
                        //we are building a list of items, decode the collctionAtt and add to 'instance' collection
                        instance.add collectionAtt
                        return instance
                    } else if (collectionAtt['entityData']) {
                        //json attribute is an complex entity expression so decode and all to instance
                        String clazzName = collectionAtt['entityData']['entityType']
                        try {
                            Class clazz = getClassForName (clazzName)
                            def decodedEntity = toObject(clazz, collectionAtt, style)
                            instance.add (decodedEntity)
                        } catch (Throwable t) {
                            //class not in local vm - build using a proxy
                            def decodedProxy = decodeToProxyInstance(clazzName, collectionAtt, style)
                            instance.add (decodedProxy)
                        }
                        return instance

                    }

                }

                //otherwise starting instance as complex type, and we are decoding each field from the json
                String attName = collectionAtt['key']
                def iterableAttValue = collectionAtt['value']

                //use reflection to get field type
                Field field = instance.getClass().getDeclaredField ("$attName")
                switch (field.type) {
                    case ArrayList:
                    case List:
                        instance["$attName"] = new ArrayList()
                        break
                    case ConcurrentLinkedDeque:
                    case Deque:
                        instance["$attName"] = new ConcurrentLinkedDeque<>()
                        break
                    case Collection:
                    case Iterable:
                    case ConcurrentLinkedQueue:
                    case Queue:
                        instance["$attName"] = new ConcurrentLinkedQueue<>()
                        break
                    case HashSet:
                    case Set:
                        instance["$attName"] = new HashSet<>()
                        break

                }
                //def instCollAtt = instance["$attName"]
                for (item in iterableAttValue ) {
                    def clazz, value, proxy, iterableInstance, entity, previouslyDecodedEntity, entityId, entityName
                    boolean isEntity = false, isPreviouslyEncoded = false
                    String clazzName
                    if (item instanceof JsonObject) {
                        //either a map object - or could be an encoded jsonObject as map
                        if (item['type']) {
                            clazz = classForSimpleTypesLookup[ (item['type'])]
                            value = item['value']
                        } else if (item['entityData'] ) {
                            entity = item['entityData']
                            entityId = entity['id']
                            entityName = entity['name']
                            if (entity['isPreviouslyEncoded'])
                                isPreviouslyEncoded = true
                            isEntity = true
                            clazzName = entity['entityType']
                        }

                    } else {
                        clazz = item.getClass()
                        value = item
                    }
                    if (!isEntity && isSimpleAttribute(clazz))
                        instance["$attName"].add (decodeSimpleAttribute(clazz, value, style) )
                    else {
                        if (isPreviouslyEncoded) {
                            previouslyDecodedEntity = findPreviouslyDecodedObjectMatch(clazzName, entityId, entityName)
                            if (previouslyDecodedEntity) {
                                instance["$attName"].add(previouslyDecodedEntity)
                            }
                        } else {
                            try {
                                clazz = getClassForName(clazzName) //might get an expando
                                iterableInstance = toObject(clazz, item, style)
                                if (iterableInstance)
                                    instance["$attName"].add(iterableInstance)
                            } catch (RuntimeException ex) {
                                //encoded iterable entity element doesn't exist in current vm - build a proxy and add that
                                println "error in getClassFromName('$clazzName'] with error:  " + ex.message
                                def decodedProxy = decodeToProxyInstance(clazzName, item, style)
                                instance["$attName"].add(decodedProxy)

                            }
                        }
                    }
                }
                break
            case JsonEncodingStyle.tmf:
                if (Collection.isAssignableFrom(instance.getClass()) ) {
                    if (isSimpleAttribute(collectionAtt)) {
                        //we are building a list of items, decode the collectionAtt and add to 'instance' collection
                        instance.add collectionAtt
                        return instance
                    } else if (collectionAtt['type']) {
                        //found one of normal java types to encode
                        def attTypeName = collectionAtt['type']
                        def attValue = collectionAtt['value']
                        def simpleAttType = classForSimpleTypesLookup[ attTypeName ]
                        Closure decoder = classImplementsDecoderType(simpleAttType)
                        if (decoder) {
                            def decodedJsonValueToTypeInstance = decoder(attValue)
                            if (decodedJsonValueToTypeInstance)
                                instance.add (decodedJsonValueToTypeInstance)
                            else
                                instance.add ("error: cant decode $attTypeName with value $attValue")
                        }

                    } else if (collectionAtt['@type']) {
                        //json attribute is an complex entity expression so decode and object and add to instance
                        String clazzName = collectionAtt['@type']
                        def entityId = collectionAtt['id']
                        boolean isPreviouslyEncoded = collectionAtt['isPreviouslyEncoded']
                        if (isPreviouslyEncoded) {
                            previouslyDecodedEntity = previouslyDecodedClassInstance.find {
                                def runtimeClazzName = (it instanceof Expando && it?.isProxy) ? it.proxiedClassName : it.getClass().canonicalName
                                def test = runtimeClazzName == clazzName && it?.id?.toString() == entityId.toString()
                                test
                            }
                            if (previouslyDecodedEntity)
                                instance["$attName"].add (previouslyDecodedEntity)
                        } else {
                            try {
                                Class clazz = getClassForName(clazzName)
                                def entity
                                entity = toObject (clazz, collectionAtt, style)

                                 if (entity)
                                    instance.add(entity)
                            } catch (Throwable t) {
                                //class not in local vm - build using a proxy
                                def decodedProxy = decodeToProxyInstance(clazzName, collectionAtt, style)
                                if (decodedProxy) {
                                    previouslyDecodedClassInstance.add (decodedProxy) //add newly decoded entity

                                    instance.add(decodedProxy)
                                }
                            }
                        }
                        return instance

                    }

                }

                break

        }
        instance

    }

    //@CompileStatic
    private def decodeMapAttribute (instance, mapAtt, JsonEncodingStyle style) {
        switch (style ) {
            case JsonEncodingStyle.softwood:
                String attName = mapAtt['key']
                def attValue = mapAtt['value']

                //check in case we are just building basic map,not an entity
                if (Map.isAssignableFrom(instance.getClass()) ) {
                    if (isSimpleAttribute(attValue)) {
                        instance.put(attName, attValue)
                        return instance
                    } else {
                        //if we are building a new Map - check if attValue is an entity and  is in VM
                        //if so just decode - else return the built expando proxy
                        if (attValue.getClass() == JsonObject && attValue['entityData']) {
                            String clazzName = attValue['entityData']['entityType']
                            try {
                                Class clazz = getClassForName(clazzName)
                                if (clazz) {
                                    def decodedAttValue = toObject(clazz, attValue, style)
                                    instance.put(attName, decodedAttValue)
                                }
                            } catch (Throwable t) {
                                def decodedProxyAttValue = decodeToProxyInstance(clazzName, attValue, style)
                                instance.put(attName, decodedProxyAttValue)
                            }
                        }
                        return instance
                    }
                }

                // else we should have received an entity as instance and building json mapEntries into entity
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
                    def clazz, clazzName, value, key, mapInstance, proxy, entity, entityId, entityName, previouslyDecodedEntity
                    boolean isEntity = false, isPreviouslyEncoded = false

                    key = item['key']
                    if (isSimpleAttribute(item['value'].getClass())) {
                        value = item['value']
                        clazz = value.getClass()
                    } else if (item['value']['entityData'] ) {
                        entity = item['value']['entityData']
                        isEntity = true
                        clazzName = entity['entityType']
                        if (entity['isPreviouslyEncoded'])
                            isPreviouslyEncoded = true
                        entityId = entity['id']
                        entityName = entity['name']
                    }

                    if (!isEntity && isSimpleAttribute(clazz)) {
                        def decodedSimpleValue = decodeSimpleAttribute(clazz, value, style)
                        instance["$attName"].put(key, decodedSimpleValue )
                    }
                    else {
                        if (isPreviouslyEncoded) {
                            previouslyDecodedEntity = findPreviouslyDecodedObjectMatch(clazzName, entityId)
                            if (previouslyDecodedEntity) {
                                instance["$attName"].put(key, previouslyDecodedEntity )
                            }
                        } else {
                            try {
                                clazz = getClassForName(clazzName)
                                decodedMapValue = toObject(clazz, item, style)
                                instance["$attName"].put(key, decodedMapValue)
                            } catch (Throwable t) {
                                //encoded iterable entity element doesn't exist in current vm - build a proxy and add that
                                def decodedProxyMapValue = decodeToProxyInstance(clazzName, item, style)
                                instance["$attName"].put(key, decodedProxyMapValue)

                            }
                        }
                    }
                }
                break
            case JsonEncodingStyle.tmf:
                boolean isPreviouslyEncoded = false, isSummarised = false
                def entityId, entityName, previouslyDecodedEntity

                String attName = mapAtt['key']
                def attValue = mapAtt['value']

                if (isSimpleAttribute(attValue)) {
                    instance.put (attName, attValue)
                } else if (attValue['@type']) {
                    //value is itself a complex entity, decode entity value and store as value in map
                    def entity = attValue
                    def clazz
                    def clazzName = attValue['@type']
                    if (entity['isPreviouslyEncoded'])
                        isPreviouslyEncoded = true
                    if (entity['isSummarised'])
                        isSummarised = true
                    entityId = entity['id']
                    entityName = entity['name']
                    if (isSummarised) {
                        def summarisedProxyEntity = decodeToProxyInstance(clazzName, attValue, style)
                        if (summarisedProxyEntity)
                            instance.put(attName,summarisedProxyEntity)
                    }
                    else if (isPreviouslyEncoded) {
                        previouslyDecodedEntity = findPreviouslyDecodedObjectMatch(clazzName, entityId, entityName)
                        if (previouslyDecodedEntity) {
                            instance.put(attName, previouslyDecodedEntity )
                        }

                    } else {
                        //its entity in encoded json - try and decode it, else build a proxy
                        try {
                            clazz = getClassForName(clazzName)
                            //toObject will add new instance to decoded class list - dont do it twice here
                            def decodedEntity = toObject(clazz, attValue, style)
                            if (decodedEntity)
                                instance.put(attName, decodedEntity)
                        } catch (Throwable t) {
                            println "tried to decode object ${attValue.toString()} caught exception $t.message,  try and build a proxy "
                            def decodedProxyEntity = decodeToProxyInstance(clazzName, attValue, style)
                            if (decodedProxyEntity)
                                instance.put(attName, decodedProxyEntity)
                        }
                    }
                }
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

    /*
     * take a pogo and encode to softwood json format, defaults to one layer of expansion of an object graph
     */
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
        } else if (level > (options.expandLevels+1)) {
            println "toTmfJson : exceeded encode levels $level (>${options.expandLevels}), on pogo [$pogo] return null"
            json = null     //too many levels drop out with null json
        } else if (isSimpleAttribute(pogo.getClass())) {
            if ( named && isJsonStandardEncodableAttribute(pogo.getClass()) ) {
                (json as JsonObject).put("$named".toString(), pogo)
            } else {
                iterLevel.set (--level)
                return encodeSimpleType(pogo, JsonEncodingStyle.tmf)
            }
        } else if (Iterable.isAssignableFrom(pogo.getClass()) )
                 if (named) {
                     JsonArray encIterable = encodeIterableType(pogo as Iterable, JsonEncodingStyle.tmf)
                     (json as JsonObject).put("$named".toString(), encIterable)
                 }
                 else {
                     //if array just ignore any metadata if present and return the encoded array values
                     JsonArray encIterable = encodeIterableType(pogo as Iterable, JsonEncodingStyle.tmf)
                     json = encIterable

                 }
        else if (Map.isAssignableFrom(pogo.getClass()))
                 if (named) {
                     JsonObject encMap = encodeMapType(pogo as Map, JsonEncodingStyle.tmf)
                     (json as JsonObject).put("$named".toString(), encMap )
                 }
                 else {
                     //if map just ignore any metadata if present and return the encoded map values
                     JsonObject encMap = encodeMapType(pogo as Map, JsonEncodingStyle.tmf)
                     if (encMap)
                         json = encMap
                 }
        else {
            //json = new JsonObject()
            if (classInstanceHasBeenEncodedOnce[(pogo)]) {
                //println "already encoded pogo $pogo so just put toString summary and stop recursing"

                //JsonObject wrapper = new JsonObject()
                JsonObject jsonObj = new JsonObject()

                jsonObj.put ("isPreviouslyEncoded", true)
                jsonObj.put ("@type", pogo.getClass().canonicalName)
                if (pogo.hasProperty ("id")) {
                    def id = (pogo as GroovyObject).getProperty("id")
                    if (isSimpleAttribute(id.getClass()))
                        jsonObj.put("id", id)
                    else
                        jsonObj.put("id", id.toString().toString())
                }
                if (pogo.hasProperty ("name")) {
                    def name = (pogo as GroovyObject).getProperty("name")
                    jsonObj.put("name", name)
                }
                jsonObj.put ("shortForm", pogo.toString())
                //wrapper.put ("entity", jsonObj)
                iterLevel.set (--level)
                return jsonObj // pogo.toString()
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
                        jsonFields.put (prop.key.toString(), field )
                   } else {
                        if (!options.excludeNulls)
                            jsonFields.putNull(prop.key.toString())

                    }
                }
                (json as JsonObject).mergeIn (jsonFields as JsonObject, true)
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
        } else if (level > (options.expandLevels+1)) {
            println "toSoftwoodJson : exceeded decode levels $level (>${options.expandLevels}), on pogo [$pogo] return null"
            json = null     //too many levels drop out with null json
        }
        else if (Iterable.isAssignableFrom(pogo.getClass()) )
            if (named) {
                (json as JsonObject).put ("$named".toString(),  encodeIterableType(pogo as Iterable))
            } else
                (json as JsonObject).put ("iterable",  encodeIterableType(pogo as Iterable))
        else if (Map.isAssignableFrom(pogo.getClass()))
            if (named)
                (json as JsonObject).put ("$named".toString(),  encodeMapType(pogo as Map))
            else
                (json as JsonObject).put ("map",  encodeMapType(pogo ))
        else if ( isSimpleAttribute(pogo.getClass()) ){
            if ( named && isJsonStandardEncodableAttribute(pogo.getClass()) ) {
                (json as JsonObject).put("$named", pogo)
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
                if (pogo.hasProperty ("id")) {
                    def id = (pogo as GroovyObject).getProperty("id")
                    if (isSimpleAttribute(id.getClass()))
                        jsonObj.put("id", id)
                    else
                        jsonObj.put("id", id.toString().toString())
                }
                if (pogo.hasProperty ("name")) {
                    def name = (pogo as GroovyObject).getProperty("name")
                    jsonObj.put("name", name)
                }
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

            def jsonFields = new JsonObject()
            def jsonAttributes = new JsonObject()
            def jsonEntityReferences = new JsonObject()
            def jsonCollections = new JsonObject()
            def jsonMaps = new JsonObject()

            for (prop in nonIterableFields) {

                def encodedField = encodeFieldType(prop, JsonEncodingStyle.softwood)
                if (isSimpleAttribute(encodedField)) {
                    //simple things just generate the type and value
                    def wrapper = new JsonObject()
                    wrapper.put ('type', prop['value'].getClass().simpleName)
                    wrapper.put ('value', encodedField)
                    jsonAttributes.put(prop.key.toString(), wrapper)

                } else {
                    def wrapper = new JsonObject()
                    def entity = (encodedField as JsonObject).getValue('entityData')
                    if (entity) {
                        def entityType = (entity as JsonObject).getValue ('entityType')
                        //complex entity - generate full entity value returned from encodeField type
                        wrapper.put ('type', entityType ?: "null")
                        wrapper.put ('value', encodedField)
                        jsonAttributes.put (prop.key.toString(), wrapper )
                    }

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


            def id = (pogo.hasProperty("id")) ? (pogo as GroovyObject).getProperty("id") : "<not defined>"
            def name = (pogo.hasProperty("name")) ? (pogo as GroovyObject).getProperty("name") : "<not defined>"
            def type = pogo.getClass().canonicalName
            jsonFields.put ("entityType", type)
            if (!isSimpleAttribute(pogo.getClass())) {
                if (id != "<not defined>")
                    jsonFields.put("id", encodeSimpleType(id, JsonEncodingStyle.softwood))
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

            (json as JsonObject).put ("entityData", jsonFields)
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
                        def type = prop.value.getClass().canonicalName
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
            String type = pogo.getClass().canonicalName
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
        else if ( (converter = classImplementsEncoderType (prop.value.getClass())) ) {
            converter.delegate = prop.value
            return converter(prop.value)
        }
        else if (prop.value.getClass() == Optional ) {
            def value
            def result
            if ((prop.value as Optional).isPresent()) {
                value = (prop.value as Optional).get()
                Closure valueConverter = options.typeEncodingConverters.get (prop.value.getClass())
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
                                    JsonObject previouslyEncodedObject  = new JsonObject()
                                    wrapper.put("entityType", prop.value.getClass().canonicalName)
                                    wrapper.put ("isPreviouslyEncoded", true)
                                    if (prop?.value.hasProperty ("id")) {
                                        def id = (prop.value as GroovyObject).getProperty("id")
                                        if (isSimpleAttribute(id.getClass()))
                                            wrapper.put("id", id)
                                        else
                                            wrapper.put("id", id.toString().toString())
                                    }
                                    if (prop?.value?.hasProperty ("name")) {
                                        def name = (prop.value as GroovyObject).getProperty("name")
                                        wrapper.put("name", name)
                                    }

                                    wrapper.put ("shortForm", prop.value.toString())
                                    previouslyEncodedObject.put ('entityData', wrapper)
                                    return previouslyEncodedObject

                                } else {
                                    //not seen before but too many levels in - just summarise
                                    JsonObject summarisedObject = new JsonObject()
                                    wrapper.put("entityType", prop.value.getClass().canonicalName)
                                    wrapper.put("isSummarised", true)
                                    if (prop?.value.hasProperty ("id")) {
                                        def id = (prop.value as GroovyObject).getProperty("id")
                                        if (isSimpleAttribute(id.getClass()))
                                            wrapper.put("id", encodeSimpleType(id, style))
                                        else
                                            wrapper.put("id", id.toString().toString())

                                    }
                                    if (prop?.value.hasProperty ("name")) {
                                        def name = (prop.value as GroovyObject).getProperty("name")
                                        wrapper.put("name", name)
                                    }

                                    wrapper.put("shortForm", prop.value.toString())
                                    summarisedObject.put ('entityData', wrapper)
                                    return summarisedObject
                                }
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
                                    wrapper.put("@type", prop.value.getClass().canonicalName)
                                    if (prop?.value.hasProperty ("id")) {
                                        def id = (prop.value as GroovyObject).getProperty("id")
                                        if (isSimpleAttribute(id.getClass()))
                                            wrapper.put("id", id)
                                        else
                                            wrapper.put("id", id.toString().toString())

                                    }
                                    if (prop?.value.hasProperty ("name")) {
                                        def name = (prop.value as GroovyObject).getProperty("name")
                                        wrapper.put("name", name)
                                    }
                                    wrapper.put ("shortForm", prop.value.toString())

                                } else {
                                    //else just report we are now summarising at this level and beyond
                                    wrapper.put("isSummarised", true)
                                    wrapper.put("@type", prop.value.getClass().canonicalName)
                                    if (prop?.value.hasProperty ("id")) {
                                        def id = (prop.value as GroovyObject).getProperty("id")
                                        if (isSimpleAttribute(id.getClass()))
                                            wrapper.put("id", id)
                                        else
                                            wrapper.put("id", id.toString().toString())

                                    }
                                    if (prop?.value.hasProperty ("name")) {
                                        def name = (prop.value as GroovyObject).getProperty("name")
                                        wrapper.put("name", name)
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
                                JsonObject previouslyEncodedObject  = new JsonObject()
                                def wrapper = new JsonObject ()
                                wrapper.put("entityType", prop.value.getClass().canonicalName)
                                wrapper.put ("isSummarised" , true )
                                if (prop?.value.hasProperty ("id")) {
                                    def id = (prop.value as GroovyObject).getProperty("id")
                                    if (isSimpleAttribute(id.getClass()))
                                        wrapper.put("id", id)
                                    else
                                        wrapper.put("id", id.toString().toString())

                                }
                                if (prop.value.hasProperty ("name")) {
                                    def name = (prop.value as GroovyObject).getProperty("name")
                                    wrapper.put("name", name)
                                }

                                wrapper.put ("shortForm", prop.value.toString())
                                previouslyEncodedObject.put ('entityData', wrapper )
                                return previouslyEncodedObject
                            } else
                                return prop.value.toString()

                        case JsonEncodingStyle.jsonApi:
                            if (options.excludeClass == false) {
                                def wrapper = new JsonObject ()
                                wrapper.put("entityType", prop.value.getClass().canonicalName)
                                wrapper.put ("isSummarised" , true )
                                if (prop?.value.hasProperty ("id")) {
                                    def id = (prop.value as GroovyObject).getProperty("id")
                                    if (isSimpleAttribute(id.getClass()))
                                        wrapper.put("id", id)
                                    else
                                        wrapper.put("id", id.toString().toString())

                                }
                                if (prop.value.hasProperty ("name")) {
                                    def name = (prop.value as GroovyObject).getProperty("name")
                                    wrapper.put("name", name)
                                }

                                wrapper.put ("shortForm", prop.value.toString())
                                return wrapper
                            } else
                                return prop.value.toString()


                        case JsonEncodingStyle.tmf :
                            if (options.excludeClass == false) {
                                def wrapper = new JsonObject ()
                                wrapper.put ("isSummarised" , true )
                                wrapper.put("@type", prop.value.getClass().canonicalName)
                                if (prop?.value.hasProperty ("id")) {
                                    def id = (prop.value as GroovyObject).getProperty("id")
                                    if (isSimpleAttribute(id.getClass()))
                                        wrapper.put("id", id)
                                    else
                                        wrapper.put("id", id.toString().toString())

                                }
                                if (prop.value.hasProperty ("name")) {
                                    def name = (prop.value as GroovyObject).getProperty("name")
                                    wrapper.put("name", name)
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
                            type = it.getClass().canonicalName

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
                                    keyWrapper.put("@type", entityRef.getClass().canonicalName)
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
                            type = it.value.getClass().canonicalName
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

    /*
     * looks for first match in previouslyDecodeClassInstance List, uses class name as string, and entity id
     * as match fields.  in case of proxy looks matches on proxiedClassName and id
     */
    private def findPreviouslyDecodedObjectMatch (String clazzName,  entityId, String entityName = null) {

        //first check for concrete classes and check if they have been decoded
        def previouslyDecodedEntity = previouslyDecodedClassInstance.find {
            def test
            if (entityId == null && entityName != null ) {
                //weak test, type and name only
                test = (it.getClass().canonicalName == clazzName && it?.name?.toString() == entityName.toString())
            }
            else {
                //strong test using id
                test = (it.getClass().canonicalName == clazzName && it?.id?.toString() == entityId.toString())
                //test = (it.getClass().canonicalName && it?.id?.toString() == entityId.toString())
            }
            test
        }
        if (previouslyDecodedEntity == null) {
            //search for proxies that match the class and id and return that instead
            def previouslyDecodedProxyEntity = previouslyDecodedClassInstance.find {
                def test = (it.getClass() == Expando && it.isProxy && it?.proxiedClassName == clazzName && it?.id?.toString() == entityId.toString())
                test
            }
            if (previouslyDecodedProxyEntity)
                previouslyDecodedEntity =  previouslyDecodedProxyEntity
        }
        previouslyDecodedEntity
    }

    @CompileStatic
    private Closure classImplementsEncoderType (Class<?> clazz ) {

        //eg. is Temporal assignable from LocalDateTime

        def entry = options.typeEncodingConverters.find { Map.Entry rec ->
            Class key = rec.key
            key.isAssignableFrom(clazz)
        }
        entry?.value
    }

    @CompileStatic
    private Closure classImplementsDecoderType (Class<?> clazz ) {

        if (clazz == null)
            return {}

        //eg. is Temporal assignable from LocalDateTime
        //otherwise try and find an match
        def entry = options.typeDecodingConverters.find { Map.Entry rec ->
            Class key = rec.key
            key.isAssignableFrom(clazz)
        }
        Closure decoder = entry?.value as Closure ?: {}

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
