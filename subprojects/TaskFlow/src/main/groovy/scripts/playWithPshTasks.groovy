package scripts

import com.softwood.flow.core.flows.FlowContext
import com.softwood.flow.core.nodes.CmdShellAction
import com.softwood.flow.core.nodes.FlowNodeStatus
import com.softwood.flow.core.nodes.PowerShellAction
import com.softwood.flow.core.nodes.TaskAction
import groovyx.gpars.dataflow.Promise

powerShell = PowerShellAction::newPowerShellAction

FlowContext freeStandingCtx = FlowContext.newFreeStandingContext()

PowerShellAction psh  = powerShell ('psh') {cmdWithArguments ('dir', 'w') }
psh.run ()
println psh.resultValue


