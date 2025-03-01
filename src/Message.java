import java.nio.charset.StandardCharsets;

public class Message implements Comparable<Message> {
    short source;
    short destination;
    Command command;
    String body;
    VectorClock vectorClock;

    public Message(short source, short destination, Command command, VectorClock vectorClock) {
        this.source = source;
        this.destination = destination;
        this.command = command;
        this.body = "";
        this.vectorClock = vectorClock;
    }

    public Message(short source, short destination, Command command, String body, VectorClock vectorClock) {
        this.source = source;
        this.destination = destination;
        this.command = command;
        this.body = body;
        this.vectorClock = vectorClock;
    }

    public Message(String rawMessage) throws InstantiationException {
        System.out.println("Debug 1: " + rawMessage.trim());
        if (!rawMessage.startsWith("BEGIN") || !rawMessage.endsWith("END")) {
            throw new InstantiationException("Message does not contain boundary strings.");
        }
        // Message format: BEGIN<source><command><destination><body>CLOCK<clock>END

        // Trim boundaries
        String debugMsg = rawMessage;
        rawMessage = rawMessage.replaceFirst("BEGIN", "");
        rawMessage = rawMessage.substring(0, rawMessage.length() - 3);
        // <source><command><destination><body>CLOCK<clock>

        // Extract and decode vector clock
        String[] tokens = rawMessage.split("CLOCK", 2);
        try {
            this.vectorClock = new VectorClock(tokens[1].getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("Error decoding vector clock. " + e.getMessage());
            System.out.println(debugMsg);
            throw new InstantiationException("Error decoding vector clock.");
        }
        rawMessage = tokens[0];
        // <source><command><destination><body>

        // Parse command. Use first found command, any others will be considered part of the body
        this.command = Command.NOOP;
        int commandIndex = Integer.MAX_VALUE;
        for (Command c : Command.values()) {
            if (rawMessage.contains(c.name()) && rawMessage.indexOf(c.name()) < commandIndex) {
                this.command = c;
                commandIndex = rawMessage.indexOf(c.name());
            }
        }
        if(this.command == Command.NOOP) {
            throw new InstantiationException("Message does not contain a valid command.");
        }

        tokens = rawMessage.split(this.command.name(), 2);
        this.source = Short.parseShort(tokens[0]);
        this.destination = Short.parseShort(tokens[1].substring(0, 2));
        this.body = tokens[1].substring(2);
    }

    @Override
    public String toString() {
        return String.format("BEGIN%02d%S%02d%sCLOCK%sEND",
                source,
                command,
                destination,
                body,
                new String(vectorClock.serialize(), StandardCharsets.UTF_8)
        );
    }

    @Override
    public int compareTo(Message o) {
        return this.vectorClock.compareTo(o.vectorClock);
    }
}
