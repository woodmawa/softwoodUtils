
appender("Console-Appender", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"  //"%msg%n"
    }
}

//see https://dzone.com/articles/logback-configuration-using-groovy
logger("com.softwood.rules", INFO, ["Console-Appender" /*, "File-Appender", "Async-Appender"*/], false)

root(DEBUG,["Console-Appender"])