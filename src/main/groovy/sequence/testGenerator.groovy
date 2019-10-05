package sequence

import com.softwood.utils.SequenceGenerator

SequenceGenerator seq = new SequenceGenerator()

long num = seq.nextId()

println "seq : $num"