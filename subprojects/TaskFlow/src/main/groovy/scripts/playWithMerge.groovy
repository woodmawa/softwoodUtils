package scripts

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.flows.Subflow
import com.softwood.flow.core.nodes.MergeAction


def subflow = Subflow::newSubflow


def ctx = FlowContext.newFreeStandingContext()  //FlowContext.newProcessContext()


def sf1 = subflow (ctx, "sf#1"){action (ctx, "#act1sf1"){println "sf1 $it"} }
def sf2 = subflow (ctx, "sf#2"){action (ctx, "#act1sf2"){println "sf2 $it"}  }
def sf3 = subflow (ctx, "sf#3"){action (ctx, "#act1sf3"){println "sf3 $it"}  }

sf1.run()
sf2.run()
sf3.run()



//mergeWith << sf1
//mergeWith << sf2

def merge = MergeAction.newMergeAction(ctx, 'myMerge') {mergeSubflows ->
    mergeSubflows.addAll ([sf1, sf2])

    subflow (ctx, "mergeSf#4") { action (ctx, "#act1sf1"){ArrayList mergedTasks, args -> println "mergesf4 $mergedTasks, $args"}  }

    //action (ctx, "#m-sf4-act1"){ArrayList mergedTasks, args -> println "merge-sf4 $mergedTasks, args:$args,  $delegate"}
    //action (ctx, "#m-sf4-act2"){name -> println "merge-sf4# (name:$name),  $delegate"}
}

merge.join('fred')

ctx.taskActions.collect{it.result}*.join()

