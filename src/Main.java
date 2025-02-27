import java.io.IOException;
import java.util.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    // Connection Info
    static final HashMap<Short, Socket> socketMap = new HashMap<Short, Socket>();
    static final HashMap<Short, Integer> portMap = new HashMap<Short, Integer>();
    static short[] processNums;
    // Message info
    static final Queue<String> buffer = new ArrayDeque<>();
    static final LogicalClock clock = new LogicalClock();
    static final VectorClock vectorClock = new VectorClock();
    static final String LOCK = "";
    static Boolean serverInitialized = false;
    static Boolean dispatcherInitialized = false;

    public static String generateUrl(Short processNum) {
        return String.format("dc%02d.utdallas.edu", processNum);
    }

    public static void main(String[] args) {
        // Set process nums from CLI
        // e.g. java main.class 2:5050 44:5055 11:5050 4:5050
        // 2 is self, 44,11,4 are other processes

        if(args.length < 2) {
            System.err.println("Error: Multiple processes needed");
            System.out.println("Usage: java -jar Main.jar <process num>");
        }

        // Process storage
        processNums = new short[args.length];
        Socket[] connections = new Socket[args.length]; // Socket objects

        for(int i = 0; i < args.length; i++) {
            try {
                if(!args[i].contains(":")) {
                    throw new RuntimeException("Port missing from arg " + i + ": \"" + args[i] + "\"");
                }
                String[] tokens = args[i].split(":");
                processNums[i] = Short.parseShort(tokens[0]);
                portMap.put(processNums[i], Integer.parseInt(tokens[1]));
            } catch(NumberFormatException e) {
                throw new RuntimeException("Invalid process number passed into args");
            }
        }

        vectorClock.initialize(processNums);
        short self = processNums[0];
        Arrays.sort(processNums);

        Server receiver = new Server(self, 5050, buffer);
        receiver.start();

        // TODO: Create sockets to other processes

        // Verify that other processes are connected
        // TODO: Send pings and wait for response?


        // Send messages
        for (int i = 0; i < 100; i++) {
            // Wait for a random amount of time (0, 10] Milliseconds
            try {
                int millis = (int)(Math.random() * 10);
                TimeUnit.MILLISECONDS.sleep(millis);
            } catch (InterruptedException e) {
                throw new RuntimeException("Sleep interrupted", e);
            }

            // Send messages to all processes
            for(int j = 1; j < connections.length; j++) {
                System.out.println("TODO: Send message to process " + j);
                // TODO: Send messages
                // TODO: Determine protocol
                // // process number + command (PING, MESSAGE, ACK) + target process + terminator?
                // // e.g. 07PING22END -> 22ACK07END -> 07MESSAGE22END -> 22MESSAGE07END
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

    public static void initializeDispatcher() {
        dispatcherInitialized = true;
    }
}
