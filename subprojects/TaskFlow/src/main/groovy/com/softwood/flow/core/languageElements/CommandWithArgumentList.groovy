package com.softwood.flow.core.languageElements

import com.softwood.flow.core.flows.FlowContext

public class CommandWithArgumentList {
    @Delegate
    List list  = []
    FlowContext ctx
    String name = ""

    static CommandWithArgumentList newShellCommand (FlowContext ctx, String name,  List arglist=null) {
        CommandWithArgumentList cmdWithArgList = new CommandWithArgumentList(ctx:ctx, name:name)
        arglist.each {
            cmdWithArgList.add ("/$it")  //add to items generated within the running closure
        }

        if (cmdWithArgList.ctx.newInClosure != null) {
            cmdWithArgList.ctx.newInClosure << cmdWithArgList  //add to items generated within the running closure
        }

        cmdWithArgList
    }

    static CommandWithArgumentList newShellCommand (FlowContext ctx, String name, Object item) {

        CommandWithArgumentList cmdWithArgList = new CommandWithArgumentList(ctx: ctx, name: name)
        if (item != null)
            cmdWithArgList.add ("/$item")

        if (cmdWithArgList.ctx.newInClosure != null) {
            cmdWithArgList.ctx.newInClosure << cmdWithArgList  //add to items generated within the running closure
        }

        cmdWithArgList
    }

    static CommandWithArgumentList newShellCommand (FlowContext ctx, String name, ArrayList alist) {
        newShellCommand (ctx, name, alist.toList())
    }
}
