package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils
import com.softwood.utils.UuidUtil
import io.vertx.core.json.JsonObject

import java.time.LocalDateTime

JsonUtils.Options options = new JsonUtils.Options()
options.registerConverter(LocalDateTime) {it.toString()}
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

    String toString () {
        "SubClass (name:$name) [id:$id]"
    }

}

SubClass sc1 = new SubClass (id:1, name:"subclass 1")

TestClass tc = new TestClass(id: UuidUtil.timeBasedUuid, name:"myTestClass", fl:12.9, today:Date.newInstance(), ldt:LocalDateTime.now())
tc.simpleList = [LocalDateTime.now(),true, "test string", 3]
tc.simpleMap = [a:1, b:true]
tc.subClassList = [sc1]
tc.complexMap = ['a': sc1]


JsonObject enc
/*enc = jsonGenerator.toSoftwoodJson(Date.newInstance())
println "encoded Date as \n" + enc.encodePrettily()

enc = jsonGenerator.toSoftwoodJson(LocalDateTime.now())
println "encoded LDT as \n" + enc.encodePrettily()*/

/*
def lenc = jsonGenerator.toSoftwoodJson([1, true, sc1])
println "encoded List as : " + lenc.encodePrettily()

def menc = jsonGenerator.toSoftwoodJson([a:1, b:true, c:sc1])
println "encoded map as : " + menc.encodePrettily()


 def lres = jsonGenerator.toObject(LinkedList, lenc)
println lres
//System.exit(0)

def mres = jsonGenerator.toObject(HashMap, menc)
println mres*/

//System.exit(0)

enc = jsonGenerator.toSoftwoodJson(tc)
println "encoded test class as : " + enc.encodePrettily()

TestClass dec = jsonGenerator.toObject(TestClass, enc)
println "decoded json as object : " + dec.dump()
