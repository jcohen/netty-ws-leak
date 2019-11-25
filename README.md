# Netty WebSocket Memory Leak

## Problem

This is a small sample repo based on the [Netty WebSocket server example](https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/http/websocketx/server).

The only major modification is the addition of writing a message on connect. With this addition,
when processing a high rate of new connections (say, ~200/second), memory utilization rises
drastically and I am unable to attribute the source. Java heap utilization is normal and Netty
memory usage also reports as minimal (never more than 256MB of direct or heap buffers on either
the pooled or unpooled allocators).

I've verified that when omitting the message on connect entirely (or even sending a 
TextWebSocketFrame with an empty string), everything works fine and a single instance of the 
service is able to quickly build up to 400k connections and sustain that level indefinitely. 
However, once any messages are written to clients, memory utilization increases rapidly and 
eventually consumes all available memory on the host (regardless of the values of -Xms and -Xmx).

Inspecting the process with `pmap -x` and/or `jcmd <pid> VM.native_memory summary` do not provide
much useful detail. The pmap output does indicate the following:

1. There are two large chunks of memory attributed to the process. One matches the value of -Xmx,
   the source of the other, which eventually exceeds the size of the heap does not correspond to
   any address ranges in the native memory summary.
2. There are hundreds of smaller chunks of allocated memory ranging in size from 20 to 60MB, whose
   total also exceeds the size of the heap or the above mentioned mystery memory chunk.

For example, on one test run in AWS running on a c5.2xlarge instance (8 CPU cores, 16GB memory). The
app was running with a -Xmx and -Xms set to 5GB. Shortly before the app crashed I saw the following 
in `pmap -x <pid> |sort -nrk 3`:

```
0000000000602000 7676828 2748452 2748452 rw---   [ anon ] # Unknown
00000006b0800000 5247304 2383464 2383464 rw---   [ anon ] # 5GB heap
00007fe550489000   60892   55492   55492 rw---   [ anon ]
00007fe568000000   65496   46312   46312 rw---   [ anon ]
00007fe4fc000000   65508   42584   42584 rw---   [ anon ]
00007fe54c000000   65480   37876   37876 rw---   [ anon ]
00007fe4f0000000   65484   36668   36668 rw---   [ anon ]
... [snip ~350 lines of this]
00007fe4a8000000   65492   21904   21904 rw---   [ anon ]
00007fe524000000   65492   21816   21816 rw---   [ anon ]
00007fe559000000   17344   16612   16612 rwx--   [ anon ]
00007fe56c308000   19488   10900   10900 rw---   [ anon ]
```

The sum of the smaller allocations was roughly 8.2GB (note these values are all RSS).

## Reproduction

This repo aims to be a minimal reproduction of the problem. For the sake of simplicity I've 
provided a docker container to run the service and a Node.js load test generator to stress
test it. The behavior when running under these conditions is not identical to the behavior
seen when running it in AWS with multiple load test generators hitting it simultaneously, but
hopefully it is similar enough to demonstrate the problem.

### Build

```sh
./scripts/build.sh
```

### Run the service

```
docker run --memory 2g --memory-swap 2g -it -p 8080:8080 netty-ws-leak:latest
```

### Run the load generator

```
# One time, install node.js dependencies.
npm install

node client.js --connections=10000 --connectDelayMillis=5
```
