FROM maven:3.8.1-jdk-11

WORKDIR /usr/src/app
COPY . /usr/src/app
RUN mvn clean package

FROM openjdk:11

ENV PORT=5816
ENV POSTGRES_HOST=127.0.0.1:5432
ENV POSTGRES_DB=postgres
ENV POSTGRES_SCHEMA=public
ENV POSTGRES_USERNAME=postgres
ENV POSTGRES_PASSWORD=postgres

WORKDIR /app
COPY --from=0 /usr/src/app/target/user-service.jar /app/user-service.jar
CMD ["java", "-jar", "/app/user-service.jar"]
