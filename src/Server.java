import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Queue;

public class Server extends Thread {
    ServerSocket serverSocket;
    int port;
    Queue<String> buffer;
    short sourceProcessNum;

    public Server(short processNum, int port, Queue<String> buffer) {
        this.port = port;
        this.buffer = buffer;
        this.sourceProcessNum = processNum;

        try {
            serverSocket = new ServerSocket(port);
            // Receive first message, set process num
            serverSocket.accept();
        } catch (IOException e) {
            throw new RuntimeException("Unable to start server", e);
        }
    }

    public void run() {
        BufferedReader reader;
        PrintWriter writer;

        int rank = Arrays.binarySearch(Main.processNums, sourceProcessNum);
        int requestsToMake = Main.processNums.length - 1 - rank;

        for(short targetProcessNum : Main.processNums) {
            if ( targetProcessNum == sourceProcessNum) {
                continue;
            }

            if (requestsToMake > 0) {
                int retries = 5;
                // Send connection requests to lower priority servers
                while (retries > 0) {
                    try (Socket socket = new Socket(Main.generateUrl(sourceProcessNum), port)) {
                        socket.connect(new InetSocketAddress(Main.generateUrl(targetProcessNum), port), 1000);
                        Main.socketMap.put(targetProcessNum, socket);

                        writer = new PrintWriter(socket.getOutputStream());
                        break;
                    } catch (IOException e) {
                        System.err.println("Process" + sourceProcessNum + ":");
                        System.err.println("\tUnable to connect to " + Main.generateUrl(targetProcessNum) + ". " + retries + " tries left.");
                        System.err.println("\t" + retries + " tries left.");
                        retries--;
                        if (retries <= 0) {
                            throw new RuntimeException("Error establishing connection to " + Main.generateUrl(targetProcessNum));
                        }
                    }
                }

                requestsToMake--;
            } else {
                // Wait for connections from higher priority servers
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

        if (currentThread().isInterrupted()) {
            //TODO: Cleanup
            return;
        }


        
    }

}
