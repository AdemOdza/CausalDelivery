import java.nio.charset.StandardCharsets;

public class Message {
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
        if (!rawMessage.startsWith("BEGIN") || !rawMessage.endsWith("END")) {
            throw new InstantiationException("Message does not contain boundary strings.");
        }

        rawMessage = rawMessage.replaceFirst("BEGIN", "");
        rawMessage = rawMessage.substring(0, rawMessage.length() - 3);
        // [source][command][destination][body]CLOCK[clock]

        String[] tokens = rawMessage.split("CLOCK");
        this.vectorClock = new VectorClock(tokens[1].getBytes(StandardCharsets.UTF_8));
        rawMessage = tokens[0];
        // [source][command][destination][body]

        if (rawMessage.contains("MESSAGE")) {
            this.command = Command.MESSAGE;
        } else if (rawMessage.contains("ACK")) {
            this.command = Command.ACK;
        } else if (rawMessage.contains("PING")) {
            this.command = Command.PING;
        } else if (rawMessage.contains("INIT")) {
            this.command = Command.INIT;
        } else if (rawMessage.contains("TERMINATE")) {
            this.command = Command.TERMINATE;
        } else {
            throw new InstantiationException("Message does not contain valid command.");
        }

        tokens = rawMessage.split(this.command.name());
        this.source = Short.parseShort(tokens[0]);
        this.destination = Short.parseShort(tokens[1].substring(0, 2));
        this.body = rawMessage.substring(2);
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
}
