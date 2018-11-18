package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils
import com.softwood.utils.UuidUtil
import io.vertx.core.json.JsonObject

import java.time.LocalDateTime

JsonUtils.Options options = new JsonUtils.Options()
options.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.setExpandLevels(1)
options.setJsonEncodingStyle(JsonEncodingStyle.softwood)
options.setIncludeVersion(true)
options.summaryClassFormEnabled(false)


jsonGenerator = options.build()

class TestClass {
    UUID id
    float fl
    String name
    Date today
    LocalDateTime ldt
    List simpleList  //works for List, Collection
    Map simpleMap
    List subClassList
    Map complexMap

    String toString() {
        "TestClass (id:$id)"
    }
}

class SubClass {
    long id
    String name

    SubSubClass ssc

    String toString () {
        "SubClass (name:$name) [id:$id]"
    }

}

class SubSubClass {
    long id
    String name

    String toString () {
        "SubSubClass (name:$name) [id:$id]"
    }

}
SubClass sc1 = new SubClass (id:1, name:"subclass 1")
SubSubClass ssc1 = new SubSubClass (id:10, name:"subSubClass 1")
sc1.ssc = ssc1


//TestClass tc = new TestClass(id: UuidUtil.timeBasedUuid, name:"myTestClass")
TestClass tc = new TestClass(id: UuidUtil.timeBasedUuid, name:"myTestClass", fl:12.9, today:Date.newInstance(), ldt:LocalDateTime.now())
tc.simpleList = [LocalDateTime.now(),true, "test string", 3]
tc.simpleMap = [a:1, b:true]
tc.subClassList = [sc1]
tc.complexMap = ['a': sc1]


JsonObject enc
def decoded
enc = jsonGenerator.toSoftwoodJson(Date.newInstance())
println "encoded Date as \n" + enc.encodePrettily()
decoded = jsonGenerator.toObject(Date, enc)
println "date decoded " + decoded + " with type " + decoded.getClass().simpleName + "\n"

enc = jsonGenerator.toSoftwoodJson(LocalDateTime.now())
println "encoded LDT as \n" + enc.encodePrettily()
decoded = jsonGenerator.toObject(LocalDateTime, enc)
println "LDT decoded " + decoded + " with type " + decoded.getClass().simpleName + "\n"


def lenc = jsonGenerator.toSoftwoodJson([1, true, sc1])
println "encoded List as : \n" + lenc.encodePrettily()
decoded = jsonGenerator.toObject(ArrayList, lenc)
println "List decoded " + decoded + " with type " + decoded.getClass().simpleName + "\n"


def menc = jsonGenerator.toSoftwoodJson([a:1, b:true, c:sc1])
println "encoded map as : \n" + menc.encodePrettily()
decoded = jsonGenerator.toObject(HashMap, menc)
println "Map decoded " + decoded + " with type " + decoded.getClass().simpleName + "\n"

println "----"
enc = jsonGenerator.toSoftwoodJson(tc)
println "encoded test class as : " + enc.encodePrettily()

TestClass dec = jsonGenerator.toObject(TestClass, enc)
println "decoded json as object : " + dec.dump()
