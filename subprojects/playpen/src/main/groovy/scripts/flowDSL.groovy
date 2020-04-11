package scripts

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Dataflows
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.dataflow.operator.DataflowProcessor
import groovyx.gpars.dataflow.stream.DataflowStream

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import static groovyx.gpars.dataflow.Dataflow.*

enum ActionFlowType {
    sequential, concurrent
}

class Flow {
    String name = ""
    Expando flowContext = new Expando()
    DataflowVariable result = new DataflowVariable()
    DataflowQueue queue = new DataflowQueue()
    //Dataflows df = new Dataflows()  - bit like an expando with variables df.x, df.y etc as df.z = df.x+ df.y
    AtomicInteger seqNum = new AtomicInteger(0)
    ConcurrentLinkedQueue<Promise> promises = new ConcurrentLinkedQueue<>()
    ConcurrentLinkedQueue<DataflowProcessor> operators = new ConcurrentLinkedQueue<>()

    def initialValue = 0

    def start () {
        println "starting flow : $name"
        queue << initialValue
        this
    }

    def stop () {

        promises*.join()
        result << queue.getVal()
        assert promises[-1].val == result.val
        println "stopping flow : $name, with result $result.val"
        promises.clear()
        operators*.terminateAfterNextRun()
        operators*.join()
        operators.clear()
        result.val

    }

    def action (String name = 'anonymous', ActionFlowType ft = scripts.ActionFlowType.sequential, Closure work) {
        //forces previous promise to complete
        //if (promises)
        //   promises[-1].join()
        Promise promise = task {

            def seq = seqNum.incrementAndGet()
            def val = queue.val

            def intermediateResult = work(val)
            queue << intermediateResult
            println "action ($name) #$seq: received $val and pushed $intermediateResult to queue}"
            intermediateResult
        }
        promises << promise
        if (ft == ActionFlowType.sequential)
            promise.join()
        this
    }

    def asyncAction (Closure work) {
       Promise promise = task {

            def seq = seqNum.incrementAndGet()
            def val = queue.val

            def intermediateResult = work(val)
            queue << intermediateResult
            println "asyncAction #$seq: received $val and pushed $intermediateResult to queue}"
           intermediateResult
        }
        promises << promise
        this
    }

    def split (splits) {

        def intermediateValue = queue.val

        def size = splits.size()

        def inp = new DataflowVariable() << intermediateValue

        /*DataflowStream lhs = new DataflowStream()
        lhs.wheneverBound {println "sent $it to lhs"}
        def rhs = new DataflowStream()
        rhs.wheneverBound {println "sent $it to rhs"}
*/

        DataflowQueue lhs = new DataflowQueue()
        lhs.wheneverBound {println "sent $it to lhs"}
        def rhs = new DataflowQueue()
        rhs.wheneverBound {println "sent $it to rhs"}

        DataflowProcessor op = operator(inputs: [inp], outputs: [lhs, rhs]) { val ->

            println "split started with $val"
            //lhs << val
            //rhs << val

            for (int i=0; i< splits.size(); i++) {
                outputs[i] << intermediateValue
                Closure clos = splits[i]
                clos (outputs[i])
            }
        }
        operators << op
        this
    }
}

def flow = new Flow (name:'myflow')

def ans = flow
        .start()
        /*.action {println "doing work ..., initial value $it"; it + 10}
        .asyncAction {println "do more work ..., initial value $it"; it + 20}
        .asyncAction {println "do more and more work ..., initial value $it"; it + 30}*/
        .action ('doStuff') {it + 10}
        .action ('do more stuff') {it + 20}
        .action {it - 5}
        /*.split ( [
                {DataflowQueue s = it;
                    def val = s.val
                    println "lhs got $it, with value $val" },
                {println "rhs got $it"}
        ])*/

        .stop()

println "result was : " + flow.result.get()

Binding binding = new Binding()

block = {nestedClosure ->
    def cloned = nestedClosure.clone()
    cloned.delegate = delegate
    scripts.flowDSL.enclosing = "block"

    println "within block"
    cloned()
}

