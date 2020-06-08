package scripts.databinding

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.inject.Inject
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

@Slf4j

class DataBinder {

        List simpleAttributeTypes = [Number, Integer, Short, Long, Float, Double, byte[], Byte, String, GString, Boolean, Instant, Character, CharSequence, Enum, UUID, URI, URL, Date, LocalDateTime, LocalDate, LocalTime, Temporal, BigDecimal, BigInteger]

        Map classForSimpleTypesLookup = ['Number'       : Number, 'Enum': Enum, 'Temporal': Temporal,
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

        List defaultGroovyClassFields = ['$staticClassInfo', '__$stMC', 'metaClass', '$callSiteArray']

        protected Options options

        Options getOptions() { options }

        private JsonUtils(Options options) {
            //add getAt function for array index notation on JsonObject
            this.options = options
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
                typeEncodingConverters.put(Date, { Date it -> it.toLocalDateTime().toString() })  //save in LDT format
                typeEncodingConverters.put(Calendar, { it.toString() })
                typeEncodingConverters.put(Temporal, { it.toString() })
                typeEncodingConverters.put(URI, { it.toString() })
                typeEncodingConverters.put(UUID, { it.toString() })

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


}