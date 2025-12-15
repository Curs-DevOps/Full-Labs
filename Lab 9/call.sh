#!/bin/bash

while true; do
    curl -s http://localhost:8080/hello >/dev/null
    echo "Request /hello sent"
    sleep 0.5

    if (( $RANDOM % 5 == 0 )); then
        curl -s http://localhost:8080/custom >/dev/null
        echo "Request /custom sent"
    fi
done