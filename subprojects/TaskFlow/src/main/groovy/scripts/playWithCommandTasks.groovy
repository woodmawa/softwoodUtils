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

