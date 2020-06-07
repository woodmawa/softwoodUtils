package scripts

/** method gets wrapped up into a Script class - so we need to reference the func via 'this' instance of script
 *
 * 
 * @return
 */
String func () {
    "called func"
}

def  funcRef = this.&func
def  methodRef = this::func

println func()
println " use funcref () : " + funcRef()
println " use methodref () : " + funcRef()

