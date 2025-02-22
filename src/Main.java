import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static final HashMap<Short, Socket> socketMap = new HashMap<Short, Socket>();
    static final Queue<String> buffer = new ArrayDeque<>();
    static short[] processNums;

    public static String generateUrl(Short processNum) {
        return String.format("dc%02d.utdallas.edu", processNum);
    }

    public static void main(String[] args) {
        // Set process nums from CLI
        // e.g. java main.class 2 44 11 4
        // 2 is self, 44,11,4 are other processes
        // TODO: Refactor to use 2 threads, one for sending and one for receiving
        if(args.length < 2) {
            System.err.println("Error: Multiple processes needed");
            System.out.println("Usage: java -jar Main.jar <process num>");
        }

        // Process storage
        processNums = new short[args.length];
        Socket[] connections = new Socket[args.length]; // Socket objects

        for(int i = 0; i < args.length; i++) {
            try {
                processNums[i] = Short.parseShort(args[i]);
            } catch(NumberFormatException e) {
                throw new RuntimeException("Invalid process number passed into args");
            }
        }

        short self = processNums[0];
        Arrays.sort(processNums);

        Server receiver = new Server(self, 5050, buffer);
        receiver.start();

        // Establish connections
        for(int i = 1; i < processNums.length; i++) {
            String host = generateUrl(processNums[i]);
            try (Socket curr = new Socket(host, 5050)) {
                connections[i] = curr;
            } catch (UnknownHostException e) {
                System.out.println("Could not connect to unknown host " + host + ".");
            } catch (IOException e) {
                System.out.println("Error creating socket");
            }
        }
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
        for(Socket curr : connections) {
            try {
                if(!curr.isClosed()) {
                    curr.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing connection");
            }
        }
    }
}
