package scripts

import groovy.json.JsonOutput

println "encode int 2 : " + JsonOutput.toJson(2)
println "encode list int [1,2] : " + JsonOutput.toJson([1,2])
println "encode map [a:2, b:3] : " + JsonOutput.toJson([a:2, b:3])