package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils
import com.softwood.utils.UuidUtil
import io.vertx.core.json.JsonObject

import java.time.LocalDateTime

JsonUtils.Options options = new JsonUtils.Options()
options.registerTypeEncodingConverter(LocalDateTime) { it.toString() }
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.setExpandLevels(1)
options.setJsonEncodingStyle(JsonEncodingStyle.tmf)
options.setIncludeVersion(true)
options.summaryClassFormEnabled(true)


jsonSumGenerator = options.build()

class SumTmfTestClass {
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
        "SumTmfTestClass (id:$id)"
    }
}

class SumTmfSubClass {
    long id
    String name
    SumTmfSub2Class s2c

    String toString() {
        "SumTmfSubClass (name:$name) [id:$id]"
    }
}

class SumTmfSub2Class {
    long id
    String name

    String toString() {
        "SumTmfSub2Class (name:$name) [id:$id]"
    }
}

SumTmfSubClass sc1 = new SumTmfSubClass(id: 1, name: "sum-subclass 1")
SumTmfSubClass sc2 = new SumTmfSubClass(id: 2, name: "sum-subclass 2")
SumTmfSubClass sc3 = new SumTmfSubClass(id: 3, name: "sum-subclass 3")

SumTmfSub2Class ss2c = new SumTmfSub2Class(id: 10, name: "2nd level sum-subclass")

sc1.s2c = ss2c

arrayOfTmfSubClass = [sc1, sc2, sc3]

def sumJson = jsonSumGenerator.toTmfJson(arrayOfTmfSubClass)
println "sumJsonList of array of tmfSubClass"
println sumJson.encodePrettily()


System.exit(0)


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
