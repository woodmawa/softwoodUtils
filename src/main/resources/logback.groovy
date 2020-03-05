
appender("Console-Appender", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"  //"%msg%n"
    }
}


root(DEBUG,["Console-Appender"])