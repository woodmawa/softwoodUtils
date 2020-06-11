package scripts.databinding

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j

import javax.inject.Inject
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.stream.Collectors
import java.util.stream.Stream

@Slf4j
class DataBinder {

    final List simpleAttributeTypes = [Number, Integer, Short, Long, Float, Double, byte[], Byte, String, GString, Boolean, Instant, Character, CharSequence, Enum, UUID, URI, URL, Date, LocalDateTime, LocalDate, LocalTime, Temporal, BigDecimal, BigInteger]

    final Map classForSimpleTypesLookup = ['Number'       : Number, 'Enum': Enum, 'Temporal': Temporal,
                                     'Date'         : Date, 'Calendar': Calendar, 'Instant': Instant,
                                     'LocalDateTime': LocalDateTime, 'LocalDate': LocalDate, 'LocalTime': LocalTime,
                                     'UUID'         : UUID, 'URI': URI, 'URL': URL,
                                     'String'       : String, 'GString': GString,
                                     'byte[]'       : Byte[], 'Byte': Byte,
                                     'CharSequence' : CharSequence, 'Character': Character,
                                     'Boolean'      : Boolean, 'Integer': Integer, 'Long': Long,
                                     'Float'        : Float, 'Double': Double,
                                     'BigDecimal'   : BigDecimal, 'BigInteger': BigInteger]

    //todo note these may need to be thread local ??
    Map classInstanceHasBeenEncodedOnce = new ConcurrentHashMap()
    Queue previouslyDecodedClassInstance = new ConcurrentLinkedQueue()
    ThreadLocal<Integer> iterLevel = ThreadLocal.withInitial { 0 }

    final List defaultGroovyClassFields = ['$staticClassInfo', '__$stMC', 'metaClass', '$callSiteArray']

    protected Options options

    Options getOptions() { options }

    private DataBinder (Options options) {
        //add getAt function for array index notation on JsonObject
        this.options = options
        this
    }

    /**
     * inner class to set options in fluent api form and then build the
     * generator with the options provided
     */
    @CompileStatic
    class Options {

        ClassLoader defaultClassLoader = Thread.currentThread().getContextClassLoader()

        boolean includeVersion = false
        boolean compoundDocument = false
        boolean excludeNulls = true
        boolean excludeClass = true
        boolean excludePrivateFields = false
        boolean excludeStaticFields = true
        List excludedFieldNames = []
        List excludedFieldTypes = []
        int expandLevels = 1

        //encoders and decoders for 'normal types'
        Map typeEncodingConverters = new HashMap<Class, Closure>()
        Map typeDecodingConverters = new HashMap<Class, Closure>()

           Options() {
                //default type encoders to json text output
               /*
                typeEncodingConverters.put(Date, { Date it -> it.toLocalDateTime().toString() })  //save in LDT format
                typeEncodingConverters.put(Calendar, { it.toString() })
                typeEncodingConverters.put(Temporal, { it.toString() })
                typeEncodingConverters.put(URI, { it.toString() })
                typeEncodingConverters.put(UUID, { it.toString() })
                */

                //default type decoders from text to target type
                typeDecodingConverters.put(Date, { String it ->
                    //SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")  //looses precision
                    LocalDateTime ldt = LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    Date date = ldt.toDate()
                    date
                }
                )
                typeDecodingConverters.put(Calendar, { String it ->
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")
                    Date date = sdf.parse(it)
                    Calendar cal = Calendar.getInstance()
                    cal.setTime(date)
                    cal
                }
                )
                typeDecodingConverters.put(LocalDate, { String it ->
                    //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
                    LocalDate.parse(it)
                })
                typeDecodingConverters.put(LocalDateTime, { String it ->
                    //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
                    LocalDateTime.parse(it)
                })
                typeDecodingConverters.put(LocalTime, { String it ->
                    //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
                    LocalTime.parse(it)
                })
                typeDecodingConverters.put(URL, { String it -> new URL(it) })
                typeDecodingConverters.put(URI, { String it -> new URI(it) })
                typeDecodingConverters.put(UUID, { String it -> UUID.fromString(it) })

                this
            }

            Options setDefaultClassLoader(ClassLoader cl) {
                defaultClassLoader = cl
                this
            }

            Options setExpandLevels(Integer level) {
                expandLevels = level
                this
            }


            Options excludePrivateFields(boolean value = false) {
                excludePrivateFields = value
                this
            }

            Options excludeStaticFields(boolean value = true) {
                excludeStaticFields = value
                this
            }

            Options excludeNulls(boolean value = true) {
                excludeNulls = value
                this
            }

            Options excludeClass(boolean value = true) {
                excludeClass = value
                this
            }

            Options excludeFieldByNames(String name, args = null) {
                excludedFieldNames << name
                if (args) {
                    args.each { excludedFieldNames << it }
                }
                this
            }

            Options excludeFieldByNames(List<String> names) {
                excludedFieldNames.addAll(names)
                this
            }

            Options excludeFieldByTypes(Class clazz, args = null) {
                excludedFieldTypes << clazz
                if (args) {
                    args.each { excludedFieldTypes << it }
                }
                this
            }