/*
nestedBlock = {twiceNestedClosure ->
    assert twiceNestedClosure.delegate.enclosing == 'block', "Nested block must be within a block"
    def cloned = twiceNestedClosure.clone()
    cloned.delegate = delegate
    this.enclosing = "nestedBlock"

    println "within nested block"
    cloned()
}

StringBuilder output = new StringBuilder()

block {
    def bo = owner.toString()
    def bd = delegate.toString()

    output << "block owner: $bo, delegate: $bd"
    nestedBlock {
        def nbo = owner.toString()
        def nbd = delegate.toString()
        output << "\n\tnested block owner: $nbo, delegate: $nbd"

        //System.out.println "owner : $owner, delegate : $delegate"
        println "hi"
    }
}
println output.toString()
*/


//will fail
/*nestedBlock {
    block {

    }
}
*/


scriptInstance = this
context = new Expando()
context.name = "thisContext"
context.scriptInstance = this  //store on context
context.promises = new ConcurrentLinkedDeque<>()

subflow = SubFlow::newSubFlow
action = TaskAction::newAction  //generates a new action
asyncAction = TaskAction::newAsyncAction
split = SplitAction::newSplit  //generates a new split


enum NodeStatus {
    ready, running, deferred, completed, errors
}

abstract class  FlowNode {
    protected Closure action = {}
    //todo must fix this -
    protected static Expando ctx    //has to be static to get splits 'action's' to have a ctx
    protected FlowNode previousNode
    protected Closure cloned
    //protected async = false

    String name = "anon"
    NodeStatus status = NodeStatus.ready
    protected DataflowVariable result = new DataflowVariable()
    int taskDelay = 0

    void setTaskDelay (int delay) {
        taskDelay = delay
    }


    void setResult (value) {
        result << value
    }

    //todo version that takes timeout
    def  getResult () {
        result.getVal()
    }

    def then (FlowNode nextStep, Closure errhandler = null) {
        cloned  = nextStep.action.clone()
        nextStep.previousNode = this
        //action = cloned
        if (taskDelay == 0)
            status = NodeStatus.running
        else
            status = NodeStatus.deferred

        cloned.delegate = ctx.scriptInstance

        if (!nextStep.ctx)
            nextStep.ctx = this.ctx

        //check if any promises have completed and if so remove from list

        def promises = ctx.promises
        def boundPromises = promises.grep {it.isBound()}
        boundPromises.each {
            boolean yesNo = ctx.promises.remove (it)
            yesNo
        }


        println "fire <$name> action "
        nextStep


    }

    //syntactic sugar
    def rightShift (FlowNode nextStep,  Closure errhandler = null) {
        then (nextStep, errhandler)
    }
}


class TaskAction extends FlowNode {
    static newAction (name = null) {
        def action = new TaskAction(name: name ?: "anon")
        action
    }

    static newAction (name = null,  Closure clos) {
        def cloned = clos.clone()
        new TaskAction(name: name ?: "anon", action:cloned)
    }

    static newAction (name = null,  int delay, Closure clos) {
        def cloned = clos.clone()
        new TaskAction(name: name ?: "anon", action:cloned, taskDelay: delay)

    }

    /*static newAsyncAction (name = null,  Closure clos) {
        def cloned = clos.clone()
        new TaskAction(name: name ?: "anon", action:cloned, async:true)
    }*/



    def then (FlowNode nextStep, Closure errHandler = null) {
        super.then (nextStep, errHandler)
        try {
            if (nextStep.taskDelay) {
                Thread.sleep( nextStep.taskDelay, {println "thread interupted "})
                println "finished delay $nextStep.taskDelay ms"
            }

            //schedule task and receive the future and store it
            //pass promise from this into new closure in the task
            Promise promise  = task {
                def ans
                if (cloned.maximumNumberOfParameters == 2)
                    ans = cloned(ctx, this.result)
                else
                    ans = cloned(ctx)
                nextStep.status = NodeStatus.completed
                ans
            }
            ctx.promises << promise
            nextStep.result = promise


            nextStep
        } catch (Exception e) {
            if (errHandler) {
                nextStep.status = NodeStatus.errors
                errHandler(e, this)
            }
            nextStep
        }
    }

