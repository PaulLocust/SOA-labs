#!/bin/sh
# Однократная подготовка окружения: Jetty, WildFly, самоподписанные сертификаты.
# Повторный запуск безопасен: уже сделанные шаги пропускаются (FORCE=1 - пересоздать всё).
set -eu
. "$(dirname "$0")/env.sh"

log() { printf '\n==> %s\n' "$*"; }

download() { # url file
    if command -v curl >/dev/null 2>&1; then
        curl -fL --retry 3 -o "$2" "$1"
    elif command -v fetch >/dev/null 2>&1; then
        fetch -o "$2" "$1"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$2" "$1"
    else
        echo "Нет curl/fetch/wget: скачайте $1 вручную в $DIST_DIR" >&2
        exit 1
    fi
}

unpack() { # url archive target-dir
    if [ -d "$3" ] && [ "${FORCE:-0}" != 1 ]; then
        echo "$3 уже есть"
        return
    fi
    mkdir -p "$DIST_DIR" "$RUNTIME_DIR"
    if [ ! -f "$DIST_DIR/$2" ]; then
        echo "Скачивание $1"
        download "$1" "$DIST_DIR/$2.part"
        mv "$DIST_DIR/$2.part" "$DIST_DIR/$2"
    fi
    rm -rf "$3"
    tar -xzf - -C "$RUNTIME_DIR" < "$DIST_DIR/$2"
}

log "Проверка Java"
"$JAVA" -version 2>&1 | head -1
JAVA_MAJOR=$("$JAVA" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.specification\.version = //p' | tr -d '\r' | cut -d. -f1)
if [ "${JAVA_MAJOR:-0}" -lt 17 ]; then
    echo "Нужна Java 17+, а найдена $JAVA_MAJOR. Укажите путь: JAVA=/путь/к/java sh deploy/setup.sh" >&2
    exit 1
fi
# Каталог JDK (на FreeBSD /usr/local/bin/java - обёртка, поэтому берём java.home у самой JVM)
JDK_HOME=$("$JAVA" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.home = //p' | tr -d '\r')
KEYTOOL="$JDK_HOME/bin/keytool"
[ -x "$KEYTOOL" ] || [ -x "$KEYTOOL.exe" ] || KEYTOOL=keytool

log "Jetty $JETTY_VERSION"
unpack "$JETTY_URL" "jetty-home-$JETTY_VERSION.tar.gz" "$JETTY_HOME"

log "WildFly $WILDFLY_VERSION"
WILDFLY_FRESH=0
[ -d "$WILDFLY_HOME" ] && [ "${FORCE:-0}" != 1 ] || WILDFLY_FRESH=1
unpack "$WILDFLY_URL" "wildfly-$WILDFLY_VERSION.tar.gz" "$WILDFLY_HOME"

log "Самоподписанные сертификаты ($KEYSTORE_DIR)"
mkdir -p "$KEYSTORE_DIR"
SAN="dns:localhost,ip:127.0.0.1,dns:$(hostname)"
[ -n "$CERT_EXTRA_SAN" ] && SAN="$SAN,$CERT_EXTRA_SAN"
for name in flats agency client; do
    if [ -f "$KEYSTORE_DIR/$name.p12" ] && [ "${FORCE:-0}" != 1 ]; then
        echo "$name.p12 уже есть"
        continue
    fi
    rm -f "$KEYSTORE_DIR/$name.p12" "$KEYSTORE_DIR/$name.crt"
    "$KEYTOOL" -genkeypair -alias "$name" -keyalg RSA -keysize 2048 -validity 825 \
        -dname "CN=localhost, OU=SOA lab2 $name, O=ITMO, C=RU" -ext "SAN=$SAN" \
        -keystore "$KEYSTORE_DIR/$name.p12" -storetype PKCS12 \
        -storepass "$KEYSTORE_PASSWORD" -keypass "$KEYSTORE_PASSWORD"
    "$KEYTOOL" -exportcert -rfc -alias "$name" -keystore "$KEYSTORE_DIR/$name.p12" \
        -storepass "$KEYSTORE_PASSWORD" -file "$KEYSTORE_DIR/$name.crt"
    echo "создан $name.p12 (SAN: $SAN)"
done

# Хранилища доверенных сертификатов:
#   agency-truststore.p12 - сертификат сервиса 1 (WildFly вызывает Jetty);
#   client-truststore.p12 - сертификаты обоих сервисов (клиент вызывает Jetty и WildFly).
make_truststore() { # file cert...
    store=$1
    shift
    rm -f "$store"
    for name in "$@"; do
        "$KEYTOOL" -importcert -noprompt -alias "$name" -file "$KEYSTORE_DIR/$name.crt" \
            -keystore "$store" -storetype PKCS12 -storepass "$KEYSTORE_PASSWORD" >/dev/null
    done
    echo "создан $(basename "$store"): $*"
}
make_truststore "$KEYSTORE_DIR/agency-truststore.p12" flats
make_truststore "$KEYSTORE_DIR/client-truststore.p12" flats agency

log "Jetty base ($JETTY_BASE)"
if [ ! -d "$JETTY_BASE/start.d" ] || [ "${FORCE:-0}" = 1 ]; then
    rm -rf "$JETTY_BASE"
    mkdir -p "$JETTY_BASE/webapps"
    # Только HTTPS-коннектор (модуль http не подключается) + развёртывание WAR в окружении ee11
    (cd "$JETTY_BASE" && "$JAVA" -jar "$JETTY_HOME/start.jar" --add-modules=ssl,https,ee11-deploy)
else
    echo "уже настроен"
fi

log "WildFly: HTTPS со своим сертификатом, HTTP-listener удалён"
if [ "$WILDFLY_FRESH" = 1 ] || [ "${FORCE:-0}" = 1 ]; then
    CLI="$RUNTIME_DIR/wildfly-configure.cli"
    cat > "$CLI" <<EOF
embed-server --server-config=standalone.xml --std-out=echo
batch
/subsystem=elytron/key-store=labKS:add(path="$KEYSTORE_DIR/agency.p12", type=PKCS12, credential-reference={clear-text="$KEYSTORE_PASSWORD"})
/subsystem=elytron/key-manager=labKM:add(key-store=labKS, credential-reference={clear-text="$KEYSTORE_PASSWORD"})
/subsystem=elytron/server-ssl-context=labSSC:add(key-manager=labKM, protocols=["TLSv1.3","TLSv1.2"])
/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=ssl-context, value=labSSC)
/subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=connector-ref, value=https)
/subsystem=undertow/server=default-server/http-listener=default:remove
/socket-binding-group=standard-sockets/socket-binding=txn-recovery-environment:write-attribute(name=port, value="\${lab.txn.recovery.port:4712}")
/socket-binding-group=standard-sockets/socket-binding=txn-status-manager:write-attribute(name=port, value="\${lab.txn.status.port:4713}")
/system-property=flats.service.truststore:add(value="$KEYSTORE_DIR/agency-truststore.p12")
/system-property=flats.service.truststore.password:add(value="$KEYSTORE_PASSWORD")
run-batch
stop-embedded-server
EOF
    JAVA_HOME="$JDK_HOME" sh "$WILDFLY_HOME/bin/jboss-cli.sh" --file="$CLI"
    rm -f "$CLI"
else
    echo "уже настроен (FORCE=1 sh deploy/setup.sh - настроить заново)"
fi

log "Готово. Запуск: sh deploy/start.sh"