            Options excludeFieldByTypes(List<Class> clazzes) {
                excludedFieldTypes.addAll(clazzes)
                this
            }

            Options registerTypeEncodingConverter(Class clazz, Closure converter) {
                typeEncodingConverters.put(clazz, converter)
                this
            }

            Options registerTypeDecodingConverter(Class clazz, Closure converter) {
                typeDecodingConverters.put(clazz, converter)
                this
            }

            DataBinder build() {
                def binder = DataBinder.newInstance(this)
                binder
            }
        }

    private Boolean isSimpleAttribute (Class clazz) {

        simpleAttributeTypes.contains(clazz)

    }

    @CompileStatic (TypeCheckingMode.SKIP)
     List getFilteredClassFields (Class clazz) {

        assert clazz

        List<Field> listOfAttributes =  []

        Class parent = clazz

        while (parent) {

            List listOfFields = Stream.of (parent.getDeclaredFields())
                    .filter {!defaultGroovyClassFields.contains(it)}
                    .filter {!Modifier.isSynthetic(it.modifiers)}   //Exceeds access rights!
                    .filter {!Modifier.isTransient(it.modifiers)}
                    .filter {!options.excludedFieldNames.contains (it.name)}
                    .filter {!options.excludedFieldTypes.contains (it.type)}
                    .collect (Collectors.toList())
            listOfAttributes.addAll(listOfFields)
            parent = parent.superclass
        }

         listOfAttributes

    }


    /* bind using class definition */
    def bind (Class<?> clazz, Map data) {
        bind (clazz, data, null, null)
    }

    def bind (Class<?> clazz, Map data, Map blackOrWhite) {

        assert blackOrWhite

        String[] blacklist = blackOrWhite.get('blacklist', '') as String[]
        String[] whitelist = blackOrWhite.get('whitelist', '') as String[]

        bind (clazz, data, blacklist, whitelist)
    }

    def bind (Class<?> clazz, Map data, String[] whitelist) {
        bind (clazz, data, null, whitelist)
    }

    /**
     *
     * @param Class clazz  to instantiate
     * @param Map data - initialising map of data
     * @param String[] blacklist  - list of attribute names to drop from any mapping
     * @param String[] whitelist - list of absolute attribute names to include in any target mapping
     * @return
     */
    def bind (Class<?> clazz, Map data, String[] blacklist, String[] whitelist) {
        assert clazz
        assert data

        List exclusions = (blacklist)? options.excludedFieldNames + blacklist : options.excludedFieldNames

        List<Field> loa = getFilteredClassFields (clazz)

        List<String> listOfAttributeNames = loa.stream().map{it.name}.collect(Collectors.toList())


        Map subData = data.subMap(listOfAttributeNames - exclusions)
        if (whitelist)
            subData = subData.subMap (whitelist)

        //invoke the map constructor
        def instance = clazz.newInstance(subData)

        instance
    }


    /*  new instance binding */
    def bind (Object instance, Map data) {
        bind (instance, data, null, null)
    }

    def bind (Object instance, Map data, Map blackOrWhite) {

        assert blackOrWhite

        String[] blacklist = blackOrWhite.get('blacklist', '') as String[]
        String[] whitelist = blackOrWhite.get('whitelist', '') as String[]

        bind (instance, data, blacklist, whitelist)
    }

    def bind (Object instance, Map data, String[] whitelist) {
        bind (instance, data, null, whitelist)
    }

    /**
     *
     * @param def instance  to instantiate
     * @param Map data - initialising map of data
     * @param String[] blacklist  - list of attribute names to drop from any mapping
     * @param String[] whitelist - list of absolute attribute names to include in any target mapping
     * @return
     */
    def bind (Object instance, Map data, String[] blacklist, String[] whitelist) {
        assert instance
        assert data

        List exclusions = (blacklist)? options.excludedFieldNames + blacklist : options.excludedFieldNames

        List<Field> loa = getFilteredClassFields (instance.getClass())

        List<String> listOfAttributeNames = loa.stream().map{it.name}.collect(Collectors.toList())

        Map subData = data.subMap(listOfAttributeNames - exclusions)
        if (whitelist)
            subData = subData.subMap (whitelist )

        //for each field try and update the instances field values
        subData.each {key, value ->
            Closure decoder = options.typeDecodingConverters.get(value.getClass())
            ArrayList<Field> fieldMatch = loa.grep { Field f -> f.name == key} as ArrayList<Field>
            Field field = fieldMatch?[0]
            if (field) {
                //try and use a setter if available
                String setter = 'set' +  key[0].capitalize() + key.substring(1)
                if (instance.respondsTo (setter, value.getClass())) {
                    instance.invokeMethod (setter, value)
                } else if  (decoder) {
                    def converted = decoder (value)
                    if (instance.respondsTo (setter, converted.getClass())) {
                        instance.invokeMethod(setter, converted)
                    }
                }
                else {
                    //use field to set value
                    boolean access = field.accessible
                    if (field.trySetAccessible()) {
                        if (decoder) {
                            field.set (instance, decoder(value))
                        } else {
                            field.set (instance, value)
                        }
                        field.set (instance, value)
                        field.setAccessible(access)
                    }
                }
            }
        }

        instance
    }
}