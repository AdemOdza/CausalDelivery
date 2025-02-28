import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Dispatcher extends Thread implements Runnable {
    short self;
    
    public Dispatcher(short self) {
        this.self = self;
    }

    //The Dispatcher class will send requests to processes with a higher process number
    @Override
    public void run() {
        int rank = Arrays.binarySearch(Main.processNums, self);
        int requestsToMake = Main.processNums.length - 1 - rank;

        // Go through all processes with a number greater than ours and attempt to connect.
        System.out.println("Dispatcher: Establishing connections to lower priority servers...");
        for(int i = rank + 1; i < Main.processNums.length; i++) {
            try {
                Main.socketMap.put(Main.processNums[i], makeConnection(Main.processNums[i]));
            } catch (IOException | InstantiationException e) {
                System.err.println("Dispatcher: Error establishing connection to " + Main.generateUrl(Main.processNums[i]));
            }
        }

        System.out.println("Dispatcher: All connections established.");
        synchronized (Main.dispatcherInitialized){
            Main.dispatcherInitialized = true;
        }
        System.out.println("Dispatcher: Waiting for server initialization...");
        while(!Main.serverInitialized) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Server: Error waiting for dispatcher initialization");
            }
        }
        //TODO: Implement message sending.
        // Wait for a random amount of time in the range (0,10] millisecond before sending

    }

    /**
     * Attempts to make a connection to the specified process. Returns the socket if successful
     * @param process The process number we wish to connect to
     * @return The created socket
     * @throws IOException Error creating socket
     * @throws InstantiationException Error parsing message
     **/
    public Socket makeConnection(Short process) throws IOException, InstantiationException {
        BufferedReader reader;
        PrintWriter writer;
        
        // Send connection requests to lower priority servers
        System.out.println("Dispatcher: Connecting to " + Main.generateUrl(process));
        Socket socket = new Socket(Main.generateUrl(process), Main.portMap.get(process));
        System.out.println("Dispatcher: Connected to " + Main.generateUrl(process));

        // Update clock before sending INIT msg
        System.out.println("Dispatcher: Incrementing clock...");
        synchronized (Main.clock) {
            Main.clock.increment();
        }
        synchronized (Main.vectorClock) {
            Main.vectorClock.put(self, Main.clock.getTime());
        }

        // Build INIT message and write to socket stream
        System.out.println("Dispatcher: Sending INIT message...");
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        Message msg = new Message(self, process, Command.INIT, Main.vectorClock);
        writer.append(msg.toString()).append('\n');
        writer.flush();
        System.out.println("Dispatcher: INIT message sent. Waiting for ACK...");

        // Wait for ACK
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String received = reader.readLine();
        Message receivedMsg = new Message(received);
        System.out.println("Dispatcher: Received ACK from " + Main.generateUrl(process));

        // Emulate variable network delay upon message reception
        Main.variableNetworkDelay();

        // On reception of ACK, update clocks with new timestamps and save confirmed socket
        System.out.println("Dispatcher: Updating clocks...");
        if(receivedMsg.command == Command.ACK) {
            synchronized (Main.vectorClock) {
                Main.vectorClock.update(receivedMsg.vectorClock.vector);
            }
            synchronized (Main.clock) {
                Main.clock.setTime(Main.vectorClock.vector.get(self));
            }
            System.out.println("Dispatcher: Connection to " + Main.generateUrl(process) + " established and verified.");
            return socket;
        }

        throw new RuntimeException("Error establishing connection to " + Main.generateUrl(process));
    }
}
