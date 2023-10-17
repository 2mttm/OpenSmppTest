/*
 * Copyright (c) 1996-2001
 * Logica Mobile Networks Limited
 * All rights reserved.
 *
 * This software is distributed under Logica Open Source License Version 1.0
 * ("Licence Agreement"). You shall use it and distribute only in accordance
 * with the terms of the License Agreement.
 *
 */

import java.io.IOException;
import java.util.logging.Logger;

import org.smpp.*;
import org.smpp.pdu.*;

public class SMSCSession extends SmppObject implements Runnable{
    private static Logger logger = Logger.getLogger(SMSCSession.class.getName());
    
    private Receiver receiver;
    private Transmitter transmitter;
    private Connection connection;
    private long receiveTimeout = Data.RECEIVER_TIMEOUT;
    private boolean keepReceiving = true;
    private boolean isReceiving = false;
    private int timeoutCntr = 0;

    /**
     * Initialises the session with the connection the session
     * should communicate over.
     * @param connection the connection object for communication with client
     */
    public SMSCSession(Connection connection) {
        this.connection = connection;
        transmitter = new Transmitter(connection);
        receiver = new Receiver(transmitter, connection);
    }

    /**
     * Signals the session's thread that it should stop.
     * Doesn't wait for the thread to be completly finished.
     * Note that it can take some time before the thread is completly
     * stopped.
     * @see #run()
     */
    public void stop() {
        debug.write("SMSCSession stopping");
        keepReceiving = false;
    }

    public void run() {
        PDU pdu = null;

        receiver.start();
        isReceiving = true;
        try {
            while (keepReceiving) {
                try {
                    //logger.info("SMSCSession going to receive a PDU");
                    pdu = receiver.receive(getReceiveTimeout());
                } catch (Exception e) {
                    logger.info("SMSCSession caught exception receiving PDU " + e.getMessage());
                }

                if (pdu != null) {
                    timeoutCntr = 0;
                    if (pdu.isRequest()) {
                        logger.info("SMSCSession got request: " + pdu.debugString());
                        Response response = ((Request)pdu).getResponse();
                        logger.info("Sending response: " + response.debugString());
                        transmitter.send(response);
                    } else if (pdu.isResponse()) {
                        logger.info("SMSCSession got response " + pdu.debugString());
                        //transmitter.send((Response) pdu);
                    } else {
                        logger.info("SMSCSession not reqest nor response => not doing anything.");
                    }
                } else {
                    timeoutCntr++;
                    if (timeoutCntr > 5) {
                        logger.info("SMSCSession stoped due to inactivity");
                        stop();
                    }
                }
            }
        } catch (ValueNotSetException | IOException e) {
            e.printStackTrace();
        } finally {
            isReceiving = false;
        }
        logger.info("SMSCSession stopping receiver");
        receiver.stop();

        try {
            logger.info("SMSCSession closing connection");
            connection.close();
        } catch (IOException e) {
            logger.info("closing SMSCSession's connection.");
        }
        logger.info("SMSCSession exiting run()");
    }

    /**
     * Sends a PDU to the client.
     * @param pdu the PDU to send
     */
    public void send(PDU pdu) throws IOException, PDUException {
        timeoutCntr = 0;
        logger.info("SMSCSession going to send pdu over transmitter");
        transmitter.send(pdu);
        logger.info("SMSCSession pdu sent over transmitter");
    }

    /**
     * Sets the timeout for receiving the complete message.
     * @param timeout the new timeout value
     */
    public void setReceiveTimeout(long timeout) {
        receiveTimeout = timeout;
    }

    /**
     * Returns the current setting of receiving timeout.
     * @return the current timeout value
     */
    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    /**
     * Returns the details about the account that is logged in to this session
     * @return An object representing the account. It is casted to the correct type by the implementation
     */
    public Object getAccount() {
        return null;
    }

    /**
     * Set details about the account that is logged in to this session
     * @param account An object representing the account. It is casted to the correct type by the implementation
     */
    public void setAccount(Object account) {
    }

    /**
     * @return Returns the isReceiving.
     */
    public boolean isReceiving() {
        return isReceiving;
    }

    /**
     * @param isReceiving The isReceiving to set.
     */
    public void setReceiving(boolean isReceiving) {
        this.isReceiving = isReceiving;
    }

    public Connection getConnection() {
        return connection;
    }
}
/*
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2003/09/30 09:17:49  sverkera
 * Created an interface for SMSCListener and SMSCSession and implementations of them  so that it is possible to provide other implementations of these classes.
 *
 * Revision 1.1  2003/07/23 00:28:39  sverkera
 * Imported
 *
 */
