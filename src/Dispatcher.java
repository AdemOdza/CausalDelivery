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
        for(int i = rank + 1; i < Main.processNums.length; i++) {
            try {
                Main.socketMap.put(Main.processNums[i], makeConnection(Main.processNums[i]));
            } catch (IOException | InstantiationException e) {
                System.err.println("Dispatcher: Error establishing connection to " + Main.generateUrl(Main.processNums[i]));
            }
        }
        synchronized (Main.dispatcherInitialized){
            Main.dispatcherInitialized = true;
        }

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
        Socket socket = new Socket(Main.generateUrl(process), Main.portMap.get(process));

        // Update clock before sending INIT msg
        synchronized (Main.clock) {
            Main.clock.increment();
        }
        synchronized (Main.vectorClock) {
            Main.vectorClock.put(self, Main.clock.getTime());
        }

        // Build INIT message and write to socket stream
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        Message msg = new Message(self, process, Command.INIT, Main.vectorClock);
        writer.append(msg.toString()).append('\n');
        writer.flush();

        // Wait for ACK
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String received = reader.readLine();
        Message receivedMsg = new Message(received);

        // Emulate variable network delay upon message reception
        Main.variableNetworkDelay();

        // On reception of ACK, update clocks with new timestamps and save confirmed socket
        if(receivedMsg.command == Command.ACK) {
            synchronized (Main.vectorClock) {
                Main.vectorClock.update(receivedMsg.vectorClock.vector);
            }
            synchronized (Main.clock) {
                Main.clock.setTime(Main.vectorClock.vector.get(self));
            }
            return socket;
        }

        throw new RuntimeException("Error establishing connection to " + Main.generateUrl(process));
    }
}
