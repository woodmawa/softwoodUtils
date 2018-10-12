package scripts

import com.softwood.utils.JsonUtils

import java.time.LocalDateTime

class Site {
    String name
    long id

    String toString (){
        "Site (name:$name) [id:$id]"
    }
}

class Ci {
    String name

    String toString () {
        "Ci (name:$name)"
    }
}

class BillOfMaterials {
    Ci myCi
    Map basket = new HashMap<Site, List<Object>>()
    //Map mapField = [someThing: "fred", anotherThing: "joe"]
    //List list = ["a", "b", "c"]
}

Site site = new Site (name:"hsbc ho", id:1)
Ci ci = new Ci (name:"router27")
BillOfMaterials bom = new BillOfMaterials()
bom.myCi = ci
bom.basket.put (site, ["hello", ci])

JsonUtils.Options options = new JsonUtils.Options()
options.registerConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.summaryClassFormEnabled(false)

jsonGenerator = options.build()

println "encoded int as "+jsonGenerator.toJson (2).encodePrettily()
println "encoded list int as "+jsonGenerator.toJson ([2]).encodePrettily()
println "encoded map as "+jsonGenerator.toJson ([a:2]).encodePrettily()

println jsonGenerator.toJson(bom).encodePrettily()
