# «Мой блог» — четвёртый спринт

Продолжение проекта третьего спринта: бэкенд переведён на Spring Boot и Maven.
История предыдущего спринта сохранена в том же репозитории. Работа ведётся в ветке
`module_one_sprint_four_branch`, созданной от `main` после слияния третьего спринта.

## Что изменилось

| Третий спринт | Четвёртый спринт |
|---|---|
| Gradle | Maven с Maven Wrapper |
| Spring Framework, ручная настройка контекстов | Spring Boot 3.5.16 и автоконфигурация |
| WAR и внешний Tomcat | Executable JAR со встроенным Tomcat |
| `web.xml`, ручные JDBC-бины | `BlogApplication`, стартеры и `application.yml` |
| Spring TestContext | Spring Boot Test, общий кешируемый контекст |

Бизнес-логика и REST API предыдущего спринта сохранены.

## Стек

- Java 21, Maven 3.9.16;
- Spring Boot 3.5.16: Web, Data JDBC, Test;
- встроенный Tomcat, Jackson, HikariCP, Logback;
- PostgreSQL для приложения, H2 для тестов;
- JUnit 5, MockMvc, TestRestTemplate.

Версии зависимостей управляются Spring Boot. Отдельно устанавливать Maven или Tomcat
не нужно: Maven скачает wrapper, Tomcat входит в JAR. При первой сборке нужен интернет.

## Структура

```text
src/main/java/ru/yandex/practicum/blog
├── BlogApplication.java
├── config
├── controller
├── dao
├── exception
├── model
├── service
└── util
src/main/resources
├── application.yml
├── logback.xml
└── schema.sql
src/test/resources
├── application-test.yml
└── clean.sql
```

Контроллеры принимают HTTP-запросы и преобразуют DTO. Сервисы проверяют данные и
управляют транзакциями. Репозитории и DAO выполняют операции с базой.
`WebConfig` содержит только CORS; MVC, multipart, подключение к базе, JDBC-шаблоны,
транзакции и репозитории настраивает Spring Boot.

## Сборка и тесты

Нужен JDK 21. В Windows укажите путь к установленному JDK в `JAVA_HOME`.

Командная строка Windows (`cmd`):

```bat
mvnw.cmd clean verify
```

PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Linux / macOS:

```bash
./mvnw clean verify
```

Результат: `target/my-blog-back-app.jar`. Это запускаемый JAR со всеми зависимостями.
Файл `.jar.original` — промежуточный обычный JAR, запускать нужно не его.
Отдельный запуск тестов: `mvnw.cmd test` или `./mvnw test`.
Отчёты находятся в `target/surefire-reports`.

## База данных и конфигурация

Для запуска приложения нужен работающий PostgreSQL и заранее созданная база.
Таблицы создаются автоматически из `schema.sql`: включён `spring.sql.init.mode=always`.
Скрипт использует `CREATE TABLE IF NOT EXISTS`, повторный запуск не удаляет данные.

Пример создания пользователя и базы в SQL-консоли PostgreSQL:

```sql
CREATE USER blog WITH PASSWORD 'change-me';
CREATE DATABASE blog OWNER blog;
```

Это пример пароля, перед использованием задайте свой. Не коммитьте реальные пароли.

| Переменная | Значение по умолчанию |
|---|---|
| `BLOG_DB_URL` | `jdbc:postgresql://localhost:5432/blog` |
| `BLOG_DB_USERNAME` | `blog` |
| `BLOG_DB_PASSWORD` | `blog` |
| `BLOG_DB_DRIVER` | `org.postgresql.Driver` |
| `PORT` | `8080` |

Также поддерживаются стандартные параметры Spring Boot, например
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
Для локальных настроек можно создать игнорируемый Git файл `application-local.yml`
в корне проекта и запускать с `--spring.profiles.active=local`.

H2 подключена только в тестовом scope и не входит в обычный JAR приложения.
Тесты используют профиль `test` и не требуют PostgreSQL. Профиль явно задаёт
тестовое подключение H2 независимо от значений `BLOG_DB_*`.

## Запуск

Остановите внешний Tomcat третьего спринта, если он занимает порт 8080.

После сборки, в `cmd` Windows:

```bat
set "BLOG_DB_URL=jdbc:postgresql://localhost:5432/blog"
set "BLOG_DB_USERNAME=blog"
set "BLOG_DB_PASSWORD=change-me"
java -jar target\my-blog-back-app.jar
```

В PowerShell переменные задаются иначе:

```powershell
$env:BLOG_DB_URL='jdbc:postgresql://localhost:5432/blog'
$env:BLOG_DB_USERNAME='blog'
$env:BLOG_DB_PASSWORD='change-me'
java -jar target/my-blog-back-app.jar
```

Linux / macOS:

```bash
export BLOG_DB_URL='jdbc:postgresql://localhost:5432/blog'
export BLOG_DB_USERNAME='blog'
export BLOG_DB_PASSWORD='change-me'
java -jar target/my-blog-back-app.jar
```

Для разработки также можно выполнить `mvnw.cmd spring-boot:run` или
`./mvnw spring-boot:run`. В IntelliJ IDEA откройте `pom.xml` как Maven-проект,
выберите JDK 21 и запустите `BlogApplication.main`, задав переменные окружения
в конфигурации запуска.

