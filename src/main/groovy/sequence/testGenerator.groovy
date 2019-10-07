package sequence

import com.softwood.utils.SequenceGenerator

SequenceGenerator seq = new SequenceGenerator()

long num1 = seq.nextId()

long last = seq.lastTimestamp
long num2 = seq.nextId()
long num3 = seq.nextId()
long num4 = seq.nextId()
long num5 = seq.nextId()
long num6 = seq.nextId()

println "seq : [$num1, $num2, $num3, $num4, $num5, $num6]"

println seq.getDateStringForSequence(last)

println "exit"