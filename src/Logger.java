import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private short self;
    private String filename;
    private DateTimeFormatter formatter;
    private File file;
    private PrintWriter fileWriter;

    public Logger(short self) throws IOException {
        this.self = self;
        LocalDateTime now = LocalDateTime.now();
        this.filename = String.format("Project1Process%02d.txt", self);
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        file = new File(filename);
//        System.out.println(file.getAbsolutePath());
//        fileWriter = new PrintWriter(file);
//        Files.deleteIfExists(file.toPath());
//        if(file.createNewFile()) {
//            String log = String.format("Log file created: %s", filename);
//            out(log);
//        } else {
//            throw new RuntimeException("Logger: Error creating log file");
//        }

    }

    public synchronized void outAppend(String message) {
        LocalDateTime now = LocalDateTime.now();
        String log = String.format("[%s] Process %02d: %s", now.format(this.formatter), self, message);
        System.out.print(log);
//        fileWriter.print(log);
//        try {
//            Files.write(file.toPath(), log.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    public synchronized void out(String message) {
        LocalDateTime now = LocalDateTime.now();
        String log = String.format("[%s] Process %02d: %s%n", now.format(this.formatter), self, message);
        System.out.println(log);
//        fileWriter.println(log);
//        try {
//            Files.write(file.toPath(), log.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    public synchronized void errAppend(String message) {
        LocalDateTime now = LocalDateTime.now();
        String log = String.format("[%s] Process %02d ERROR: %s", now.format(this.formatter), self, message);
        System.err.print(log);
//        fileWriter.print(log);
//        try {
//            Files.write(file.toPath(), log.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    public synchronized void err(String message) {
        LocalDateTime now = LocalDateTime.now();
        String log = String.format("[%s] Process %02d ERROR: %s%n", now.format(this.formatter), self, message);
        System.err.println(log);
//        fileWriter.println(log);
//        try {
//            Files.write(file.toPath(), log.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

//    public void destroy() {
//        fileWriter.close();
//    }

}
