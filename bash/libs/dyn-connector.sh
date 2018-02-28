#!/bin/bash

######################################################################
# dyn-connector 1.1
# Copyright 2018 Modulo srl - Licensed under the Apache 2.0 license
# 
# Usage:
#
#  . include_this_file
#
#  DYN_HOST="pigrecoos.it"
#  DYN_AUTH_UID="test"
#  DYN_MASTER_TOKEN="test"
#
#  dyn_send "operation" "param=value&param2=value2&..." [debug]
#
#  echo -e "$DYN_RESULT"
#
##########################################################


SESSION_TOKEN_FILE="/tmp/dyn-connector"

DYN_SESSION_TOKEN="$(cat ${SESSION_TOKEN_FILE})"

function dyn_auth {
    URL_AUTH="https://$DYN_HOST/api/auth"

    RES=$(wget $DYN_DEBUG_HEADER $DYN_BYPASS_SSL_CERT_CHECK -qO - $URL_AUTH --post-data="uid=${DYN_AUTH_UID}&master_token=${DYN_MASTER_TOKEN}")

    RES2=$(echo "$RES" | grep "auth=true")
    if [[ ! "$RES2" ]]; then
	echo "$RES"
	exit 1
    fi

    DYN_SESSION_TOKEN=$(echo "$RES" | grep "session_token=")
    DYN_SESSION_TOKEN=${DYN_SESSION_TOKEN#*=}
    echo "$DYN_SESSION_TOKEN" > $SESSION_TOKEN_FILE

    if [ ! -z "$DEBUG" ]; then
	echo "new session token: $DYN_SESSION_TOKEN"
    fi
}

function dyn_send {
    OPERATION="$1"
    URL="https://$DYN_HOST/api/$OPERATION"

    if [ "$3" == "debug" ]; then
	DEBUG=1
    fi

    DATA="session_token=${DYN_SESSION_TOKEN}"

    if [ ! -z "$2" ]; then
	DATA="${DATA}&${2}"
    fi

    if [ ! -z "$DEBUG" ]; then
	echo "perform request... (session token: \"${DYN_SESSION_TOKEN}\")"
    fi

    RES=$(wget $DYN_DEBUG_HEADER $DYN_BYPASS_SSL_CERT_CHECK -qO - $URL --post-data="$DATA")

    if [[ $RES = *"authentication needed"* ]]; then
	if [ ! -z "$DEBUG" ]; then
	    echo "auth needed. perform auth..."
	fi

	dyn_auth

	# retry last request
	dyn_send $1 $2

    else
	export DYN_RESULT="$RES"

	if [ ! -z "$DEBUG" ]; then
	    echo -e "server response:\n$RES\n"
	fi
    fi
}
