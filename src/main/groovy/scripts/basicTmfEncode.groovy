package scripts

import com.softwood.utils.JsonUtils

import java.time.LocalDateTime

JsonUtils.Options options = new JsonUtils.Options()
options.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.setExpandLevels(2)
options.summaryClassFormEnabled(false)

jsonGenerator = options.build()


//println "encode int 2 : " + jsonGenerator.toTmfJson(2)
//println "encode list int [1,2] : " + jsonGenerator.toTmfJson([1,2])
//println "encode map [a:2, b:3] : " + jsonGenerator.toTmfJson([a:2, b:3])

class SimpleTmf {
    long id = 1
    String name = "simpleInst"
    Point locationRef
    List children = new ArrayList<TmfChild>()
    List simpleList = []
    Map simpleMap =[:]
    Map tmfMap = [:]
}

class Point {
    int x, y,z

    String toString() {
        "Point ($x,$y,$z)"
    }
}

class TmfChild {
    String name
    SimpleTmf parent

    String toString() {
        "TmfChild (name:$name)"
    }
}

/*SimpleTmf s2 = new SimpleTmf()
s2.tmfMap = [(s2):100]
println "encode simple s2 : " + jsonGenerator.toTmfJson(s2).encodePrettily()*/

SimpleTmf s1 = new SimpleTmf()
//s1.locationRef = new Point (x:1, y:1, z:1)
TmfChild c1 = new TmfChild(name:"Tobias", parent:s1)
TmfChild c2 = new TmfChild(name:"Dominic", parent:s1)
s1.children << c1
s1.children << c2
//s1.simpleList = [c1,c2]
//s1.tmfMap << [a:s1, b:c1]

/*println "encode simple s1 : " + jsonGenerator.toTmfJson(s1).encodePrettily()

def res  = jsonGenerator.toTmfJson([1,2,"hello"]).encodePrettily()
println "simple array : $res"

res  = jsonGenerator.toTmfJson([a:1,C:2,c:"hello"]).encodePrettily()
println "simple map : $res" */

res  = jsonGenerator.toTmfJson([a:c1/*, b:s1*/]).encodePrettily()
println "complex map : $res"