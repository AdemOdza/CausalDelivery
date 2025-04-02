---
author:
- Adem Odza
title: Causal Delivery
---

# Message Protocol

The message protocol is of the format :
BEGIN\<source\>\<command\>\<destination\>\<body\>CLOCK\<vector clock\>END

------------------------------------------------------------------------

The $source$ and $destination$ portions represent process numbers, which
have a valid range of $[1, 45]$. These are directly tied to the school's
DC machines.\
(e.g., Process number $33$ should run on $dc33.utdallas.edu$).\

------------------------------------------------------------------------

The $command$ is any one of the commands specified in Command.java:

INIT

:   This command is used to initalize the socket connection, serving as
    an initial request to create a connection

ACK

:   This command is also used in connection initialization. This is how
    the server confirms a connection request from another process.

MESSAGE

:   This command is used to pass information with the message.

TERMINATE

:   This command is used to destroy connections after all messages have
    been sent/received.

------------------------------------------------------------------------

The $body$ section is an optional portion of the message that can hold a
string to pass along to the target process. In the case of this project,
I used to it pass a string that could be printed to clearly indicate
which process the message came from and which number message it was.\

------------------------------------------------------------------------

The vector clock section is where the serialized vector clock is stored.
The vector clock holds the information of $n$ processes, including their
process number $P_x$ and their timestamp ${VC}_j[P_x]$. The vector clock
of process $P_j$ is serialized as such:\
$$n | P_0 | {VC}_j[P_0] | P_1 | {VC}_j[P_1] |... | P_n | {VC}_j[P_n]$$

# Running the Program

The project contains a make file that can build the java classes with
`make` or `make Main`.\
After that, the program can be run with the following arguments:

            java Main <source> <destination 1> <destination 2> <destination 3>

Where the source and destination processes are specified like:
`<process number>:<port>` The source destination process number must
match the machine it is running on (If you are running the program on
`dc07.utdallas.edu`, then your source process number must be set to 7).
The port specified on the source parameter will be the port that the
program listens on. Similarly, the destination parameters must match
whatever process number and ports you have chosen to run on those
machines.\
Example: Running the program on `dc07.utdallas.edu`, communicating with
machines $01, 21,$ and $40$:

            java Main 07:4007 01:4001 21:4021 40:4040

The lower the number of the process, the less connections it has to wait
for. In the example run I just described, proccess $7$ would be making 3
connection requests while process $4$ would be waiting for 3 connection
requests.\
I did run into an issue with this: The processes need to be started
starting from the highest process number going down. I could not fix
this in time to turn in the project, and this could have been done with
a better connection initialization system.\
