package scripts

class MapDelegate {

    Map m = [:]

    String name = "my object"

    def getProperty(String name) {

        def val

        if (this.hasProperty("$name")) {
            val = getMetaClass().getProperty(this, "$name")
            val
        } else {
            m.get(name, null)
        }
    }

    void setProperty(String name, value) {
        if (this.hasProperty("$name")) {
            getMetaClass().setProperty(this, "$name", value)
        } else {
            m[(name)] = value
        }
    }
}

MapDelegate md = new MapDelegate()

md.dependsOn = "someObject"

//println md.dependsOn

println "${md.name}"
println "${md.dependsOn}"