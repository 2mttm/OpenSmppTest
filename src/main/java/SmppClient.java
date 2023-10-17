import org.smpp.Data;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.*;
import org.smpp.util.DataCodingCharsetHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class SmppClient {
    private static final Logger logger = Logger.getLogger(SmppClient.class.getName());

    private static Session session = null;
    private static boolean bound = false;
    private static int port = 1234;
    private static String systemId = "Smpp Client";
    private static String password = "password";
    private static String systemType = "Client";
    private static String serviceType = Data.DFLT_SRVTYPE;
    private static String sourceAddress = "localhost";
    private static TCPIPConnection connection;

    static String scheduleDeliveryTime = "";
    static String validityPeriod = "";
    static String messageId = "";

    static byte esmClass = 0;
    static byte protocolId = 0;
    static byte priorityFlag = 0;
    static byte registeredDelivery = 0;
    static byte replaceIfPresentFlag = 0;
    static byte dataCoding = 0;
    static byte smDefaultMsgId = 0;

    public static void main(String[] args) {
        loggerFormat();

        bind();
        submit("dest address", "Hello World!", "me", (byte) 1, (byte) 1);
        unbind();
    }

    private static void bind() {
        try {
            if (bound) {
                logger.info("Already bound, unbind first.");
                return;
            }
            connection = new TCPIPConnection("localhost", port);
            connection.setReceiveTimeout(5000);
            session = new Session(connection);

            BindRequest request = null;
            BindResponse response = null;

            request = new BindTransmitter();

            // set values
            request.setSystemId(systemId);
            //request.setPassword(password);
            //request.setSystemType(systemType);
            request.setInterfaceVersion((byte) 0x34);
            //request.setAddressRange(addressRange);

            // send the request
            logger.info("Bind request " + request.debugString());
            response = session.bind(request);
            logger.info("Bind response " + response.debugString());
            if (response.getCommandStatus() == Data.ESME_ROK) {
                bound = true;
            } else {
                logger.info("Bind failed, code " + response.getCommandStatus());
            }
        } catch (Exception e) {
            logger.info("Bind operation failed. " + e);
        }
    }

    private static void unbind() {
        try {

            if (!bound) {
                logger.info("Not bound, cannot unbind.");
                return;
            }

            // send the request
            Unbind unbind = new Unbind();
            logger.info("Unbind request: " + unbind.debugString());
            UnbindResp response = session.unbind();
            logger.info("Unbind response " + response.debugString());
            bound = false;
        } catch (Exception e) {
            logger.info("Unbind operation failed. " + e);
        }
    }

    private static void submit(String destAddress, String shortMessage, String sender, byte senderTon, byte senderNpi) {
        try {
            SubmitSM request = new SubmitSM();
            SubmitSMResp response;

            // set values
            request.setServiceType(serviceType);

            if(sender != null) {
                if(sender.startsWith("+")) {
                    sender = sender.substring(1);
                    senderTon = 1;
                    senderNpi = 1;
                }
                if(!sender.matches("\\d+")) {
                    senderTon = 5;
                    senderNpi = 0;
                }

                if(senderTon == 5) {
                    request.setSourceAddr(new Address(senderTon, senderNpi, sender, 11));
                } else {
                    request.setSourceAddr(new Address(senderTon, senderNpi, sender));
                }
            } else {
                request.setSourceAddr(sourceAddress);
            }

            if(destAddress.startsWith("+")) {
                destAddress = destAddress.substring(1);
            }
            request.setDestAddr(new Address((byte)1, (byte)1, destAddress));
            request.setReplaceIfPresentFlag(replaceIfPresentFlag);

            String encoding = DataCodingCharsetHandler.getCharsetName(dataCoding);
            request.setShortMessage(shortMessage, encoding);
            request.setScheduleDeliveryTime(scheduleDeliveryTime);
            request.setValidityPeriod(validityPeriod);
            request.setEsmClass(esmClass);
            request.setProtocolId(protocolId);
            request.setPriorityFlag(priorityFlag);
            request.setRegisteredDelivery(registeredDelivery);
            request.setDataCoding(dataCoding);
            request.setSmDefaultMsgId(smDefaultMsgId);

            // send the request

            //request.assignSequenceNumber(true);
            logger.info("Submit request " + request.debugString());
            response = session.submit(request);
            if (response != null){
                logger.info("Submit response " + response.debugString());
                messageId = response.getMessageId();
                enquireLink();
            }
            
        } catch (Exception e) {
            logger.info("Submit operation failed. " + e);
            //e.printStackTrace();
        }
    }

    private static void enquireLink() {
        try {
            EnquireLink request = new EnquireLink();
            EnquireLinkResp response;
            logger.info("Enquire Link request " + request.debugString());
            response = session.enquireLink(request);
            logger.info("Enquire Link response " + response.debugString());
        } catch (Exception e) {
            logger.info("Enquire Link operation failed. " + e);
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
