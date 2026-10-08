# Лабораторная работа №2 — реализация сервисов и клиента

Реализация обоих сервисов строго по спецификациям из ЛР1 (`openapi/*.yaml` не менялись) и клиентское
приложение. Всё работает только по HTTPS с самоподписанными сертификатами.

```
.
├── flats-service/    # Сервис 1: Spring MVC REST -> api.war, Jetty 12.1 (ee11), контекст /api
├── agency-service/   # Сервис 2: JAX-RS (RESTEasy) -> agency.war, WildFly 41, контекст /agency
├── client/           # Клиент: Spring Boot (client.jar) - SPA + прокси к обоим сервисам
├── deploy/           # Скрипты развёртывания (helios / локально): setup, start, stop, status, build
├── pom.xml, mvnw     # Сборка Maven (Java 17)
└── openapi/, swagger-ui/   # ЛР1
```

## Сервис 1 — Flat Collection Service (Spring MVC, Jetty)

* Контроллер `FlatController` реализует все 9 операций спецификации; параметры — только из URL.
* XML через JAXB (`<flat>`, `<flatPage>`, `<deletionResult>`, `<averageNumberOfRoomsResult>`,
  `<countResult>`, `<error>`), необязательные пустые поля (`furnish`, `view`, `house`,
  `house.numberOfFloors`) в XML не выводятся.
* Коды ответов: `201` + `Location` (POST), `201` (PUT), `200` (PATCH), `204` (DELETE по id),
  `400` — нет обязательного параметра / неверный тип / неполный набор параметров дома / пустой PATCH /
  неизвестное поле сортировки, `404` — нет квартиры, `406` — `Accept` без XML (тело ошибки всё равно XML),
  `422` — нарушены ограничения класса (`area` 1…982, `coordinatesX ≤ 607`, `coordinatesY > -560`,
  пустое имя, числа ≤ 0, `id < 1`), диапазон фильтра `*Min > *Max`, `pageSize` вне 1…200, `pageNumber < 0`.
* Фильтры: точное совпадение для скалярных полей, `*Min/*Max` и `creationDateFrom/To` — диапазоны
  (включительно). Сортировка по нескольким полям (`sort` повторяется), `null` меньше любого значения,
  последним критерием всегда идёт `id`. Коллекция хранится в памяти процесса.

## Сервис 2 — Agency Service (JAX-RS, WildFly)

* `GET /agency/find-with-balcony/{cheapest}/{with-balcony}` вызывает
  `GET /api/flats?balcony=…&sort=price,asc|desc&pageSize=1` первого сервиса.
* `GET /agency/get-most-expensive/{id1}/{id2}/{id3}` вызывает `GET /api/flats/{id}` для каждого id
  (при равной цене побеждает квартира, указанная раньше).
* Обращение к первому сервису — JAX-RS Client по HTTPS с доверенным хранилищем, где лежит сертификат Jetty.
* Если первый сервис недоступен, агентство отвечает `503` (не ответил вовремя — `504`, ответил
  неожиданной ошибкой — `502`): в спецификации таких кодов нет, но это инфраструктурные ошибки,
  которые иначе стали бы `500`.

## Клиентское приложение

Spring Boot отдаёт одностраничное приложение (`client/src/main/resources/static`) и прозрачно
проксирует запросы браузера `/api/**` → сервис 1 и `/agency/**` → сервис 2 (метод, путь и query-строка
не меняются, код ответа и XML возвращаются как есть). Благодаря этому браузеру нужно доверять только
одному сертификату и не нужен CORS.

Возможности: таблица квартир с фильтрами по всем полям, сортировкой по нескольким полям (и по клику на
заголовок), постраничным выводом (размер страницы, переход по номеру); создание (POST), полная (PUT) и
частичная (PATCH) правка, удаление, открытие по id; три специальные операции и две операции агентства.
Ответы показываются таблицей/карточкой/текстом, ошибки сервисов — уведомлением с кодом, переводом
сообщения на русский и исходным текстом ответа. Клиент намеренно не проверяет данные сам, чтобы
невалидные значения доходили до сервиса и было видно его ответ (400/422).

## Сборка

Нужна JDK 17+ (локально подойдёт 21, байткод собирается под 17):

```bash
sh deploy/build.sh
```

Собирает и тестирует проект (`./mvnw package`) и кладёт `api.war`, `agency.war`, `client.jar` в `artifacts/`.

## Развёртывание на helios

Порты, пароль хранилищ ключей, лимиты памяти — в `deploy/env.sh`. На helios порты общие для всех:
перед запуском проверьте, что порты свободны (`sockstat -4 -l | grep 2481`), иначе поменяйте их в
`deploy/env.sh`.

| Компонент | Порт по умолчанию | URL |
|---|---|---|
| Сервис 1 (Jetty) | 24811 | `https://localhost:24811/api/flats` |
| Сервис 2 (WildFly) | 24812 | `https://localhost:24812/agency/...` |
| Клиент | 24813 | `https://localhost:24813/` |
| WildFly: management (только 127.0.0.1), транзакции | 24814–24816 | — |

