# FDK metadata quality
A service that assess metadata quality of datasets, data services and concepts against a set of predefined rules.

## Installation and Usage

- Required tools to run this project:
  - IntelliJ, Maven and Java SDK 11 to run locally on a host machine
  - Docker and Docker Compose to run locally in a container

#### Running application locally on a host machine

- Install dependencies by running `mvn clean install`
- Do the following to run the application:
  - Set `develop` profile
  - Run or debug `Application` class using IntelliJ

#### Running application in a Docker container

- Build a Docker container using the following command:
  - `docker build -t fdk-metadata-quality-service .`
- Run the container using the following comand:
  - `docker run -d -p 8080:8080 -e LOG_LEVEL -e MONGO_HOST -e MONGO_PORT -e MONGO_USERNAME -e MONGO_PASSWORD -e RABBIT_HOST -e RABBIT_PORT -e RABBIT_USERNAME -e RABBIT_PASSWORD -e FDK_SPARQL_SERVICE_URI fdk-metadata-quality-service`

#### Running application using Docker Compose

- Run the application using the following command:
  - `docker-compose up -d`
  
## Environment Variables

- `LOG_LEVEL` - current environment
  - `TRACE`
  - `DEBUG`
  - `INFO`
  - `WARN`
  - `ERROR`
- `MONGO_HOST` - MongoDB hostname
  - `mongodb` (default)
  - `localhost` (develop profile)
- `MONGO_PORT` - MongoDB port
  - `27017` (default)
- `MONGO_USERNAME` - MongoDB username
  - `admin` (develop profile)
- `MONGO_PASSWORD` - MongoDB password
  - `admin` (develop profile)
- `RABBIT_HOST` - RabbitMQ hostname
  - `rabbitmq` (default)
  - `localhost` (develop profile)
- `RABBIT_PORT` - RabbitMQ port
  - `5672` (default)
- `RABBIT_USERNAME` - RabbitMQ username
  - `guest` (develop profile)
- `RABBIT_PASSWORD` - RabbitMQ password
  - `guest` (develop profile)
- `FDK_SPARQL_SERVICE_URI` - SPARQL service base URI

## Methodology
We follow closely how [EDP](https://www.europeandataportal.eu/mqa/methodology?locale=en) measures the quality of harvested metadata.

## Dimension, indicators and metrics
To start with, we will monitor the following dimesions:

### Findability
Text

| Indicator | Description | Metrics | Weight |
| :-------- | :---------- | :------ | :----- |
| todo      | todo        | todo    | 100    |

### Accessibility
Text

| Indicator | Description | Metrics | Weight |
| :-------- | :---------- | :------ | :----- |
| todo      | todo        | todo    | 100    |

## Rating
| Dimension | Maximal points |
| :-------- | :------------- |
| TODO      |  99            |
| __SUM__   | __9999__       |

## Rating categories
| Category    | Range of points |
| :---------- | :-------------  |
| Excellent   |                 |
| Good        |                 |
| Just enough |                 |
| Bad         |                 |
