package scripts.tablesaw

import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.selection.Selection

double[] numbers = [1,2,3,4]

DoubleColumn nc = DoubleColumn.create ("nc", numbers)

println nc.print()
println " -> index 3 : " + nc.get(2)

def nc2 = nc.multiply(4)
println nc2.print()

Selection s = nc.isLessThan(3)

DoubleColumn subset = nc.where (nc.isLessThan(3))

assert subset.size() ==2

StringColumn sc = StringColumn.create("sc", new String[] {"foo", "bar", "baz", "foobarbaz"})

StringColumn scCopy = sc.copy()
scCopy = scCopy.replaceFirst("foo", "bar")
scCopy = scCopy.upperCase()
scCopy = scCopy.padEnd(5, 'x' as char) // put 4 x chars at the end of each string
scCopy = scCopy.substring(1, 5)

//levenstein distance
DoubleColumn distance = sc.distance(scCopy)

println "original string table " + sc.print()


println "modified string table " + scCopy.print()
println "string distance " + distance.print()

String[] animals = ["bear", "cat", "giraffe"]
double[] cuteness = [90.1, 84.3, 99.7]

Table cuteAnimals =
        Table.create("Cute Animals")
                .addColumns(
                        StringColumn.create("Animal types", animals),
                        DoubleColumn.create("rating", cuteness))

println cuteAnimals.structure()