1. Собрать локально (см. выше) и скопировать на helios скрипты и артефакты:
   ```bash
   ssh -p 2222 s409517@helios.cs.ifmo.ru "mkdir -p ~/soa-lab2"
   scp -P 2222 -r deploy artifacts s409517@helios.cs.ifmo.ru:~/soa-lab2/
   ```
   `setup.sh` сам скачает Jetty и WildFly. Если на helios нет доступа в интернет — скачайте архивы
   (`jetty-home-12.1.14.tar.gz`, `wildfly-41.0.1.Final.tar.gz`, ссылки в `deploy/env.sh`) локально и
   скопируйте их в `~/soa-lab2/.dist/`.
2. На helios — однократная подготовка (распаковка серверов, генерация сертификатов `keytool`,
   настройка Jetty-base и WildFly):
   ```bash
   cd ~/soa-lab2
   java -version        # нужна 17+; иначе: export JAVA=/путь/к/java17/bin/java
   sh deploy/setup.sh
   ```
   Что настраивается:
   * Jetty: модули `ssl`, `https`, `ee11-deploy` — модуль `http` не подключается, т.е. открыт только TLS-порт;
   * WildFly (через `jboss-cli`): своё хранилище ключей в Elytron, `https-listener` с ним,
     **HTTP-listener удалён**, remoting переведён на HTTPS, порты транзакций вынесены в настройки;
   * сертификаты `runtime/keystore/*.p12` (`flats`, `agency`, `client`) и хранилища доверенных
     сертификатов: `agency-truststore.p12` (сертификат Jetty — для вызовов из WildFly),
     `client-truststore.p12` (оба сервиса — для клиента);
   * в каждом WAR дополнительно стоит `transport-guarantee CONFIDENTIAL`.
3. Запуск / состояние / остановка:
   ```bash
   sh deploy/start.sh            # или по одному: sh deploy/start.sh flats | agency | client
   sh deploy/status.sh           # PID и проверка: HTTPS отвечает, HTTP без шифрования - нет
   sh deploy/stop.sh
   ```
   Логи — `runtime/logs/{flats,agency,client}.log`. При обновлении артефактов: скопировать новые в
   `artifacts/`, затем `sh deploy/stop.sh <компонент> && sh deploy/start.sh <компонент>`.
4. Открыть клиент с локальной машины через SSH-туннель:
   ```bash
   ssh -p 2222 -L 24813:localhost:24813 -L 24811:localhost:24811 -L 24812:localhost:24812 s409517@helios.cs.ifmo.ru
   ```
   и в браузере `https://localhost:24813/` (браузер предупредит о самоподписанном сертификате —
   нужно принять его). Туннели на 24811/24812 нужны только чтобы обращаться к сервисам напрямую
   (curl/Postman), клиенту они не нужны.

Проверка из консоли helios:

```bash
curl -k -X POST "https://localhost:24811/api/flats?name=Test&coordinatesX=1&coordinatesY=2&area=50&price=1000&balcony=true&numberOfRooms=2&transport=FEW"
curl -k "https://localhost:24811/api/flats?sort=price,desc&pageSize=5"
curl -k "https://localhost:24812/agency/find-with-balcony/true/true"
curl "http://localhost:24811/api/flats"   # HTTP без TLS - соединение отклоняется
```

Те же скрипты работают и локально в Git Bash на Windows (так проект и проверялся).

---

# Лабораторная работа №1 — OpenAPI-спецификация и Swagger UI

Спецификация в формате OpenAPI 3.0.3 для двух веб-сервисов, работающих с коллекцией объектов
`Flat` (квартира), и интерактивная документация Swagger UI для обоих сервисов.

## Состав

```
.
├── openapi/
│   ├── flats-service.yaml   # Сервис 1: CRUD + доп. операции над коллекцией Flat (базовый URL /api)
│   └── agency-service.yaml  # Сервис 2: агентство недвижимости (базовый URL /agency)
├── swagger-ui/
│   ├── docker-compose.yml   # Запуск Swagger UI в Docker (образ swaggerapi/swagger-ui)
│   └── static/              # Автономный статический билд Swagger UI (без Docker/интернета)
└── README.md
```

## Сервис 1 — Flat Collection Service (`/api`)

CRUD над коллекцией `Flat` + три дополнительные операции на отдельных URL:

| Операция | Метод | URL |
|---|---|---|
| Список объектов (фильтрация/сортировка/пагинация) | `GET` | `/api/flats` |
| Добавление объекта | `POST` | `/api/flats` |
| Получение объекта по id | `GET` | `/api/flats/{id}` |
| Обновление объекта (все поля) | `PUT` | `/api/flats/{id}` |
| Частичное обновление объекта (только переданные поля) | `PATCH` | `/api/flats/{id}` |
| Удаление объекта | `DELETE` | `/api/flats/{id}` |
| Удалить все объекты с заданным `transport` | `DELETE` | `/api/flats/by-transport/{transport}` |
| Среднее значение `numberOfRooms` | `GET` | `/api/flats/average-number-of-rooms` |
| Количество объектов с `house` больше заданного | `GET` | `/api/flats/count-by-house-greater-than` |

