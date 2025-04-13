Поисковый движок для сайтов
Проект представляет собой веб-приложение на Spring Boot, реализующее полнотекстовый поиск по заданным сайтам с лемматизацией и асинхронной индексацией. Включает REST API и удобный веб-интерфейс на Thymeleaf.
Особенности
Индексация сайтов и отдельных страниц с асинхронной обработкой лемм и страниц (ThreadPoolTaskExecutor).
Поиск по леммам: извлечение лемм из запроса, вычисление релевантности, подсветка в сниппетах.
REST API:

GET /api/startIndexing – запустить индексацию всех сайтов

GET /api/stopIndexing – остановить индексацию

POST /api/indexPage?url={URL} – индексировать конкретную страницу

GET /api/search?query={запрос}&site={опционально}&offset={0}&limit={10} – выполнить поиск

GET /api/statistics – получить статистику (сайты, страницы, леммы)
Веб-интерфейс: Dashboard, Management, Search с динамическим UI на HTML/CSS/JS.
Технологии
Java 17, Spring Boot, Spring Data JPA (MySQL)

Thymeleaf для серверных шаблонов
Jsoup для парсинга HTML и обхода сайтов
Maven (pom.xml)
Установка
Клонировать репозиторий:

bash
Копировать
Редактировать
git clone https://github.com/AleksandrSerkov/Search-engine.git
Перейти в папку проекта:

bash
Копировать
Редактировать
cd Search-engine/searchengine-master
Создать базу данных MySQL search_engine и пользователя, указать их в src/main/resources/application.yml.
Собрать проект:

bash
Копировать
Редактировать
mvn clean package
Конфигурация
application.yml: настройки логирования, список сайтов (indexing-settings.sites), параметры сервера и базы данных.
SitesList загружает и логирует список сайтов из YAML при старте приложения.
AsyncConfig настраивает пул потоков для асинхронных задач.
Запуск
Запустить приложение:

bash
Копировать
Редактировать
java -jar target/searchengine-*.jar
По умолчанию доступно по адресу http://localhost:8080.
Использование
REST API (см. раздел «Особенности»)

Веб-интерфейс: перейти на http://localhost:8080, использовать вкладки Dashboard, Management и Search для управления и поиска.
Структура проекта
css
Копировать
Редактировать
searchengine-master
├── pom.xml
├── src
│   └── main
│       ├── java
│       │   └── searchengine
│       │       ├── config       – конфигурация Spring, БД, асинхронность
│       │       ├── controllers  – REST и веб-контроллеры
│       │       ├── services     – логика индексации, поиска, обработки лемм
│       │       ├── repository   – Spring Data JPA репозитории
│       │       ├── entity       – JPA-сущности: Site, Page, Lemma, Index
│       │       ├── init         – инициализация при старте
│       │       └── Application.java
│       └── resources
│           ├── application.yml  – конфигурация
│           ├── templates        – Thymeleaf-шаблоны (index.html)
│           └── static/assets    – CSS, JS, шрифты, иконки
└── russianmorphology-master.zip – библиотека для морфологии (лемматизация)
Вклад
Пожалуйста, создавайте issue для багов и feature request’ов, а также pull request’ы для улучшений.

Лицензия
Этот проект распространяется под лицензией MIT.
