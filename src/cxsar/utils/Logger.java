package cxsar.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class Logger {

    private static Logger instance = new Logger();

    // Prefix of the message
    private String logPrefix = "[cxsar]";

    // File we write our error & exception log to
    private File outFile;

    // Output stream
    private BufferedOutputStream outputStream;

    // Wheter the output stream is actually open
    private boolean outputStreamOpen = false;

    public static Logger getInstance() {
        return instance;
    }

    public boolean attemptOpenOutputstream() {
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(outFile));
        } catch (Exception e) {
            e.printStackTrace();
            return false; // if we even end up here
        }

        outputStreamOpen = true;
        return true;
    }

    public void writeToOutputStream(String message) {
        try {
            if (!outputStreamOpen)
                attemptOpenOutputstream();

            if (outputStreamOpen)
                outputStream.write(message.getBytes());

            if (outputStreamOpen) {
                outputStream.close();
                outputStreamOpen = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            //handleException(e);
        }
    }

    public void log(String message, LogLevel level) {
        if((level == LogLevel.ERROR || level == LogLevel.EXCEPTION) && outFile == null)
            outFile = new File(String.format("cxsar-error-%d.txt", System.nanoTime()));

        switch (level) {
            case NORMAL:
                System.out.printf("%s %s%n", logPrefix, message);
                break;
            case ERROR:
                System.err.printf("%s [error] %s%n", logPrefix, message);
                writeToOutputStream(String.format("%s [error] %s", logPrefix, message));
                break;
            case EXCEPTION:
                writeToOutputStream(String.format("###### EXCEPTION CAUGHT ######\n%s [EXCEPTION: ] %s", logPrefix, message));
                break;
        }
    }

    public void log(String format, Object... args) {
        log(String.format(format, args));
    }

    public void log(LogLevel level, String format, Object... args) {
        log(String.format(format, args), level);
    }

    public void log(String message) {
        this.log(message, LogLevel.NORMAL);
    }

    public void handleException(Exception e) {
        try {
            log(e.toString(), LogLevel.EXCEPTION);

            throw e;
        } catch (Exception _) {
            System.exit(_.hashCode());
        }
    }

}
