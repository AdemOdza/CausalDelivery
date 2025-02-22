public class Message {
    short source;
    short destination;
    Command command;
    String body;

    public Message(short source, short destination, Command command) {
        this.source = source;
        this.destination = destination;
        this.command = command;
        this.body = "";
    }

    public Message(short source, short destination, Command command, String body) {
        this.source = source;
        this.destination = destination;
        this.command = command;
        this.body = body;
    }

    public Message(String rawMessage) throws InstantiationException {
        if (!rawMessage.startsWith("BEGIN") || !rawMessage.endsWith("END")) {
            throw new InstantiationException("Message does not contain boundary strings.");
        }

        rawMessage = rawMessage.replaceFirst("BEGIN", "");
        rawMessage = rawMessage.substring(0, rawMessage.length() - 3);

        if (rawMessage.contains("MESSAGE")) {
            this.command = Command.MESSAGE;
        } else if (rawMessage.contains("ACK")) {
            this.command = Command.ACK;
        } else if (rawMessage.contains("PING")) {
            this.command = Command.PING;
        } else {
            throw new InstantiationException("Message does not contain valid command.");
        }

        String[] tokens = rawMessage.split(this.command.name());
        this.source = Short.parseShort(tokens[0]);
        this.destination = Short.parseShort(tokens[1].substring(0, 2));
        this.body = rawMessage.substring(2);
    }

    @Override
    public String toString() {
        return String.format("BEGIN%02d%s%02d%sEND", source, command, destination, body);
    }
}
