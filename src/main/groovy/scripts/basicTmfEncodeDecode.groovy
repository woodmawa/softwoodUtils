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

class TmfTestClass {
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
        "TmfTestClass (id:$id)"
    }
}

class TmfSubClass {
    long id
    String name

    String toString () {
        "TmfSubClass (name:$name) [id:$id]"
    }

}

TmfSubClass sc1 = new TmfSubClass (id:1, name:"subclass 1")

TmfTestClass tc = new TmfTestClass(id: UuidUtil.timeBasedUuid, name:"myTestClass", fl:12.9, today:Date.newInstance(), ldt:LocalDateTime.now())
tc.simpleList = [LocalDateTime.now(),true, "test string", 3]
tc.simpleMap = [a:1, b:true]
tc.subClassList = [sc1]
tc.complexMap = ['a': sc1]


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
enc = jsonGenerator.toTmfJson(tc)
println "\nencoded test class as : " + enc.encodePrettily()

TestClass dec = jsonGenerator.toObject(TestClass, enc, JsonEncodingStyle.tmf)
println "decoded json as object : " + dec.dump()
