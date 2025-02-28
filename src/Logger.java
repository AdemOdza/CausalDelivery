import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
        this.filename = String.format("Project1Process%02d_%s.txt", self, now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")));
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        file = new File(filename);
        fileWriter = new PrintWriter(file, StandardCharsets.UTF_8);
        if(file.createNewFile()) {
            String log = String.format("Log file created: %s", filename);
            out(log);
        } else {
            throw new RuntimeException("Logger: Error creating log file");
        }

    }

    public synchronized void outAppend(String message) {
        LocalDateTime now = LocalDateTime.now();
        String log = String.format("[%s] Process %02d: %s", now.format(this.formatter), self, message);
        System.out.print(log);
        fileWriter.print(log);
    }

    public synchronized void out(String message) {
        LocalDateTime now = LocalDateTime.now();
        String log = String.format("[%s] Process %02d: %s", now.format(this.formatter), self, message);
        System.out.println(log);
        fileWriter.println(log);
    }

    public synchronized void errAppend(String message) {
        LocalDateTime now = LocalDateTime.now();
        String log = String.format("[%s] Process %02d ERROR: %s", now.format(this.formatter), self, message);
        System.err.print(log);
        fileWriter.print(log);
    }

    public synchronized void err(String message) {
        LocalDateTime now = LocalDateTime.now();
        String log = String.format("[%s] Process %02d ERROR: %s", now.format(this.formatter), self, message);
        System.err.println(log);
        fileWriter.println(log);
    }

    public void destroy() {
        fileWriter.close();
    }

}
