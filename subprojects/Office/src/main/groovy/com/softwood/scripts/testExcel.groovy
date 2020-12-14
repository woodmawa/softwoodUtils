package com.softwood.scripts

import com.softwood.office.Excel

Excel excel = new Excel()

def (headers, rows) = excel.parse("D:\\Tableau play data\\nov sales data.xlsx")

println 'Headers'
println '------------------'
headers.each { header ->
    println header
}

println "\n"
println 'Rows'
println '------------------'
//rows.each { row ->
//    println excel.toXml(headers, row)
//}

println "[\n"
rows.each { row ->
    println excel.toJson(headers, row)
}
println "\n]"