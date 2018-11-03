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
    List simpleList

    String toString() {
        "TestClass (id:$id)"
    }
}

TestClass tc = new TestClass(id: UuidUtil.timeBasedUuid, name:"myTestClass", fl:12.9, today:Date.newInstance(), ldt:LocalDateTime.now())
tc.simpleList = [1,2,3]

JsonObject enc = jsonGenerator.toSoftwoodJson(tc)
println enc.encodePrettily()

TestClass dec = jsonGenerator.toObject(TestClass, enc)
println "decoded json as object : " + dec.dump()
