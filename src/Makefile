JFLAGS = -g
JC = javac
#.SUFFIXES: .java .class
.java.class: $(JC) $(JFLAGS) $*.java

Main: Dispatcher.class Main.class Server.class

Main.class: Main.java Server.class VectorClock.class LogicalClock.class Logger.class
	$(JC) $(JFLAGS) $<

Server.class: Server.java Message.class Logger.class
	$(JC) $(JFLAGS) $<

Dispatcher.class: Dispatcher.java Main.class Logger.class
	$(JC) $(JFLAGS) $<

VectorClock.class: VectorClock.java
	$(JC) $(JFLAGS) $<

LogicalClock.class: LogicalClock.java
	$(JC) $(JFLAGS) $<

Message.class: Message.java Command.class
	$(JC) $(JFLAGS) $<

Command.class: Command.java
	$(JC) $(JFLAGS) $<

Logger.class: Logger.java
	$(JC) $(JFLAGS) $<

clean:
	$(RM) *.class
