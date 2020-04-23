package scripts


class SomeClass {
    int num = 10
    String string = 'hello'

    //note this doesnt get called in groovy v3
    Object get (String name) {
        println "get $name called "

        def prop = properties.grep{it.name == name}
        prop
    }

    void set (String name, def value) {

    }

    Object getProperty (String name) {
        println "getProperty $name called "

        //have to use method to get properties else it recurrs
        ArrayList<Map.Entry> prop = getProperties().grep{it.key == name}

        Map.Entry match = prop?[0]
        def val = match.getValue ()
        val
    }
}



def a = new SomeClass ()

def num = a.'num'

Expando exp = new Expando()

exp.m = [a: {println "hello"},
b: {println "william"}]

exp.m.a()
exp.m.b()

class WillsExpando extends Expando {

    def getProperty (String name) {

        List<PropertyValue> metaProps = getMetaClass().getMetaPropertyValues()
        MetaProperty prop = getMetaClass().getMetaProperty(name)
        if (prop) {
            prop.getProperty(this)
        } else {
            //if not real property - get the expando property map and try and look in there using 'name' arg as the key
            def result

            Map m = getProperties ()
            m.get(name)
        }
    }

    void setProperty (String name, value ) {

        List<PropertyValue> metaProps = getMetaClass().getMetaPropertyValues()
        MetaProperty prop = getMetaClass().getMetaProperty(name)
        if (prop) {
            prop.setProperty(this, value)
        } else {
            //if not real property - get the expando property map and try and look in there using 'name' arg as the key
            def result

            Map m = getProperties ()
            m.put(name, value)
        }
    }
}

WillsExpando we = new WillsExpando()

we.newProp = "fred"

def n = we.name
def np = we.newProp
we.dyn = 'any'
def dyn = we.dyn


println "n = $n, and dyn = $dyn and np = $np"