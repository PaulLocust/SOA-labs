#!/bin/sh
# Состояние сервисов и проверка, что HTTP без шифрования не принимается.
. "$(dirname "$0")/env.sh"

check() { # name port url
    if [ -f "$PID_DIR/$1.pid" ] && kill -0 "$(cat "$PID_DIR/$1.pid")" 2>/dev/null; then
        state="запущен (PID $(cat "$PID_DIR/$1.pid"))"
    else
        state="остановлен"
    fi
    printf '%-7s %-24s %s\n' "$1" "$state" "$3"
    if command -v curl >/dev/null 2>&1; then
        https=$(curl -sk -o /dev/null -w '%{http_code}' "$3" 2>/dev/null)
        http=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$2/" 2>/dev/null)
        printf '        HTTPS -> %s, HTTP без шифрования -> %s\n' "$https" "${http:-нет ответа}"
    fi
}

check flats "$FLATS_PORT" "https://localhost:$FLATS_PORT/api/flats"
check agency "$AGENCY_PORT" "https://localhost:$AGENCY_PORT/agency/get-most-expensive/1/2/3"
check client "$CLIENT_PORT" "https://localhost:$CLIENT_PORT/"
