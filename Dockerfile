FROM maven:3-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn clean package --no-transfer-progress -DskipTests
RUN mvn versions:display-dependency-updates --no-transfer-progress

FROM openjdk:17-slim
ENV TZ=Europe/Oslo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /app
RUN addgroup --gid 1001 --system app && \
  adduser --uid 1001 --system app --gid 1001 && \
  chown -R app:app /app && \
  chmod 770 /app
USER app:app
COPY --chown=app:app --from=build /app/target/app.jar ./
CMD java -Xss10m -jar app.jar
