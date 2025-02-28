import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Queue;

public class Server extends Thread {
//    ServerSocket serverSocket;
    int port;
    short processNum;
    int rank;
    boolean initialized = false;

    public Server(short processNum, int port) {
        this.port = port;
        this.processNum = processNum;
        this.rank = Arrays.binarySearch(Main.processNums, this.processNum);
    }

    // The server class will wait for connection requests from processes with a lower process number
    public void run() {
        BufferedReader reader;
        PrintWriter writer;


        int requestsToMake = Main.processNums.length - 1 - rank;

        // Connect to other processes
        System.out.println("Server: Waiting for connection requests from higher priority servers...");
        for(int i = 0; i < rank; i++) {
            try {
                waitForConnection();
            } catch (IOException e) {
                throw new RuntimeException("Server: Error establishing connection, i: " + i + ", message:  " + e.getMessage());
            } catch (InstantiationException e) {
                throw new RuntimeException("Server: Error parsing received message, i: " + i + ", message:  " + e.getMessage());
            }
        }
        System.out.println("Server: All connections established.");
        synchronized (Main.serverInitialized){
            Main.serverInitialized = true;
        }

        System.out.println("Server: Waiting for dispatcher initialization...");
        while(!Main.dispatcherInitialized) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Server: Error waiting for dispatcher initialization");
            }
        }

        //TODO: Verify
        int receivedMessages = 0;
        while(receivedMessages < 100 * Main.processNums.length) {
            if (currentThread().isInterrupted()) {
                break;
            }
            //Check each socket, add to queue and increment received count
            for(Socket curr : Main.socketMap.values()) {
                try {
                    System.out.println("Server: Receiving message from " + curr.getInetAddress() + ":" + curr.getPort() + "...");
                    reader = new BufferedReader(new InputStreamReader(curr.getInputStream()));
                    Main.variableNetworkDelay();
                    System.out.println("Server: Message received.");

                    String received = reader.readLine();
                    Message receivedMsg = new Message(received);
                    Main.buffer.add(receivedMsg);
                } catch (IOException | InstantiationException e) {
                    System.err.println("Server: Error receiving message");
                }
            }
            receivedMessages++;
        }
        
    }

    /**
     * A method of the Server class that waits for a connection to be made and returns the socket if successful.
     * @throws IOException  Socket error
     * @throws InstantiationException   Error parsing the message
     * @return  The created socket
     */

    public Socket waitForConnection() throws IOException, InstantiationException {
        BufferedReader reader;
        PrintWriter writer;

        int retries = 5;
        // TODO: Flip while and try? The server socket should stay alive
        while(retries > 0) {
            // Wait for connections from higher priority servers
            try (ServerSocket serverSocket = new ServerSocket(this.port)) {
                //Create server socket to listen for connections
                System.out.println("Server: Waiting for connection on port " + this.port + "...");
                Socket socket = serverSocket.accept();
                System.out.println("Server: Connection established.");

                // Wait for INIT Message
                System.out.println("Server: Waiting for INIT message...");
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String received = reader.readLine();
                Message receivedMsg = new Message(received);

                // Emulate variable network delay upon message reception
                Main.variableNetworkDelay();
                System.out.println("Server: Received message from " + Main.generateUrl(receivedMsg.source));

                System.out.println("Server: Updating clock...");
                // Update clock
                synchronized (Main.vectorClock) {
                    Main.vectorClock.update(receivedMsg.vectorClock.vector);
                }
                synchronized (Main.clock) {
                    Main.clock.setTime(Main.vectorClock.vector.get(processNum));
                }

                if(receivedMsg.command == Command.INIT) {
                    Main.socketMap.put(receivedMsg.source, socket);

                    writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                    synchronized (Main.clock) {
                        Main.clock.increment();
                    }
                    synchronized (Main.vectorClock) {
                        Main.vectorClock.put(processNum, Main.clock.getTime());
                    }

                    System.out.println("Server: Sending ACK message to " + Main.generateUrl(receivedMsg.source) + ":" + Main.portMap.getOrDefault(receivedMsg.source, -1) + "...");
                    Message msg = new Message(processNum, receivedMsg.source, Command.ACK, Main.vectorClock);
                    writer.append(msg.toString()).append('\n');
                    writer.flush();
                    System.out.println("Server: Connection to " + Main.generateUrl(receivedMsg.source) + " established and verified.");
                    return socket;
                }
                System.out.println("Server: Invalid message received. Expected INIT, received " + receivedMsg.command);
            }

            // Wait between retries
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            retries--;
            if(retries == 0) {
                throw new RuntimeException("Server: Retries exceeded. Unable to establish connection");
            }
        }
        throw new RuntimeException("Server: Unknown Error. Unable to establish connection");
    }
}
