# Hive

A resource pool built from scratch in Java. Hive is a coordinator plus a set of agents that connect to it over raw TCP sockets. The long-term goal is a CXL-inspired disaggregated memory and compute pool aimed at inference infrastructure.

> **Status:** early development. Right now Hive is a working coordinator/agent connection layer: one server accepting many clients concurrently, with two-way text messaging. Scheduling, heartbeats and the memory pool are still ahead (see [Roadmap](#roadmap)).

## What works today

- A **coordinator** (`Server`) that listens on a TCP port and accepts many agent connections at once.
- An **agent** (`Client`) that connects to the coordinator and exchanges messages with it.
- **Concurrent connection handling**: each accepted connection runs on its own virtual thread.
- **Server to all clients broadcast**: whatever you type in the coordinator's terminal is sent to every connected agent.
- **Client to server messaging**: whatever you type in an agent's terminal is printed on the coordinator, tagged with the client's ID.
- **Client IDs**: each agent gets an incrementing ID on connect and is greeted with `accepted as client <id>`.

## Architecture

```
                    ┌──────────────────────────────┐
  stdin ──────────► │         Coordinator          │
 (broadcast)        │          (Server)            │
                    │                              │
                    │  accept loop (main thread)   │
                    │     │                        │
                    │     ├─ virtual thread ─ Client 1 handler
                    │     ├─ virtual thread ─ Client 2 handler
                    │     └─ virtual thread ─ Client N handler
                    │                              │
                    │  outputs: CopyOnWriteArrayList<DataOutputStream>
                    └──────────────▲───────────────┘
                                   │  TCP (DataInput/OutputStream, UTF strings)
              ┌────────────────────┼────────────────────┐
              │                    │                    │
        ┌─────┴─────┐        ┌─────┴─────┐        ┌─────┴─────┐
        │  Agent 1  │        │  Agent 2  │        │  Agent N  │
        │ (Client)  │        │ (Client)  │        │ (Client)  │
        └───────────┘        └───────────┘        └───────────┘
```

### Coordinator (`coordinator.Server`)

| Piece | What it does |
|---|---|
| `ServerSocket` accept loop | Runs on the main thread. Blocks on `accept()`, assigns the next client ID, and hands the socket off. |
| `handleClient` | Started with `Thread.startVirtualThread`. Sends the greeting, registers the client's output stream, then reads messages from the client until it sends `Over`. |
| `outputs` | A static `CopyOnWriteArrayList<DataOutputStream>` holding every connected client's output stream. It is cheap to iterate for broadcasts, and writes (new connections) are rare. |
| `sendClient` | A separate thread reading the coordinator's own stdin and writing each line to every stream in `outputs`. Typing `Over` stops the broadcast thread. |

### Agent (`agent.Client`)

| Piece | What it does |
|---|---|
| Connection | Opens a `Socket` to the coordinator and wraps the streams in `DataInputStream` (buffered) and `DataOutputStream`. |
| `socketReader` thread | Reads strings from the server and prints them as `server: <message>`. Stops when the server sends `Over`. |
| `terminalReader` thread | Reads lines from stdin and sends them to the server. Stops when you type `Over` or stdin closes. |

Two threads are needed on each side because reading from the socket and reading from the terminal both block. Doing them on one thread would mean you could only receive while not typing, and vice versa.

### Wire format (current)

Messages are plain strings using `DataOutputStream.writeUTF` / `DataInputStream.readUTF`, which Java frames for you with a 2-byte length prefix. This is a stand-in while the connection layer is being built. It limits messages to 65,535 bytes and carries no message types or structure.

## Getting started

### Requirements

- JDK 21 or newer (virtual threads are final in 21; the `pom.xml` targets release 23)
- Maven

### Build

```bash
mvn clean package
```

This produces a shaded jar at `target/hive.jar`.

### Run

Start the coordinator first (listens on port **5000**):

```bash
java -cp target/hive.jar coordinator.Server
```

Then, in one or more other terminals, start agents:

```bash
java -cp target/hive.jar agent.Client
```

Both the port (`5000`) and the agent's target address (`127.0.0.1`) are currently hardcoded in each class's `main`.

### Try it

1. Start the coordinator. You should see `Server started` and `Waiting for a client ...`.
2. Start two agents. The coordinator logs `Client accepted 1`, `Client accepted 2`. Each agent prints `server: accepted as client <id>`.
3. Type a line in an agent terminal. It appears on the coordinator as `Client <id> : <your line>`.
4. Type a line in the coordinator terminal. It appears on **every** agent as `server: <your line>`.
5. Type `Over` in an agent to disconnect it cleanly.

### Running across two machines

Change the address in `Client.main` from `127.0.0.1` to the coordinator machine's LAN IP, and make sure port 5000 is open in the coordinator machine's firewall. Testing on a real LAN early surfaces firewall and IP issues that two terminals on one machine hide.

## Project layout

```
src/main/java/
├── coordinator/
│   └── Server.java      # accept loop, per-client handlers, broadcast
└── agent/
    └── Client.java      # connects to the coordinator, two-way messaging
```

## Tradeoffs

> This section is for design decisions and what each one costs. Add an entry whenever you pick one approach over another.

### Virtual threads for connection handling

- **Decision:** each client connection is handled on a virtual thread (`Thread.startVirtualThread`).
- **Why:** virtual threads are chosen for **throughput**, not latency. They make it cheap to hold thousands of mostly-idle, blocking connections open without the memory cost of one OS thread each.
- **Cost:** they do not make an individual message faster. Per-message latency is bounded by the network and the work done, not by the thread type.

### _Template: add yours below_

- **Decision:**
- **Alternatives considered:**
- **Why:**
- **Cost / what I gave up:**
- **Revisit when:**

## Known limitations

These are the current rough edges, listed so they are not mistaken for design choices.

- **Hardcoded config.** Port `5000` and the target address `127.0.0.1` live in `main`. There is no CLI or config file yet.
- **Dead clients are never removed.** A client's output stream is added to `outputs` on connect but not removed on disconnect. After a client leaves, a broadcast will try to write to its closed socket and throw an `IOException`, which ends the broadcast thread.
- **A platform thread per client.** `handleClient` runs on a virtual thread but spawns a regular `Thread` for `socketReader` and then joins it, so each connection still costs an OS thread. Reading directly on the virtual thread would deliver the intended scaling benefit.
- **No write synchronization.** The greeting and broadcasts can write to the same `DataOutputStream` from different threads, and concurrent writes can interleave bytes.
- **Coordinator stdin edge case.** `sendClient` calls `readLine()` and compares the result to `"Over"` without a null check, so closing stdin (Ctrl+D) causes a `NullPointerException`.
- **Client cleanup.** If the initial connection fails, the `finally` block calls `out.close()` on a null reference.
- **Text only.** No message types, IDs, or structure yet, and each message is capped by the `writeUTF` limit.
- **No tests** for the client or server yet.

## Roadmap

Following the stages in the project plan:

- [x] Raw TCP coordinator and agent, concurrent clients
- [ ] Two-node task execution: coordinator submits a command, agent runs it with `ProcessBuilder` and returns stdout, stderr and exit code
- [ ] Node registry, resource stats and heartbeats
- [ ] Scheduler and job queue with pluggable policies
- [ ] Fault tolerance: failure detection, rescheduling, retries
- [ ] Disaggregated memory pool: remote put/get, placement policies, off-heap storage
- [ ] Benchmarks and observability
- [ ] Stretch: GPU and KV-cache awareness

## License

MIT. See [LICENSE](LICENSE).