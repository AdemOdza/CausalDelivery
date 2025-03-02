import java.io.IOException;
//import java.util.concurrent.locks.Lock;
import java.util.*;
import java.net.Socket;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    // Connection Info
    static Logger logger;
    static final HashMap<Short, Socket> socketMap = new HashMap<>();
    static final HashMap<Short, Integer> portMap = new HashMap<>();
    static short[] processNums;

    // Message info
    static final PriorityBlockingQueue<Message> buffer = new PriorityBlockingQueue<>();
    static final LogicalClock clock = new LogicalClock();
    static final VectorClock vectorClock = new VectorClock();
    static Boolean serverInitialized = false;
    static Boolean dispatcherInitialized = false;

    public static String generateUrl(Short processNum) {
        return String.format("dc%02d.utdallas.edu", processNum);
    }

    public static void main(String[] args) {
        // Set process nums from CLI
        // e.g. java main.class 2:5050 44:5055 11:5050 4:5050
        // 2 is self, 44,11,4 are other processes

        // Vaildate arguments
        if(args.length < 2) {
            System.err.println("Error: Multiple processes needed");
            System.out.println("Usage: java -jar Main.jar <process:port pairs>");
            return;
        }

        // Parse processes and ports
        processNums = new short[args.length];
        for(int i = 0; i < args.length; i++) {
            try {
                if(!args[i].contains(":")) {
                    throw new RuntimeException("Port missing from arg " + i + ": \"" + args[i] + "\"");
                }
                String[] tokens = args[i].split(":");
                processNums[i] = Short.parseShort(tokens[0]);
                portMap.put(processNums[i], Integer.parseInt(tokens[1]));
                System.out.println("Main: Parsed arg Process " + processNums[i] + " on port " + portMap.get(processNums[i]));
            } catch(NumberFormatException e) {
                throw new RuntimeException("Invalid process number passed into args");
            }
        }

        // Save self and sort processes for proper prioritization
        short self = processNums[0];
        Arrays.sort(processNums);

        // Initialize synchronized logger
        try {
            logger = new Logger(self);
        } catch (IOException e) {
            throw new RuntimeException("Error creating logger: ", e);
        }

        // Initialize clock and vector clock
        logger.out("Main: Initializing clock...");
        vectorClock.initialize(processNums);
        logger.out("Main: Clock initialized.");

        // Start server thread
        logger.out("Main: Starting server...");
        Server receiver = new Server(self, portMap.get(self));
        receiver.start();
        System.out.println("Main: Server started.");

        // Start dispatcher thread
        logger.out("Main: Starting dispatcher...");
        Dispatcher dispatcher = new Dispatcher(self);
        dispatcher.start();
        logger.out("Main: Dispatcher started.");

        // Go through queue and "deliver" messages. AKA remove from queue
        logger.out("Main: Delivering messages...");
        int messagesDelivered = 0;
        while(messagesDelivered < (processNums.length * 100)) {
            try {
                synchronized (buffer) {
                    Message msg = buffer.poll(250, TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        logger.out(String.format("Message %03d from process %02d delivered: \"%s\"", messagesDelivered + 1, msg.source, msg.body));
                        messagesDelivered++;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        logger.out("Main: All messages delivered.");

        // Wait for threads to finish
        try {
            dispatcher.join();
            receiver.join();
            System.out.println("ASDFOJKHASDGFIPKJHGSDFJLHGBSDFLIJHFSDG");
        } catch (InterruptedException e) {
            logger.err("Error waiting for threads to finish - " + e.getMessage());
        }

        // Close connections
        for(Socket curr : socketMap.values()) {
            try {
                if(!curr.isClosed()) {
                    curr.close();
                }
            } catch (IOException e) {
                logger.err("Main: Error closing connection");
            }
        }

        logger.out("Main: Finished.");
        logger.out("Messages delivered: " + messagesDelivered);
        logger.destroy();
    }

    public static void networkDelay() {
        // Emulate variable network delay upon message reception
        networkDelay(1, 10);
    }

    public static void networkDelay(int lowerBoundMillis, int upperBoundMillis) {
        // Emulate variable network delay upon message reception
        try {
            int millis = (int)(Math.random() *  (upperBoundMillis-lowerBoundMillis)) + lowerBoundMillis;
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
