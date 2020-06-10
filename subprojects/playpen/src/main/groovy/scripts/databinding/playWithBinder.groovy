package scripts.databinding

import groovy.transform.ToString

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.Temporal
import java.util.stream.Collectors
import java.util.stream.Stream


DataBinder db = new DataBinder.Options().build()

String s = Integer.simpleName
final Map basicTypes = [
        (Byte.simpleName): Byte,
        (Short.simpleName): Short,
        (Long.simpleName): Long,
        (Integer.simpleName): Integer,
        (Float.simpleName): Float,
        (Double.simpleName): Double,
        (Character.simpleName): Character,
        (String.simpleName): String,
        //(GString.simpleName): GString,
        (BigInteger.simpleName): BigInteger,
        (BigDecimal.simpleName): BigDecimal
]

List<String> basicTypeNames = basicTypes.keySet().stream().collect(Collectors.toList())



final Map temporalTypes = [(Date.simpleName): Date,
                           (Calendar.simpleName): Calendar,
                           (LocalDateTime.simpleName): LocalDateTime,
                           (LocalDate.simpleName): LocalDate,
                           (Temporal.simpleName): Temporal
]

List<String> temporalTypeNames = temporalTypes.keySet().stream().collect(Collectors.toList())

List fieldBlackList = ['class', '$staticClassInfo', '__$stMC', '$callSiteArray']

class Base {
    long id
}

@ToString
class SomeClass extends Base {

    String someString
    private int  someInt
}

println basicTypeNames
println temporalTypeNames

SomeClass sc = new SomeClass (someString : "will", someInt:5)

//only shows public properities
Map props = sc.properties

List<PropertyValue> lpv = sc.getMetaPropertyValues()
println lpv.collect {[(it.name): it.type]}

List fields = lpv.stream().filter {!fieldBlackList.contains(it.name) }.map {it.name}.collect(Collectors.toList())

List<Field> fds = []
def clazz = sc.getClass()
while (clazz != null) {
    fds.addAll(clazz.declaredFields)
    clazz = clazz.superclass
}


List<Field> fields2 = fds.stream().filter {!fieldBlackList.contains(it.name) && !it.isSynthetic()}.map {Field field -> field}.collect(Collectors.toList())
println "is private ? : " + Modifier.isPublic(fields2[1].modifiers) + " is accessible ? :  " + fields2[1].isAccessible()

/** generaically if no public accessor - then
 * reflect the named field,
 * set the accessible flag
 * swicth on the field type, and update the field with the required value (ignore synthetics), and special fields, or final fields
 * reset the flag move on to next field
 */

if (sc.respondsTo('setSomeInt', Integer)) {
    println "set using public accessor " + sc.invokeMethod('setSomeInt', 20)
} else {
    println "have to use reflection to set value "
    try {
        Field f = sc.class.getDeclaredField('someInt')
        def accessible = f.isAccessible()
        f.setAccessible(true)
        println "try setting ${f.name} with value 30 "
       // def rec = f.get(sc)
        //rec = 30
        f.setInt(sc, 30)  //field instance relative to sc instance
        if (!accessible)
            f.setAccessible(false)

    } catch (Exception e) {
        println e.message +  e.printStackTrace()
    }
}
println "sc : $sc.someInt"

println "fields -> " + fields2
println "someclass props " + sc.properties.keySet()