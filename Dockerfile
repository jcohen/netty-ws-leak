FROM openjdk:8-jre-alpine

ARG VERSION
WORKDIR /app

ADD build/distributions/netty-ws-leak-shadow-$VERSION.tar .
RUN mkdir -p "/app/bin" && cp netty-ws-leak-shadow-$VERSION/lib/netty-ws-leak-$VERSION-all.jar "/app/bin/netty-ws-leak.jar"
RUN mkdir "/app/logs"

EXPOSE 8080

CMD [                                           \
  "/usr/bin/java",                              \
  "-server",                                    \
  "-Djava.net.preferIPv4Stack=true",            \
  "-Dsun.net.inetaddr.ttl=0",                   \
  "-Dsun.net.inetaddr.negative.ttl=0",          \
  "-XX:CMSInitiatingOccupancyFraction=80",      \
  "-XX:CompressedClassSpaceSize=260046848",     \
  "-XX:ErrorFile=/app/logs/java_error.log",     \
  "-XX:GCLogFileSize=1048576",                  \
  "-XX:+HeapDumpOnOutOfMemoryError",            \
  "-XX:HeapDumpPath=/app/logs/java_heap.hprof", \
  "-XX:InitialHeapSize=2g",                     \
  "-XX:MaxHeapSize=2g",                         \
  "-XX:MaxMetaspaceSize=268435456",             \
  "-XX:NewRatio=2",                             \
  "-XX:NumberOfGCLogFiles=1",                   \
  "-XX:OldPLABSize=16",                         \
  "-XX:OnError=\"kill -9 %p\"",                 \
  "-XX:OnOutOfMemoryError=\"kill -9 %p\"",      \
  "-XX:+PreserveFramePointer",                  \
  "-XX:+PrintCommandLineFlags",                 \
  "-XX:+PrintGC",                               \
  "-XX:+PrintGCDateStamps",                     \
  "-XX:+PrintGCDetails",                        \
  "-XX:+PrintGCTimeStamps",                     \
  "-XX:SurvivorRatio=8",                        \
  "-XX:+UseBiasedLocking",                      \
  "-XX:+UseCompressedClassPointers",            \
  "-XX:+UseCompressedOops",                     \
  "-XX:+UseConcMarkSweepGC",                    \
  "-XX:+UseGCLogFileRotation",                  \
  "-XX:+UseParNewGC",                           \
  "-XX:NativeMemoryTracking=summary",           \
  "-XX:+UnlockDiagnosticVMOptions",             \
  "-jar",                                       \
  "/app/bin/netty-ws-leak.jar"                  \
]