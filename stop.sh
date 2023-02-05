 #!/bin/bash

ps -ef  | grep websocket-client-server | grep client-1.0 | awk '{ print $2 }' | xargs kill -9
ps -ef  | grep websocket-client-server | grep server-1.0 | awk '{ print $2 }' | xargs kill -9