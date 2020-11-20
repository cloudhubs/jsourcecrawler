package org.baylor.ecs.cloudhubs.sourcecrawler.request;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.*;

public class RequestLogger {

    protected static final String LOG_NAME = "RequestLog";
    protected static final String LOG_FILE = "request.log";
    public static Logger logger = null;

    public RequestLogger() throws IOException {
        createLogger();
    }
    /**
     * Creates a logger for the server
     * @throws IOException if i/o error
     */
    public static void createLogger() throws IOException {
        logger = Logger.getLogger(LOG_NAME);                      //Create instance of logger
        FileHandler fh = new FileHandler(LOG_FILE, true); //Create file to log to

        //Create logger format
        fh.setFormatter(new SimpleFormatter(){
            @Override
            public String format(LogRecord record) {
                return String.format(record.getLevel() + " " + record.getMessage() + "\n");
            }
        });
        logger.addHandler(fh);    //Add file handler to the logger
        logger.setLevel(Level.INFO);

        //Redirect console output to log file
//        System.setErr(new PrintStream(new FileOutputStream(LOG_FILE, true)));

//        return logger;
    }
}
