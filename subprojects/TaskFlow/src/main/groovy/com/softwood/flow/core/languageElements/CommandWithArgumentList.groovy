package com.softwood.flow.core.languageElements

import com.softwood.flow.core.flows.FlowContext

public class CommandWithArgumentList {
    @Delegate
    List list  = []
    FlowContext ctx
    String name = ""

    static CommandWithArgumentList newShellCommand (FlowContext ctx, String name,  List arglist=null) {
        CommandWithArgumentList argList = new CommandWithArgumentList(ctx:ctx, name:name)
        argList.addAll(arglist ?: [])

        if (argList.ctx.newInClosure != null) {
            argList.ctx.newInClosure << argList  //add to items generated within the running closure
        }

        argList
    }

    static CommandWithArgumentList newShellCommand (FlowContext ctx, String name, Object item) {

        CommandWithArgumentList argList = new CommandWithArgumentList(ctx: ctx, name: name)
        if (item != null)
            argList.add (item)

        if (argList.ctx.newInClosure != null) {
            argList.ctx.newInClosure << argList  //add to items generated within the running closure
        }

        argList
    }

    static CommandWithArgumentList newShellCommand (FlowContext ctx, String name, ArrayList alist) {
        newShellCommand (ctx, name, alist.toList())
    }
}
