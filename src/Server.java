import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Server extends Thread {
    int port;
    short processNum;
    int rank;

    public Server(short processNum, int port) {
        this.port = port;
        this.processNum = processNum;
        this.rank = Arrays.binarySearch(Main.processNums, this.processNum);
    }

    // The server class will wait for connection requests from processes with a lower process number
    public void run() {
        BufferedReader reader;

        // Connect to other processes
        Main.logger.out("Server: Waiting for connection requests from higher priority servers...");
        for(int i = 0; i < rank; i++) {
            try {
                waitForConnection();
            } catch (IOException e) {
                throw new RuntimeException("Server: Error establishing connection, i: " + i + ", message:  " + e.getMessage());
            } catch (InstantiationException e) {
                throw new RuntimeException("Server: Error parsing received message, i: " + i + ", message:  " + e.getMessage());
            } catch (InterruptedException e) {
                throw new RuntimeException("Server: Thread error, i: " + i + ", message:  " + e.getMessage());
            }
        }

        Main.logger.out("Server: All connections established.");
        synchronized (Main.serverInitialized){
            Main.serverInitialized = true;
        }

        Main.logger.out("Server: Waiting for dispatcher initialization...");
        while(!Main.dispatcherInitialized) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Server: Error waiting for dispatcher initialization");
            }
        }

        //TODO: Rework this, not working correctly
        int receivedMessages = 0;
        while(receivedMessages < 100 * Main.processNums.length) {
            if (currentThread().isInterrupted()) {
                Main.logger.err("Server: Thread interrupted.");
                break;
            }
            //Check each socket, add to queue and increment received count
            for(Socket curr : Main.socketMap.values()) {
                try {
                    // Attempt to receive message
                    Main.logger.out("Server: Receiving message from " + curr.getInetAddress() + ":" + curr.getPort() + "...");
                    reader = new BufferedReader(new InputStreamReader(curr.getInputStream()));
                    Main.networkDelay();
                    Main.logger.out("Server: Message received.");

                    // Get raw message and parse
                    String received = reader.readLine();
                    for(int retries = 5; retries > 0; retries--) {
                        if(received != null) {
                            break;
                        }
                        Thread.sleep(200);
                        received = reader.readLine();
                    }
                    Message receivedMsg = new Message(received);

                    // Update clock on reception of message
                    synchronized (Main.clock){
                        synchronized (Main.vectorClock){
                            Main.clock.increment();
                            Main.vectorClock.put(processNum, Main.clock.getTime());
                            Main.vectorClock.update(receivedMsg.vectorClock.vector);
                        }
                    }

                    // Add message to delivery buffer
                    Main.buffer.add(receivedMsg);
                    receivedMessages++;
                } catch (IOException | InstantiationException e) {
                    Main.logger.err("Server: Error receiving message - " + e.getMessage());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Server Received: " + receivedMessages);
        }

        Main.logger.out("Server: Finished.");
        synchronized (Main.serverInitialized){
            Main.serverInitialized = false;
        }

    }

    /**
     * A method of the Server class that waits for a connection to be made and returns the socket if successful.
     * @throws IOException  Socket error
     * @throws InstantiationException   Error parsing the message
     * @return  The created socket
     */

    public Socket waitForConnection() throws IOException, InstantiationException, InterruptedException {
        BufferedReader reader;
        PrintWriter writer;

        // Wait for connections from higher priority servers
        int retries = 5;
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            while (retries > 0) {
                //Create server socket to listen for connections
                Main.logger.out("Server: Waiting for connection on port " + this.port + "...");
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    retries--;
                    // Wait between retries
                    Thread.sleep(1000);
                    continue;
                }
                Main.logger.out("Server: Connection established.");

                // Wait for INIT Message
                Main.logger.out("Server: Waiting for INIT message...");
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String received = reader.readLine();
                Message receivedMsg = new Message(received);

                // Emulate variable network delay upon message reception
                Main.networkDelay();
                Main.logger.out("Server: Received message from " + Main.generateUrl(receivedMsg.source));

                // Update clock
                Main.logger.out("Server: Updating clock...");
                synchronized (Main.clock) {
                    synchronized (Main.vectorClock) {
                        Main.vectorClock.update(receivedMsg.vectorClock.vector);
                        Main.clock.setTime(Main.vectorClock.vector.get(processNum));
                    }
                }

                // When we receive an init message, save the socket and send an ACK response
                if (receivedMsg.command == Command.INIT) {
                    Main.socketMap.put(receivedMsg.source, socket);
                    writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                    synchronized (Main.clock) {
                        synchronized (Main.vectorClock) {
                            Main.clock.increment();
                            Main.vectorClock.put(processNum, Main.clock.getTime());
                        }
                    }

                    Main.logger.out("Server: Sending ACK message to " + Main.generateUrl(receivedMsg.source) + ":" + Main.portMap.getOrDefault(receivedMsg.source, -1) + "...");
                    Message msg = new Message(processNum, receivedMsg.source, Command.ACK, Main.vectorClock);
                    writer.append(msg.toString()).append('\n');
                    writer.flush();
                    Main.logger.out("Server: Connection to " + Main.generateUrl(receivedMsg.source) + " established and verified.");
                    return socket;
                }

                Main.logger.out("Server: Invalid message received. Expected INIT, received " + receivedMsg.command);
            }
            if(retries == 0) {
                throw new RuntimeException("Server: Retries exceeded. Unable to establish connection");
            }
        }
        throw new RuntimeException("Server: Unknown Error. Unable to establish connection");
    }
}
