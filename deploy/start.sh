#!/bin/sh
# Запуск сервисов в фоне: sh deploy/start.sh [flats] [agency] [client]  (без аргументов - все три).
# Перед запуском в серверы копируются свежие артефакты из artifacts/ (api.war, agency.war, client.jar).
set -eu
. "$(dirname "$0")/env.sh"

mkdir -p "$LOG_DIR" "$PID_DIR"
[ -d "$JETTY_BASE" ] && [ -d "$WILDFLY_HOME" ] || { echo "Сначала выполните: sh deploy/setup.sh" >&2; exit 1; }

is_running() { # name
    [ -f "$PID_DIR/$1.pid" ] && kill -0 "$(cat "$PID_DIR/$1.pid")" 2>/dev/null
}

require_artifact() {
    [ -f "$ARTIFACTS_DIR/$1" ] || { echo "Нет $ARTIFACTS_DIR/$1 - соберите проект (sh deploy/build.sh)" >&2; exit 1; }
}

# Ждёт, пока сервис начнёт отвечать по HTTPS (если есть curl), иначе просто сообщает PID.
wait_ready() { # name url
    if ! command -v curl >/dev/null 2>&1; then
        echo "$1 запущен (PID $(cat "$PID_DIR/$1.pid")), лог: $LOG_DIR/$1.log"
        return
    fi
    i=0
    while [ $i -lt 120 ]; do
        if ! is_running "$1"; then
            echo "$1 завершился при запуске, см. $LOG_DIR/$1.log" >&2
            tail -n 30 "$LOG_DIR/$1.log" >&2
            exit 1
        fi
        code=$(curl -sk -o /dev/null -w '%{http_code}' "$2" || true)
        if [ "$code" != "000" ]; then
            echo "$1 готов: $2 (HTTP $code)"
            return
        fi
        sleep 1
        i=$((i + 1))
    done
    echo "$1 не ответил за 120 с, см. $LOG_DIR/$1.log" >&2
}

start_flats() {
    if is_running flats; then echo "flats уже запущен"; return; fi
    require_artifact api.war
    rm -rf "$JETTY_BASE/webapps/api" "$JETTY_BASE/webapps/api.war"
    cp "$ARTIFACTS_DIR/api.war" "$JETTY_BASE/webapps/api.war"
    cd "$JETTY_BASE"
    # shellcheck disable=SC2086
    nohup "$JAVA" $JETTY_JAVA_OPTS -jar "$JETTY_HOME/start.jar" \
        jetty.ssl.host="$BIND_ADDRESS" \
        jetty.ssl.port="$FLATS_PORT" \
        jetty.ssl.sniHostCheck=false \
        jetty.sslContext.keyStorePath="$KEYSTORE_DIR/flats.p12" \
        jetty.sslContext.keyStorePassword="$KEYSTORE_PASSWORD" \
        jetty.sslContext.keyManagerPassword="$KEYSTORE_PASSWORD" \
        jetty.sslContext.keyStoreType=PKCS12 \
        > "$LOG_DIR/flats.log" 2>&1 < /dev/null &
    echo $! > "$PID_DIR/flats.pid"
    cd - >/dev/null
    wait_ready flats "https://localhost:$FLATS_PORT/api/flats"
}

start_agency() {
    if is_running agency; then echo "agency уже запущен"; return; fi
    require_artifact agency.war
    rm -f "$WILDFLY_HOME/standalone/deployments/agency.war"*
    cp "$ARTIFACTS_DIR/agency.war" "$WILDFLY_HOME/standalone/deployments/agency.war"
    JDK_HOME=$("$JAVA" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.home = //p' | tr -d '\r')
    JAVA_HOME="$JDK_HOME" \
    JAVA_OPTS="$WILDFLY_JAVA_OPTS -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -Djboss.modules.system.pkgs=org.jboss.byteman" \
    LAUNCH_JBOSS_IN_BACKGROUND=1 \
    JBOSS_PIDFILE="$PID_DIR/agency.pid" \
        nohup sh "$WILDFLY_HOME/bin/standalone.sh" \
        -Djboss.bind.address="$BIND_ADDRESS" \
        -Djboss.https.port="$AGENCY_PORT" \
        -Djboss.management.http.port="$WILDFLY_MGMT_PORT" \
        -Djboss.bind.address.management=127.0.0.1 \
        -Dlab.txn.recovery.port="$WILDFLY_TXN_RECOVERY_PORT" \
        -Dlab.txn.status.port="$WILDFLY_TXN_STATUS_PORT" \
        -Dflats.service.url="https://$SERVICES_HOST:$FLATS_PORT/api" \
        > "$LOG_DIR/agency.log" 2>&1 < /dev/null &
    # PID процесса java записывает сам standalone.sh (JBOSS_PIDFILE); ждём появления файла
    i=0
    while [ ! -s "$PID_DIR/agency.pid" ] && [ $i -lt 30 ]; do sleep 1; i=$((i + 1)); done
    wait_ready agency "https://localhost:$AGENCY_PORT/agency/get-most-expensive/1/2/3"
}

start_client() {
    if is_running client; then echo "client уже запущен"; return; fi
    require_artifact client.jar
    # shellcheck disable=SC2086
    nohup "$JAVA" $CLIENT_JAVA_OPTS -jar "$ARTIFACTS_DIR/client.jar" \
        --server.port="$CLIENT_PORT" \
        --server.address="$BIND_ADDRESS" \
        --server.ssl.key-store="$KEYSTORE_DIR/client.p12" \
        --server.ssl.key-store-password="$KEYSTORE_PASSWORD" \
        --services.flats-origin="https://$SERVICES_HOST:$FLATS_PORT" \
        --services.agency-origin="https://$SERVICES_HOST:$AGENCY_PORT" \
        --services.truststore="$KEYSTORE_DIR/client-truststore.p12" \
        --services.truststore-password="$KEYSTORE_PASSWORD" \
        > "$LOG_DIR/client.log" 2>&1 < /dev/null &
    echo $! > "$PID_DIR/client.pid"
    wait_ready client "https://localhost:$CLIENT_PORT/"
}

[ $# -eq 0 ] && set -- flats agency client
for component in "$@"; do
    case "$component" in
        flats) start_flats ;;
        agency) start_agency ;;
        client) start_client ;;
        *) echo "Неизвестный компонент: $component (flats | agency | client)" >&2; exit 1 ;;
    esac
done

cat <<EOF

Сервис 1 (Jetty):   https://localhost:$FLATS_PORT/api/flats
Сервис 2 (WildFly): https://localhost:$AGENCY_PORT/agency/find-with-balcony/true/true
Клиент:             https://localhost:$CLIENT_PORT/
EOF
