package scripts


import com.softwood.flow.core.nodes.CmdShellAction
import com.softwood.flow.core.nodes.FlowNodeStatus
import com.softwood.flow.core.nodes.PowerShellAction
import com.softwood.flow.core.nodes.TaskAction
import groovyx.gpars.dataflow.Promise

action = TaskAction::newAction

TaskAction task =  action ('first action') {println "Hello william "; 1}
task.run ()

def locCtx = task.ctx
def tl = locCtx.taskActions

assert task.resultValue == 1
assert task.status == FlowNodeStatus.completed


println "declared  task with seq# " + task.sequence

TaskAction task2 =  action (locCtx, 'second action') {println "Hello william "; 2}
task2.dependsOn (task)

task2.run()

def lastTask = task2 >> action ('task3') {ctx, Promise p ->  println " how are doing, had promise of $p.val" ; 3}


task.ctx.taskActions.collect{it.result}.toList()*.join()


assert lastTask.sequence == 3
assert lastTask.resultValue == 3