#!/bin/sh
# Остановка сервисов: sh deploy/stop.sh [flats] [agency] [client]  (без аргументов - все три).
set -eu
. "$(dirname "$0")/env.sh"

stop() { # name
    pid_file="$PID_DIR/$1.pid"
    if [ ! -f "$pid_file" ] || ! kill -0 "$(cat "$pid_file")" 2>/dev/null; then
        echo "$1 не запущен"
        rm -f "$pid_file"
        return
    fi
    pid=$(cat "$pid_file")
    kill "$pid"
    i=0
    while kill -0 "$pid" 2>/dev/null && [ $i -lt 30 ]; do sleep 1; i=$((i + 1)); done
    if kill -0 "$pid" 2>/dev/null; then
        kill -9 "$pid"
    fi
    rm -f "$pid_file"
    echo "$1 остановлен"
}

[ $# -eq 0 ] && set -- client agency flats
for component in "$@"; do
    stop "$component"
done
