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

        // Go through all processes with a number greater than ours and attempt to connect.
        Main.logger.out("Dispatcher: Establishing connections to lower priority servers...");
        for(int i = rank + 1; i < Main.processNums.length; i++) {
            try {
                Main.socketMap.put(Main.processNums[i], makeConnection(Main.processNums[i]));
            } catch (IOException | InstantiationException e) {
                System.err.println("Dispatcher: Error establishing connection to " + Main.generateUrl(Main.processNums[i]));
                System.err.println(e.getMessage());
            }
        }

        Main.logger.out("Dispatcher: All connections established.");
        synchronized (Main.dispatcherInitialized) {
            Main.dispatcherInitialized = true;
        }
        Main.logger.out("Dispatcher: Waiting for server initialization...");
        while(!Main.serverInitialized) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Server: Error waiting for dispatcher initialization");
            }
        }

        // Broadcast messages to all processes
        for(int i = 0; i < 100; i++) {
            if (currentThread().isInterrupted()) {
                Main.logger.err("Dispatcher: Thread interrupted.");
                break;
            }
            // Emulate network delay
            Main.networkDelay(0, 10);

            // Increment clock once per broadcast and send message to all processes
            synchronized (Main.clock) {
                synchronized (Main.vectorClock) {
                    Main.clock.increment();
                    Main.vectorClock.put(self, Main.clock.getTime());
                }
            }
            String body = String.format("Hello from Process #%02d! Msg #%03d", self, i+1);
            PrintWriter writer;
            for(Short process : Main.socketMap.keySet()) {
                if(process == self) {
                    continue;
                }
                Main.logger.out(String.format("Dispatcher: Sending message %03d to %s", i+1, Main.generateUrl(process)));
                Message msg = new Message(self, process, Command.MESSAGE, body, Main.vectorClock);
                try {
                    writer = new PrintWriter(new OutputStreamWriter(Main.socketMap.get(process).getOutputStream(), StandardCharsets.UTF_8), true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                writer.append(msg.toString()).append('\n');
                writer.flush();
            }
        }
        Main.logger.out("Dispatcher: Finished.");
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

        // Update clock before sending INIT msg
        System.out.println("Dispatcher: Sending message, incrementing clock...");
        synchronized (Main.clock) {
            synchronized (Main.vectorClock) {
                Main.clock.increment();
                Main.vectorClock.put(self, Main.clock.getTime());
            }
        }

        // Build INIT message and write to socket stream
        Main.logger.out("Dispatcher: Sending INIT message to Process " + process + "...");
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        Message msg = new Message(self, process, Command.INIT, Main.vectorClock);
        writer.append(msg.toString()).append('\n');
        writer.flush();
        Main.logger.out("Dispatcher: INIT message sent. Waiting for ACK...");

        // Wait for ACK
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        Message receivedMsg = new Message(reader.readLine());
        if(receivedMsg.source != process) {
            throw new RuntimeException("Dispatcher: Received message from unexpected source.");
        }
        if(receivedMsg.command != Command.ACK) {
            throw new RuntimeException("Dispatcher: Received unexpected command.");
        }

        Main.logger.out("Dispatcher: Received ACK from " + Main.generateUrl(process));

        // Emulate variable network delay upon message reception
        Main.networkDelay();

        // On reception of ACK, update clocks with new timestamps and save confirmed socket
        Main.logger.out("Dispatcher: Received message, updating clock...");
        if(receivedMsg.command == Command.ACK) {
            // Update clock on reception of message
            synchronized (Main.clock){
                synchronized (Main.vectorClock){
                    Main.clock.increment();
                    Main.vectorClock.put(self, Main.clock.getTime());
                    Main.vectorClock.update(receivedMsg.vectorClock.vector);
                }
            }
            // Return confirmed socket to be saved into map
            Main.logger.out("Dispatcher: Connection to " + Main.generateUrl(process) + " established and verified.");
            return socket;
        }

        throw new RuntimeException("Error establishing connection to " + Main.generateUrl(process));
    }

}
