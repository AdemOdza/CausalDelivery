import java.io.IOException;
import java.util.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    // Connection Info
    static final HashMap<Short, Socket> socketMap = new HashMap<>();
    static final HashMap<Short, Integer> portMap = new HashMap<>();
    static short[] processNums;
    // Message info
    static final PriorityQueue<Message> buffer = new PriorityQueue<>();
    static final LogicalClock clock = new LogicalClock();
    static final VectorClock vectorClock = new VectorClock();
    static Boolean serverInitialized = false;
    static Boolean dispatcherInitialized = false;
    static int messagesDelivered = 0;

    public static String generateUrl(Short processNum) {
        return String.format("dc%02d.utdallas.edu", processNum);
    }

    public static void main(String[] args) {
        // Set process nums from CLI
        // e.g. java main.class 2:5050 44:5055 11:5050 4:5050
        // 2 is self, 44,11,4 are other processes

        // Parse arguments
        if(args.length < 2) {
            System.err.println("Error: Multiple processes needed");
            System.out.println("Usage: java -jar Main.jar <process num>");
            return;
        }

        // Process storage
        processNums = new short[args.length];
        Socket[] connections = new Socket[args.length]; // Socket objects

        // Parse arguments
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

        // Initialize clock and vector clock
        System.out.println("Main: Initializing clock...");
        vectorClock.initialize(processNums);

        // Save self and sort processes for proper prioritization
        short self = processNums[0];
        Arrays.sort(processNums);

        System.out.println("Main: Starting server...");
        Server receiver = new Server(self, 5050);
        receiver.start();
        System.out.println("Main: Server started.");

        System.out.println("Main: Starting dispatcher...");
        Dispatcher dispatcher = new Dispatcher(self);
        dispatcher.start();
        System.out.println("Main: Dispatcher started.");


        // Wait for initialization
        System.out.println("Main: Waiting for socket initialization...");
        while(!dispatcherInitialized || !serverInitialized) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Sleep interrupted", e);
            }
        }


        // Add messages to buffer
        for (int i = 0; i < 100; i++) {
            // Wait for a random amount of time (0, 10] Milliseconds
            variableNetworkDelay();

            // Send messages to all processes
            for(int j = 1; j < connections.length; j++) {

            }
        }

        // Wait for Server and Dispatcher to be finished
        while(!(dispatcherInitialized || serverInitialized)) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Sleep interrupted", e);
            }
        }


        // Close connections
        for(Socket curr : socketMap.values()) {
            try {
                if(!curr.isClosed()) {
                    curr.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing connection");
            }
        }

        System.out.println("Main: Finished.");
        System.out.println("Messages delivered: " + messagesDelivered);
    }

    public static void variableNetworkDelay() {
        // Emulate variable network delay upon message reception
        try {
            int millis = (int)(Math.random() *  4000) + 1000;
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
