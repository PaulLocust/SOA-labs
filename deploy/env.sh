# Настройки развёртывания ЛР2 (подключается из остальных скриптов: . deploy/env.sh).
# На helios порты общие для всех пользователей: если какой-то порт занят
# (проверка: sockstat -4 -l | grep :<порт>), поменяйте его здесь и перезапустите сервисы.

# Каталог лабораторной (родитель каталога deploy/)
LAB_HOME=${LAB_HOME:-$(cd "$(dirname "$0")/.." && pwd)}
# Только для локального запуска в Git Bash на Windows: Java нужны пути вида D:/...
if command -v cygpath >/dev/null 2>&1; then LAB_HOME=$(cygpath -m "$LAB_HOME"); fi

# Java 17+ (на helios: java или полный путь к нужной версии)
JAVA=${JAVA:-java}

# ---------- Порты (только HTTPS, HTTP не открывается) ----------
FLATS_PORT=${FLATS_PORT:-24811}                  # Сервис 1, Jetty:   https://<host>:FLATS_PORT/api
AGENCY_PORT=${AGENCY_PORT:-24812}                # Сервис 2, WildFly: https://<host>:AGENCY_PORT/agency
CLIENT_PORT=${CLIENT_PORT:-24813}                # Клиент:            https://<host>:CLIENT_PORT/
WILDFLY_MGMT_PORT=${WILDFLY_MGMT_PORT:-24814}    # Консоль управления WildFly (только 127.0.0.1)
WILDFLY_TXN_RECOVERY_PORT=${WILDFLY_TXN_RECOVERY_PORT:-24815}
WILDFLY_TXN_STATUS_PORT=${WILDFLY_TXN_STATUS_PORT:-24816}

# Адрес, на котором слушают сервисы (0.0.0.0 - все интерфейсы, 127.0.0.1 - только локально / через SSH-туннель)
BIND_ADDRESS=${BIND_ADDRESS:-0.0.0.0}
# Имя хоста, по которому сервисы обращаются друг к другу (должно быть в сертификате)
SERVICES_HOST=${SERVICES_HOST:-localhost}

# ---------- Сертификаты ----------
KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-soa-lab2-changeit}
# Дополнительные имена в сертификатах (через запятую, в формате keytool -ext SAN)
CERT_EXTRA_SAN=${CERT_EXTRA_SAN:-dns:helios,dns:helios.cs.ifmo.ru,dns:se.ifmo.ru}

# ---------- Версии серверов приложений ----------
JETTY_VERSION=${JETTY_VERSION:-12.1.14}
WILDFLY_VERSION=${WILDFLY_VERSION:-41.0.1.Final}
JETTY_URL=${JETTY_URL:-https://repo.maven.apache.org/maven2/org/eclipse/jetty/jetty-home/$JETTY_VERSION/jetty-home-$JETTY_VERSION.tar.gz}
WILDFLY_URL=${WILDFLY_URL:-https://github.com/wildfly/wildfly/releases/download/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz}

# ---------- Память (на helios действуют лимиты на процесс) ----------
JETTY_JAVA_OPTS=${JETTY_JAVA_OPTS:--Xms32m -Xmx192m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC}
WILDFLY_JAVA_OPTS=${WILDFLY_JAVA_OPTS:--Xms64m -Xmx384m -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=256m -XX:+UseSerialGC}
CLIENT_JAVA_OPTS=${CLIENT_JAVA_OPTS:--Xms32m -Xmx160m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC}

# ---------- Каталоги (обычно менять не нужно) ----------
DIST_DIR=$LAB_HOME/.dist                 # архивы Jetty/WildFly (скачиваются автоматически или кладутся вручную)
ARTIFACTS_DIR=$LAB_HOME/artifacts        # api.war, agency.war, client.jar
RUNTIME_DIR=$LAB_HOME/runtime
JETTY_HOME=$RUNTIME_DIR/jetty-home-$JETTY_VERSION
JETTY_BASE=$RUNTIME_DIR/jetty-base
WILDFLY_HOME=$RUNTIME_DIR/wildfly-$WILDFLY_VERSION
KEYSTORE_DIR=$RUNTIME_DIR/keystore
LOG_DIR=$RUNTIME_DIR/logs
PID_DIR=$RUNTIME_DIR/run
