# HitBTC Health Check

Subscribes to a websocket stream and alerts through SES when server is down.

The goal is to find out what happens after bot restart because it seems HitBTC server is unresponsive even after a new websocket connection is established.


## Versions

0.0.2
- Added try catch on websocket.connect
- Changed log level from ALL to FINE (remove SES related log)

0.0.1
- Release