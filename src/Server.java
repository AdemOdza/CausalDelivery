import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Queue;

public class Server extends Thread {
//    ServerSocket serverSocket;
    int port;
    Queue<String> buffer;
    short processNum;
    int rank;
    boolean initialized = false;

    public Server(short processNum, int port, Queue<String> buffer) {
        this.port = port;
        this.buffer = buffer;
        this.processNum = processNum;
        this.rank = Arrays.binarySearch(Main.processNums, this.processNum);
    }

    // The server class will wait for connection requests from processes with a lower process number
    public void run() {
        BufferedReader reader;
        PrintWriter writer;


        int requestsToMake = Main.processNums.length - 1 - rank;

        // Connect to other processes
        for(int i = 0; i < rank; i++) {
            try {
                waitForConnection();
            } catch (IOException e) {
                throw new RuntimeException("Server: Error establishing connection, i: " + i + ", message:  " + e.getMessage());
            } catch (InstantiationException e) {
                throw new RuntimeException("Server: Error parsing received message, i: " + i + ", message:  " + e.getMessage());
            }
        }
//        for(short targetProcessNum : Main.processNums) {
//            if(this.initialized) {
//                break;
//            }
//            if (targetProcessNum == processNum) {
//                continue;
//            }
//            int retries = 5;
//            while(retries > 0) {
//                Socket socket;
//                try {
//                    if(requestsToMake > 0) {
//                        // Send connection requests to lower priority servers
//                        socket = new Socket(Main.generateUrl(processNum), Main.portMap.get(targetProcessNum));
//
//                        // Send Init Msg
//                        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
//                        Message msg = new Message(processNum, targetProcessNum, Command.INIT, Main.vectorClock);
//                        writer.append(msg.toString()).append('\n');
//                        writer.flush();
//
//                        // Wait for ACK
//                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                        String received = reader.readLine();
//                        Message receivedMsg = new Message(received);
//                        // TODO: Update and increment clocks
//                        if(receivedMsg.command == Command.ACK) {
//                            Main.socketMap.put(targetProcessNum, socket);
//                            requestsToMake--;
//                            break;
//                        }
//                    }
//                    else {
//                        // Wait for connections from higher priority servers
//                        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
//                            //Create server socket to listen for connections
//                            socket = serverSocket.accept();
//
//                            // Wait for INIT
//                            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                            String received = reader.readLine();
//                            Message receivedMsg = new Message(received);
//                            // TODO: Update and increment clocks
//                            if(receivedMsg.command == Command.INIT) {
//                                Main.socketMap.put(targetProcessNum, socket);
//
//                                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
//                                Message msg = new Message(processNum, targetProcessNum, Command.ACK, Main.vectorClock);
//                                writer.append(msg.toString()).append('\n');
//                                writer.flush();
//
//                                break;
//                            }
//                        }
//                    }
//                } catch (IOException e) {
//                    System.err.println("Process" + processNum + ":");
//                    System.err.println("\tUnable to connect to " + Main.generateUrl(targetProcessNum) + ". " + retries + " tries left.");
//
//                } catch (InstantiationException e) {
//                    System.err.println("Process" + processNum + ":");
//                    System.err.println("\tInvalid message received from " + Main.generateUrl(targetProcessNum) + ".");
//                }
//
//                retries--;
//                System.err.println("\t" + retries + " tries left.");
//
//                // Wait for a second
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//
//            }
//
//            // After 5 tries, fail
//            if (retries <= 0) {
//                throw new RuntimeException("Error establishing connection to " + Main.generateUrl(targetProcessNum));
//            }
//
//
//        }

        synchronized (Main.serverInitialized){
            Main.serverInitialized = true;
        }

        // Receive messages from other sockets, add to queue
        int receivedCount = 0;
        while(receivedCount < 100) {
            if (currentThread().isInterrupted()) {
                break;
            }
            //Check each socket, add to queue and increment received count
            receivedCount++;
        }

        //TODO: Check if main process is done sending before closing
        // OR: clean up in main
        for(Socket curr : Main.socketMap.values()) {
            try {
                if(!curr.isClosed()) {
                    curr.close();
                }
            } catch (IOException e) {
                System.out.println("Server: Error closing connection");
            }
        }

//
//        if(serverSocket != null) {
//            try {
//                socket = serverSocket.accept();
//                // Receive first message, set process num
//                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                while(targetProcessNum == -1) {
//                    String initMsg = reader.readLine();
//                    if (!initMsg.startsWith("BEGIN") || !initMsg.endsWith("END")) {
//                        continue;
//                    }
//                    initMsg = initMsg.replaceFirst("BEGIN", "");
//                    initMsg = initMsg.substring(0, initMsg.length() - 3);
//
//                    if (!initMsg.contains("PING")) {
//                        String[] tokens = initMsg.split("PING");
//                        if (Short.parseShort(tokens[1]) != sourceProcessNum) {
//                            continue;
//                        }
//                        targetProcessNum = Short.parseShort(tokens[0]);
//                        synchronized (Main.socketMap) {
//                            Main.socketMap.put(targetProcessNum, socket);
//                        }
//                        synchronized (Main.buffer) {
//                            String ackMsg = String.format("%02dACK%02dEND", sourceProcessNum, targetProcessNum);
//                            Main.buffer.add(ackMsg);
//                        }
//                        System.out.println("Successfully initialized");
//                    }
//                }
//            } catch (IOException e) {
//                throw new RuntimeException("Unable to start server", e);
//            }
//        } else {
//            throw new RuntimeException("Socket not found");
//        }




        
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
                Socket socket = serverSocket.accept();

                // Wait for INIT Message
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String received = reader.readLine();
                Message receivedMsg = new Message(received);

                // Emulate variable network delay upon message reception
                Main.variableNetworkDelay();

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

                    Message msg = new Message(processNum, receivedMsg.source, Command.ACK, Main.vectorClock);
                    writer.append(msg.toString()).append('\n');
                    writer.flush();
                    return socket;
                }
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
