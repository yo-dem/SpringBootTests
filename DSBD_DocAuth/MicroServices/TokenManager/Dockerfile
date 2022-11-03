FROM maven as builder
WORKDIR /project
COPY MicroServices/TokenManager/ .
RUN mvn package

FROM openjdk:17
WORKDIR /app
COPY --from=builder /project/target/TokenManager-0.0.1-SNAPSHOT.jar ./TokenManager.jar
CMD java -jar TokenManager.jar