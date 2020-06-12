package scripts

import databinding.DataBinder
import databinding.TargetClass
import groovy.transform.ToString


import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.Temporal
import java.util.stream.Collectors
import java.util.stream.Stream


boolean assignable1 = int.isAssignableFrom(Integer) //false
boolean assignable2 = Integer.isAssignableFrom(int) //false
boolean assignable3 = Number.isAssignableFrom(Integer)  //true - is a superclass of - no loss of precision
boolean assignable4 = Integer.isAssignableFrom(Number)  //false - is in fact a subclass of

boolean assignable5 = Long.isAssignableFrom(Integer)  //false - is in fact a subclass of


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

//DataBinder dbind = new DataBinder.Options().excludeFieldByNames('id').build()
DataBinder dbind = new DataBinder.Options().build()

List<Field> loa = dbind.getFilteredClassFields(TargetClass)

def res = dbind.rawTypeConverter (Integer, 3L)

TargetClass instance = dbind.bind (TargetClass, [someString:"hi", id:1, someInt:10, refClass:[refString:'my refString', refInt: 100]], [blacklist:['id'], whitelist:['someString']])
instance = dbind.bind (TargetClass, [someString:"hi", id:1, someInt:10, refClass:[refString:'my refString', refInt: 100]])
instance = dbind.bind (new TargetClass(), [someString:"hi", id:1, someInt:10, refClass:[refString:'my refString', refInt: 100]])


println basicTypeNames
println temporalTypeNames

TargetClass sc = new TargetClass (someString : "will", someInt:5)

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


