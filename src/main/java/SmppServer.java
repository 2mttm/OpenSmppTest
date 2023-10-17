import org.smpp.*;
import org.smpp.pdu.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class SmppServer implements Runnable{
    private static Logger logger = Logger.getLogger(SmppServer.class.getName());
    private Connection connection;
    private static boolean keepRunning = true;
    private boolean isUp = false;
    private boolean asynchronous = true;
    private int port = 1234;
    private int timeout = 5000;

    public SmppServer(){
        connection = new TCPIPConnection(port);
        connection.setReceiveTimeout(timeout);
        try {
            connection.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        loggerFormat();

        SmppServer server = new SmppServer();
        server.start();

    }

    private void start() throws IOException {
        if (!isUp) {
            isUp = true;
            logger.info("Starting listener... ");

            if (asynchronous){
                //logger.info("Starting listener in separate thread.");
                Thread serverThread = new Thread(this);
                serverThread.start();
                logger.info("Listener started in separate thread.");
            }

        } else {
            logger.info("Listener is already running.");
        }
    }

    public synchronized void stop() throws IOException {
        logger.info("going to stop SMSCListener");
        isUp = false;
        Thread.yield();
        connection.close();
        logger.info("SMSCListener stopped");
    }

    protected void exit() throws IOException {
        stop();
        keepRunning = false;
    }

    public void run() {
        isUp = true;
        try {
            while (isUp) {
                listen();
                Thread.yield();
            }
        } finally {
            isUp = false;
        }
    }

    private void listen() {
        try {
            connection.setReceiveTimeout(timeout);
            Connection listenConnection = connection.accept();
            PDU pdu = null;
            if (listenConnection != null) {
                logger.info("Listener accepted a connection on port " + port);
                SMSCSession session = new SMSCSession(listenConnection);
                session.setReceiveTimeout(timeout);
                session.getConnection().setReceiveTimeout(timeout);
                Thread thread = new Thread(session);
                thread.start();

                logger.info("Listener launched a session on the accepted connection.");

            } else {
                logger.info("Waiting for an incoming connection...");
            }
        } catch (InterruptedIOException e) {
            // thrown when the timeout expires => it's ok, we just didn't
            // receive anything
            logger.info("InterruptedIOException accepting, timeout? -> " + e);
        } catch (IOException e) {
            // accept can throw this from various reasons
            // and we don't want to continue then (?)
            logger.info("IOException accepting connection" + e);
        } catch (NullPointerException e){
            logger.info("No PDU received");;
        }
    }

    private static void loggerFormat(){
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()){
            rootLogger.removeHandler(handler);
        }
        rootLogger.setLevel(Level.ALL);
        Handler rootHandler = new ConsoleHandler();
        rootHandler.setFormatter(new SimpleFormatter(){
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            @Override
            public synchronized String format(java.util.logging.LogRecord record) {
                String dateTime = dateFormat.format(new Date(record.getMillis()));
                String level = record.getLevel().toString();
                String message = formatMessage(record);

                return dateTime + " [" + level + "]: " + message + "\n";
            }
        });
        rootLogger.addHandler(rootHandler);
    }
}