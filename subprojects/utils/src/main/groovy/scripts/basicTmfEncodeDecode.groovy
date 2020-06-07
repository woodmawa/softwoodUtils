package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils
import com.softwood.utils.UuidUtil
import groovy.transform.EqualsAndHashCode
import io.vertx.core.json.JsonObject

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue

JsonUtils.Options options = new JsonUtils.Options()
options.registerTypeEncodingConverter(LocalDateTime) { it.toString() }
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.setExpandLevels(1)
options.setJsonEncodingStyle(JsonEncodingStyle.softwood)
options.setIncludeVersion(true)
options.summaryClassFormEnabled(false)


jsonGenerator = options.build()

@EqualsAndHashCode
class TmfTestClass {
    UUID id
    float fl
    String name
    Date today
    LocalDateTime ldt
    List simpleList  //works for List, Collection
    TmfSubClass sc1
    TmfSub2Class sc2
    Map simpleMap
    List subClassList
    Map complexMap

    String toString() {
        "TmfTestClass (id:$id)"
    }
}

@EqualsAndHashCode
class TmfSubClass {
    long id
    String name
    TmfSub2Class s2c

    String toString() {
        "TmfSubClass (name:$name) [id:$id]"
    }
}

@EqualsAndHashCode
class TmfSub2Class {
    long id
    String name

    String toString() {
        "TmfSub2Class (name:$name) [id:$id]"
    }
}

TmfSubClass sc1 = new TmfSubClass(id: 1, name: "subclass 1")
TmfSub2Class s2c = new TmfSub2Class(id: 10, name: "2nd level subclass")
sc1.s2c = s2c

TmfTestClass tc = new TmfTestClass(id: UuidUtil.timeBasedUuid, name: "myTestClass", fl: 12.9, today: Date.newInstance(), ldt: LocalDateTime.now())
tc.simpleList = [LocalDateTime.now(), true, "test string", 3]
tc.simpleMap = [a: 1, b: true]
tc.subClassList = [sc1]
tc.complexMap = ['a': sc1]
tc.sc2 = s2c
tc.sc1 = sc1

JsonObject enc
/*enc = jsonGenerator.toTmfJson(Date.newInstance())
println "encoded Date as \n" + enc.encodePrettily()

enc = jsonGenerator.toTmfJson(LocalDateTime.now())
println "encoded LDT as \n" + enc.encodePrettily()*/


/*def lenc = jsonGenerator.toTmfJson([1, true, sc1])
println "encoded List as : " + lenc.encodePrettily()

def lres = jsonGenerator.toObject(LinkedList, lenc, JsonEncodingStyle.tmf)
println "basic decoded list is :" + lres
*/
/*
def menc = jsonGenerator.toTmfJson([a:1, b:true, c:sc1, d:sc1])
println "encoded map as : " + menc.encodePrettily()

def mres = jsonGenerator.toObject(HashMap, menc, JsonEncodingStyle.tmf)
println "basic decoded map is :" + mres

//System.exit(0)
*/

class SimpleClass {
    long id
    String name
}

SimpleClass simple = new SimpleClass(id: 1, name: 'fred')

enc = jsonGenerator.toTmfJson(simple)

//assert [a:1] == jsonGenerator.toObject ('{"a":1}', JsonEncodingStyle.tmf)
//assert ['a',1]  == jsonGenerator.toObject ('["a",1]', JsonEncodingStyle.tmf)


println "simple class " + enc
//def res = jsonGenerator.toObject (SimpleClass, enc, JsonEncodingStyle.tmf)
def res = jsonGenerator.toObject(enc.toString(), JsonEncodingStyle.tmf)

enc = jsonGenerator.toTmfJson(tc)
println "\nencoded test class as : " + enc.encodePrettily()

println "simple class " + enc
TmfTestClass dec = jsonGenerator.toObject(enc, JsonEncodingStyle.tmf)//jsonGenerator.toObject(TmfTestClass, enc, JsonEncodingStyle.tmf)

println "decoded json as object : " + dec.dump()

assert dec.id == tc.id
assert dec.sc2.id == tc.sc2.id
assert dec.sc2.name == tc.sc2.name
assert dec.sc2 == tc.sc2
assert dec.sc1 == tc.sc1
assert dec.complexMap.a == tc.complexMap.a
assert dec.complexMap == tc.complexMap
assert dec.subClassList[0] == tc.subClassList[0]
assert dec.subClassList == tc.subClassList
assert dec.simpleList == tc.simpleList
assert dec.ldt == tc.ldt    //works ok
assert dec.today == tc.today  //this Date type assertion fails !!
assert dec.fl == tc.fl
assert dec.name == tc.name
assert dec == tc  //hence so does this



