import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Server extends Thread {
    int port;
    short processNum;
    int rank;

    public Server(short processNum, int port) {
        this.port = port;
        this.processNum = processNum;
        synchronized (Main.processNums) {
            this.rank = Arrays.binarySearch(Main.processNums, processNum);
        }
    }

    // The server class will wait for connection requests from processes with a lower process number
    public void run() {
        BufferedReader reader;

        // Connect to other processes
        Main.logger.out("Server: Waiting for connection requests from higher priority servers...");
        for (int i = 0; i < rank; i++) {
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
        synchronized (Main.serverInitialized) {
            Main.serverInitialized = true;
        }

        Main.logger.out("Server: Waiting for dispatcher initialization...");
        while (!Main.dispatcherInitialized) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Server: Error waiting for dispatcher initialization");
            }
        }

        ArrayList<Thread> threads = new ArrayList<>();
        synchronized (Main.socketMap) {
            for (Socket s : Main.socketMap.values()) {
                Thread t = new Thread(() -> {
                    int numReceived = 0;
                    BufferedReader r = null;
                    try {
                        r = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    } catch (IOException e) {
                        throw new RuntimeException("Server Worker: Error receiving message - " + e.getMessage());
                    }

                    while (numReceived < 100) {
                        try {
                            // Get raw message and parse
                            String received = null;
                            int retries = 5;
                            while ((received = r.readLine()) == null) {
                                Thread.sleep(100);
                                if (--retries == 0) {
                                    Main.logger.err("Server Worker: Retries exceeded. Unable to receive message #" + (numReceived + 1) + "from " + s.getInetAddress());
                                    throw new RuntimeException("Server Worker: Retries exceeded. Unable to receive message");
                                }
                            }
                            Message receivedMsg = new Message(received);

                            // Update clock on reception of message
                            synchronized (Main.clock) {
                                synchronized (Main.vectorClock) {
                                    Main.clock.increment();
                                    Main.vectorClock.put(processNum, Main.clock.getTime());
                                    Main.vectorClock.update(receivedMsg.vectorClock.vector);
                                }
                            }
                            // Check for termination and finish thread
                            if (receivedMsg.command == Command.TERMINATE) {
                                Main.logger.out("Server Worker: Received termination message from " + receivedMsg.source);
                                return;
                            }
                            // Add message to delivery buffer
                            Main.logger.out("Server Worker: Adding message to buffer: " + receivedMsg.source + " " + receivedMsg.body + " " + numReceived);
                            Main.buffer.add(receivedMsg);
                            numReceived++;
                        } catch (IOException | InstantiationException e) {
                            throw new RuntimeException("Server Worker: Error receiving message - " + e.getMessage());
                        } catch (InterruptedException e) {
                            throw new RuntimeException("Server Worker: Interrupted - " + e.getMessage());
                        }
                    }

                });
                threads.add(t);
            }
        }

        Main.logger.out("Server: Starting worker threads.");
        for (Thread t : threads) {
            t.start();
        }

        Main.logger.out("Server: All worker threads started.");
        for (Thread t : threads) {
            try {
                System.out.println("Joining thread");
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("Server: Error joining thread - " + e);
            }
        }
        Main.logger.out("Server: Finished.");

    }

    /**
     * A method of the Server class that waits for a connection to be made and returns the socket if successful.
     *
     * @return The created socket
     * @throws IOException            Socket error
     * @throws InstantiationException Error parsing the message
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
                    synchronized (Main.socketMap) {
                        Main.socketMap.put(receivedMsg.source, socket);
                    }
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
            if (retries == 0) {
                throw new RuntimeException("Server: Retries exceeded. Unable to establish connection");
            }
        }
        throw new RuntimeException("Server: Unknown Error. Unable to establish connection");
    }
}
