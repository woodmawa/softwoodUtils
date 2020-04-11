package scripts

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable

class Flow2 {
    DataflowVariable df = new DataflowVariable()
    DataflowQueue dfq = new DataflowQueue()
    def initialValue

    Flow2(init) {
        initialValue = init
    }

    def rightShift ( Closure work ) {

        def dfv = compose ({val ->
            println "work completed with $val";
            def dfv =  new DataflowVariable<>()
            dfv << val
            dfv
        }, work)
        //dfq << work(df)
        //def val = dfq.getVal()
        //new Flow2(new DataflowVariable() << val)
        new Flow2 (dfv)
    }

    def compose (Closure thisClos, Closure other) {
        Closure first = thisClos
        Closure second = other

        def intermediateResult = second ()
        def result = first (intermediateResult)

    }
}

def flow = new Flow2(0)

//flow >> {println "hi with $it"; 1} >> {println " there with $it"; 2}

def v = new DataflowVariable()
def comb =  {println "clos 'add one' with $it"; it + 1} >> {println " clos times 2 $it"; it * 2} >> {println " clos times 4 $it"; it * 4}
v << comb(1)

def res = v.val
println "result of combined closures $res"

def ans = new DataflowVariable()
v = new DataflowVariable()
v.then {println "times 2"; it*2} then {println "times 4"; it*4} then {println "deposit answer in ans"; ans <<it}
//v.whenBound {println "v bound with $it"}

v << 1 //when bound triggers the then closure
res = ans.val

println "result of dataflow then chaining $res"