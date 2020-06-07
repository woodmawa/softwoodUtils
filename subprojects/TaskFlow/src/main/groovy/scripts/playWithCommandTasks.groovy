package scripts

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.nodes.CmdShellAction
import com.softwood.flow.core.nodes.FlowNodeStatus
import com.softwood.flow.core.nodes.PowerShellAction
import com.softwood.flow.core.nodes.TaskAction
import groovyx.gpars.dataflow.Promise

cmdShell = CmdShellAction::newCmdShellAction

/*PowerShellAction psh  = powerShell ('psh') {}
psh.run ('dir')
println psh.resultValue*/

FlowContext freeStandingCtx = FlowContext.newFreeStandingContext()

CmdShellAction cmd = cmdShell(freeStandingCtx, 'cmd') { arg ->
    cmdWithArguments('dir', '/w')
}
//cmdWithArguments ('systeminfo') }

cmd.run()
println cmd.resultValue

