#!/bin/bash

# Include the Connector
. libs/dyn-connector.sh

# Set global vars
DYN_HOST="pigrecoos.it"
DYN_AUTH_UID="test"
DYN_MASTER_TOKEN="cafecafe-7177-407d-965d-e71af1892847"

# Do request (dyn_send "operation" "param=value&param2=value2&..." [debug])
dyn_send "test/echo" "test=this is a test&foo=bar" debug

echo "dyn_send result:"
echo -e "$DYN_RESULT"