        String toString () {
        "Action (name:$name, action:${action.toString()}"
    }
}

class SplitAction extends FlowNode {
    static newSplit (name = null) {
        def action = new SplitAction(name: name ?: "anon")
        action
    }

    static newSplit (name = null,  Closure clos) {
        def cloned = clos.clone()
        println "new split created"
        new SplitAction(name: name ?: "anon", action:cloned)
    }

    def then (FlowNode nextStep, Closure errHandler = null) {
        super.then (nextStep, errHandler)
        try {
            if (cloned.maximumNumberOfParameters == 2)
                nextStep.setResult (cloned(ctx, result))
            else
                nextStep.setResult (cloned (ctx))       //binds the

            nextStep.status = NodeStatus.completed
            nextStep
        } catch (Exception e) {
            if (errHandler) {
                nextStep.status = NodeStatus.errors
                errHandler(e, this)
            }
            nextStep
        }
    }

    String toString () {
        "Split (name:$name, action:${action.toString()}"
    }
}

enum FlowStatus {
    ready, running, suspended, completed, forcedExit
}

abstract class AbstractFlow {
    String name

    Expando ctx
    ConcurrentLinkedDeque<Promise> promises = new ConcurrentLinkedDeque<>()
    FlowStatus status = FlowStatus.ready

}

class Cflow extends AbstractFlow {

    protected ConcurrentLinkedDeque subflows = new ConcurrentLinkedDeque<>()

    static Cflow newFlow (flowName = null)  {

        Cflow flow = new Cflow ()
        if (!flow.ctx)
            flow.ctx = new Expando ()
        flow
    }


    def rightShift (TaskAction firstAction ) {

        firstAction.name =  "initial action"
        println "cflow rightShift on first action $firstAction.name "

        Closure cloned  = firstAction.action.clone()

        cloned.delegate = ctx.scriptInstance
        firstAction.ctx = ctx
        firstAction.status = NodeStatus.running

         Promise promise =  task {def ans = cloned(ctx)
            firstAction.status = NodeStatus.completed
            ans
        }
        ctx.promises << promise
        promises << promise
        firstAction.@result = promise

        firstAction
    }

    def leftShift (SubFlow sflow) {
        subflows << sflow
        sflow.parent = this as AbstractFlow
        if (!sflow.ctx)
            sflow.ctx = this.ctx

        sflow
    }
}

class SubFlow extends AbstractFlow {

    protected AbstractFlow parent
    protected ConcurrentLinkedDeque<Promise> subFlowPromises = new ConcurrentLinkedDeque<>()


    static SubFlow newSubFlow (flowName = null, Closure clos)  {

        SubFlow sflow = new SubFlow (name: flowName)

         sflow

    }

    def rightShift (FlowNode firstStep ) {

        firstStep.name =  "subflow initial step"
        println "subflow rightShift on first node $firstStep.name "

        Closure cloned  = firstStep.action.clone()

        cloned.delegate = ctx
        firstStep.ctx = ctx
        firstStep.status = NodeStatus.running

        Promise promise =  task {def ans = cloned(ctx)
            firstStep.status = NodeStatus.completed
            ans
        }
        ctx.promises << promise
        promises << promise
        subFlowPromises << promise
        firstStep.@result = promise

        firstStep
    }

}



flow = new Cflow(name: 'myflow', ctx : context)

flow << subflow ('subflow') {println "new subflow "}

System.exit(1)

flow = new Cflow(ctx : context)

def result = flow >> action {println "first action, return 1 "; 1}
        >> action ('act#2', 1000) {println "second action, return 2 "; 2}
        >> split ('split') {
            println "doing the split "
            def outp = action ('split1') {'2.1'} >> action ('substep') {println "substep 2.1";'2.1.2'}
            action ('split2') {'2.2'}
            action ('split3') {'2.3'}
            outp
            }

println result.result