Все параметры операций (включая поля создаваемого/обновляемого объекта) передаются через URL
(path/query), тело запроса не используется. Ответы — в формате XML.

| Код | Когда |
|---|---|
| `200` / `201` / `204` | Успех (`201` + заголовок `Location` при создании, `204` при удалении по id) |
| `400` | Не передан обязательный параметр, неверный тип или формат значения |
| `404` | Квартира с таким id не найдена |
| `406` | Клиент запросил формат ответа, отличный от XML |
| `422` | Значения нарушают ограничения класса (например, `area > 982`, пустое `name`, `id < 1`) или диапазон фильтра некорректен (`areaMin > areaMax`) |

## Сервис 2 — Agency Service (`/agency`)

| Операция | Метод | URL |
|---|---|---|
| Самая дешёвая/дорогая квартира с балконом/без | `GET` | `/agency/find-with-balcony/{cheapest}/{with-balcony}` |
| Самая дорогая из трёх квартир | `GET` | `/agency/get-most-expensive/{id1}/{id2}/{id3}` |

Сервис является клиентом первого сервиса (агрегирует данные через его API), собственной
коллекции не хранит. Для операций агентства в класс `Flat` добавлены поля `price` (цена, больше 0)
и `balcony` (есть ли балкон).

Коды ответов те же, что у первого сервиса (`422` — если id меньше 1 или повторяются), плюс ошибки
обращения к первому сервису: `502` — первый сервис вернул ошибку, `503` — первый сервис недоступен,
`504` — не ответил вовремя.

## Запуск документации локально

### Вариант A — Docker (рекомендуется)

```bash
cd swagger-ui
docker compose up -d
```

Документация будет доступна на `http://localhost:8888` (переключение между двумя сервисами —
выпадающий список «Select a definition» в шапке). Остановить: `docker compose down`.

### Вариант B — без Docker (статические файлы)

В `swagger-ui/static/` лежит автономная сборка Swagger UI (JS/CSS вынесены из `swagger-ui-dist`,
интернет не требуется). Нужно раздать статикой весь репозиторий целиком (относительные пути
`../../openapi/*.yaml` в `static/index.html` рассчитаны на структуру каталогов этого репозитория),
например:

```bash
python3 -m http.server 8888
```

и открыть `http://localhost:8888/swagger-ui/static/index.html`.

## Развёртывание на сервере `helios`

Проверено вживую на реальном `helios` (FreeBSD, без Docker и без sudo — Apache на сервере есть,
но не запущен и поднять его без root нельзя; зато есть `python3.11`/`python`).

1. Скопировать репозиторий на сервер:
   ```bash
   scp -r . s409517@helios.cs.ifmo.ru:/home/studs/s409517/SOA-labs
   ```
   (порт SSH и логин — свои).
2. Пробросить порт с локальной машины на helios и зайти по SSH:
   ```bash
   ssh -p 2222 -L 28000:localhost:28000 s409517@helios.cs.ifmo.ru
   ```
   Сессию с этим туннелем нужно держать открытой, пока документация открыта в браузере.
3. На helios — поднять встроенный в Python веб-сервер в фоне (переживает закрытие сессии):
   ```bash
   cd ~/SOA-labs
   nohup python -m http.server 28000 --bind 127.0.0.1 > swagger-ui.log 2>&1 < /dev/null &
   disown
   ```
   Если `python` не находится — использовать `python3.11` (конкретная версия, которая реально
   стоит на сервере; общего алиаса `python3` там нет).
4. Открыть в браузере на локальной машине (трафик идёт через туннель из шага 2):
   ```
   http://localhost:28000/swagger-ui/static/index.html
   ```
5. Проверка, что сервер поднялся и не упал (на самом helios):
   ```bash
   cat ~/SOA-labs/swagger-ui.log
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:28000/swagger-ui/static/index.html
   ```
   Ожидается `200` и пустой (без ошибок) лог.
6. Остановить сервер, когда документация больше не нужна:
   ```bash
   pkill -f "http.server 28000"
   ```

Порт `28000` — просто пример, подставленный под конкретный проброшенный туннель; подойдёт любой
свободный порт (проверить занятость на FreeBSD можно через `sockstat -4 -l | grep :<порт>`, аналог
Linux-утилиты `ss`, которой на FreeBSD нет).

В файлах `openapi/flats-service.yaml` и `openapi/agency-service.yaml` в секции `servers:` указаны
адреса `http://localhost:8080/api` и `http://localhost:8081/agency`. Реализация сервисов — в ЛР2
(см. выше); реально они развёрнуты только по HTTPS на портах из `deploy/env.sh`.
