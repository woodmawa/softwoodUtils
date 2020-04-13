package scripts


import com.softwood.flow.core.nodes.CmdShellAction
import com.softwood.flow.core.nodes.FlowNodeStatus
import com.softwood.flow.core.nodes.PowerShellAction
import com.softwood.flow.core.nodes.TaskAction
import groovyx.gpars.dataflow.Promise

action = TaskAction::newAction
cmdShell = CmdShellAction::newCmdShellAction
powerShell = PowerShellAction::newPowerShellAction

PowerShellAction psh  = powerShell ('psh') {}
psh.run ('dir')
println psh.resultValue

CmdShellAction cmd  = cmdShell ('cmd') {}
//cmd.run('dir', '/w')
cmd.run('systeminfo')
println cmd.resultValue

TaskAction task =  action ('first action') {println "Hello william "; 1}
task.run ()

task.ctx.taskActions.collect{it.result}.toList()*.join()

assert task.resultValue == 1
assert task.status == FlowNodeStatus.completed


println "ran task with seq# " + task.sequence



def lastTask = task >> action ('task2') {ctx, Promise p ->  println " how are doing, had promise of $p.val" ; 2}


task.ctx.taskActions.collect{it.result}.toList()*.join()


assert lastTask.sequence == 4
assert lastTask.resultValue == 2