package scripts.databinding

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.Temporal
import java.util.stream.Collectors
import java.util.stream.Stream

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

class SomeClass extends Base {

    String someString
    protected int someInt
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


List fields2 = fds.stream().filter {!fieldBlackList.contains(it.name) && !it.isSynthetic()}.map {Field f -> [f.name,f.type,f.modifiers, f.isAccessible(), f.isSynthetic()]}.collect(Collectors.toList())
println "is private ? : " + Modifier.isPublic(fields2[1][2])

println "fields -> " + fields2
println "someclass props " + sc.properties.keySet()