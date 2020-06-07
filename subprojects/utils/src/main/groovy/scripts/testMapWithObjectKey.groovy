package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils

import java.time.LocalDateTime

class Request {
    long id = 1
    String name = "myRequest"
    BillOfMaterials bom

    String toString() {
        "Request (name:$name} [id:$id]"
    }
}

class Site {
    String name
    long id

    String toString() {
        "Site (name:$name) [id:$id]"
    }
}

class Ci {
    String name

    String toString() {
        "Ci (name:$name)"
    }
}

class BillOfMaterials {
    Ci myCi
    Map basket = new HashMap<Site, List<Object>>()

}

Site site = new Site(name: "hsbc ho", id: 1)
Ci ci = new Ci(name: "router27")
BillOfMaterials bom = new BillOfMaterials()
bom.myCi = ci
bom.basket.put(site, ["hello", ci])  //one entry with site as key and a list

JsonUtils.Options options = new JsonUtils.Options()
options.registerTypeEncodingConverter(LocalDateTime) { it.toString() }
options.excludeFieldByNames("myCi")
options.excludeNulls(true)
//options.setExpandLevels(1)
options.setJsonEncodingStyle(JsonEncodingStyle.softwood)
options.setIncludeVersion(true)
options.summaryClassFormEnabled(false)

jsonGenerator = options.build()

println "encoded int as " + jsonGenerator.toSoftwoodJson(2).encodePrettily()
println "encoded list int as " + jsonGenerator.toSoftwoodJson([2]).encodePrettily()
println "encoded map as " + jsonGenerator.toSoftwoodJson([a: 2]).encodePrettily()

println jsonGenerator.toSoftwoodJson(bom).encodePrettily()