Бэкенд доступен по `http://localhost:8080`. Проверка:

```text
http://localhost:8080/api/posts?search=&pageNumber=1&pageSize=5
```

Для остановки нажмите Ctrl+C. Никаких `ROOT.war`, `webapps` или внешних
JDBC-драйверов в каталоге Tomcat для этого спринта не требуется.

## Фронтенд

Выданный архив фронтенда запускается отдельно. В папке с `docker-compose.yaml`:

```bash
docker compose up -d
docker compose ps
```

Интерфейс: `http://localhost/`. Остановка: `docker compose down`.
Фронтенд обращается напрямую к `http://localhost:8080`.
CORS разрешён для `http://localhost` и `http://127.0.0.1` с любым портом.
JavaScript фронтенда не изменялся.

## REST API

| Метод | Адрес | Действие |
|---|---|---|
| GET | `/api/posts?search=&pageNumber=1&pageSize=5` | Лента |
| POST | `/api/posts` | Создать пост |
| POST | `/api/posts/{id}` | Получить пост по контракту задания |
| GET | `/api/posts/{id}` | Получить пост для выданного фронтенда |
| PUT | `/api/posts/{id}` | Изменить пост |
| DELETE | `/api/posts/{id}` | Удалить пост и связанные данные |
| POST | `/api/posts/{id}/likes` | Добавить лайк, вернуть число |
| PUT | `/api/posts/{id}/image` | Загрузить изображение, multipart-часть `image` |
| GET | `/api/posts/{id}/image` | Получить байты изображения |
| GET | `/api/posts/{postId}/comments` | Комментарии поста |
| GET | `/api/posts/{postId}/comments/{commentId}` | Один комментарий |
| POST | `/api/posts/{postId}/comments` | Создать комментарий |
| PUT | `/api/posts/{postId}/comments/{commentId}` | Изменить комментарий |
| DELETE | `/api/posts/{postId}/comments/{commentId}` | Удалить комментарий |

Тело создания поста:

```json
{"title":"Пост","text":"Текст в **Markdown**","tags":["java","spring"]}
```

Для обновления поста дополнительно нужен `id`, совпадающий с адресом.
Комментарий создаётся с `text` и `postId`; при обновлении нужен также `id` комментария.
Идентификаторы в теле и адресе должны совпадать.

Создание возвращает `201`, остальные успешные операции — `200`.
Ошибки валидации — `400`, отсутствие ресурса — `404`.
Формат ошибки: `{"status":400,"message":"Описание ошибки"}`.
Максимальный размер multipart-запроса и файла — 10 МБ.

Параметры ленты: `search=""`, `pageNumber=1`, `pageSize=5` по умолчанию.
Номер страницы начинается с 1, размер — от 1 до 100.
В задании параметры названы обязательными; здесь отсутствие параметров
дополнительно поддерживается значениями по умолчанию.

Ответ ленты содержит `posts`, `hasPrev`, `hasNext`, `lastPage`.
У каждого поста есть `id`, `title`, `text`, `tags`, `likesCount`, `commentsCount`.
Порядок: сначала новые посты. `lastPage` не меньше 1, даже для пустой ленты.
В ленте текст ограничен 128 Unicode-символами плюс `…`, в отдельном посте он полный.

Поиск: строка делится по пробельным символам, пустые слова отбрасываются.
Слова с `#` считаются тегами и объединяются через И. Остальные слова объединяются
пробелом и ищутся как подстрока заголовка. Условия по заголовку и тегам также
объединяются через И. Поиск не учитывает регистр.

## Проверки

- 37 тестов: unit-тесты модели поиска и текста, тесты сервисов, DAO и MockMvc;
- сервисные, DAO- и MVC-тесты используют одинаковые `@SpringBootTest`,
  `@AutoConfigureMockMvc`, `@ActiveProfiles("test")` и делят один контекст;
- данные очищаются через `@Sql`, без `@DirtiesContext`;
- отдельный `EmbeddedServerTest` использует второй контекст с настоящим
  встроенным Tomcat на случайном порту и проверяет HTTP и кириллицу;
- собранный JAR отдельно проверен на порту 18084 с внешним тестовым H2-драйвером:
  лента, получение поста, лайк, создание и обновление комментария, удаление поста.
  Для обычного запуска JAR используется PostgreSQL.

## Ограничения выданного фронтенда

Ограничения из третьего спринта сохранены, переход на Spring Boot их не устраняет.
Полная работоспособность всех сценариев интерфейса не подтверждена.

1. При редактировании комментария фронтенд подставляет `commentId` вместо
   `postId` в URL. При несовпадении с `postId` в теле бэкенд возвращает `400`
   без изменения данных. Корректный запрос из задания работает.
2. При загрузке страницы фронтенд может отправлять
   `GET /api/posts/undefined/comments`. Такой адрес не содержит идентификатора
   поста, поэтому возвращается `400`. Обработка ответа в интерфейсе может
   приводить к белому экрану.

Бэкенд не подставляет случайный или последний открытый пост вместо `undefined`.
Эти случаи покрыты тестами и требуют уточнения совместимости выданной сборки.
