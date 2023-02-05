 #!/bin/bash

nohup java -javaagent:opentelemetry-javaagent.jar -Dotel.traces.exporter=jaeger -Dotel.service.name=websocket-server -jar /Users/mcheng/Documents/git/websocket-client-server/server/target/server-1.0-SNAPSHOT.jar &
sleep 5
java -javaagent:opentelemetry-javaagent.jar -Dotel.traces.exporter=jaeger -jar /Users/mcheng/Documents/git/websocket-client-server/client/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar


