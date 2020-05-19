package scripts

//how to encode and decode a older Date format without losing precision
//https://stackoverflow.com/questions/61821985/java-date-conversion-to-string-and-back-to-date-fail-equality-check

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

Date now = new Date()
SimpleDateFormat sdtf = new SimpleDateFormat('EEE MMM dd HH:mm:ss.SSS z yyyy')

String fds = sdtf.format (now)

Date parsedDate = sdtf.parse(fds)

println now.toString()


println fds

assert parsedDate == now

