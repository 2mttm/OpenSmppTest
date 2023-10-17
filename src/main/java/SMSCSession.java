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

    public SMSCSession(Connection connection) {
        this.connection = connection;
        transmitter = new Transmitter(connection);
        receiver = new Receiver(transmitter, connection);
    }

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

    public void send(PDU pdu) throws IOException, PDUException {
        timeoutCntr = 0;
        logger.info("SMSCSession going to send pdu over transmitter");
        transmitter.send(pdu);
        logger.info("SMSCSession pdu sent over transmitter");
    }

    public void setReceiveTimeout(long timeout) {
        receiveTimeout = timeout;
    }

    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    public Object getAccount() {
        return null;
    }

    public void setAccount(Object account) {}

    public boolean isReceiving() {
        return isReceiving;
    }

    public void setReceiving(boolean isReceiving) {
        this.isReceiving = isReceiving;
    }

    public Connection getConnection() {
        return connection;
    }
}