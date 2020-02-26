package com.softwood.rules.scripts

import com.softwood.rules.api.Action
import com.softwood.rules.core.BasicAction

Action action = new BasicAction (name:"act#1", description:"do something", action: {println "hi william"})

action.execute()

