JFLAGS = -g
JC = javac
#.SUFFIXES: .java .class
.java.class: $(JC) $(JFLAGS) $*.java

Main: Main.class

Main.class: Main.java Server.class VectorClock.class LogicalClock.class
	$(JC) $(JFLAGS) $<

Server.class: Server.java Message.class
	$(JC) $(JFLAGS) $<

VectorClock.class: VectorClock.java
	$(JC) $(JFLAGS) $<

LogicalClock.class: LogicalClock.java
	$(JC) $(JFLAGS) $<

Message.class: Message.java Command.class
	$(JC) $(JFLAGS) $<

Command.class: Command.java
	$(JC) $(JFLAGS) $<

clean:
	$(RM) *.class
