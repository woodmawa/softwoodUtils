package scripts

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.nodes.CmdShellAction
import com.softwood.flow.core.nodes.FlowNodeStatus
import com.softwood.flow.core.nodes.PowerShellAction
import com.softwood.flow.core.nodes.TaskAction
import groovyx.gpars.dataflow.Promise

action = TaskAction::newAction
cmdShell = CmdShellAction::newCmdShellAction
powerShell = PowerShellAction::newPowerShellAction

/*PowerShellAction psh  = powerShell ('psh') {}
psh.run ('dir')
println psh.resultValue*/

FlowContext freeStandingCtx = FlowContext.newFreeStandingContext()

CmdShellAction cmd  = cmdShell (freeStandingCtx, 'cmd') {arg ->
    cmdWithArguments ('systeminfo') }
//cmd.run('dir', '/w')
cmd.run()
println cmd.resultValue

