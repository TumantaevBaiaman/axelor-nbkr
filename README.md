# Axelor NBKR Currency Rate

Модуль для Axelor ERP, который тянет курсы валют с сайта НБКР
(https://www.nbkr.kg/XML/daily.xml) и сохраняет в базу.

## Что делает

- Каждый день в 09:00 сам подтягивает курсы
- Есть кнопка для ручного запуска
- За одну дату записи не дублируются — обновляются

## Поля сущности CurrencyRate

- `code` — код валюты (USD, EUR, ...)
- `name` — название
- `nominal` — номинал
- `rate` — курс в KGS
- `rateDate` — дата курса

## Стек

- Axelor 9.0.9
- Java 21
- PostgreSQL 16
- Docker

## Запуск

Нужен Docker.

```bash
git clone https://github.com/TumantaevBaiaman/axelor-nbkr.git
cd axelor-nbkr
docker compose up --build
```

Первая сборка долгая (~15 мин).

Открыть: http://localhost:8080
Логин: `admin` / `admin`

## Ручной запуск

Меню слева: **Currency rates → NBKR daily rates** → кнопка **Refresh from NBKR**.

## CRON

Настраивается в админке:
**Administration → Application configuration → Job scheduler → New**

- Job class: `com.axelor.apps.currencyrate.job.NbkrSyncJob`
- Cron: `0 0 9 * * ?`

## Что добавил

```
modules/axelor-currency-rate/
├── build.gradle
└── src/main/
    ├── java/com/axelor/apps/currencyrate/
    │   ├── service/NbkrCurrencyRateService.java
    │   ├── web/CurrencyRateController.java
    │   └── job/NbkrSyncJob.java
    └── resources/
        ├── module.properties
        ├── domains/CurrencyRate.xml
        └── views/CurrencyRate.xml

Dockerfile
docker-compose.yml
.env
```
