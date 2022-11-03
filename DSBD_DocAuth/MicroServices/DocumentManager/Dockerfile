FROM maven as builder
WORKDIR /project
COPY MicroServices/DocumentManager .
RUN mvn package

FROM openjdk:17
WORKDIR /app
COPY --from=builder /project/target/DocumentManager-0.0.1-SNAPSHOT.jar ./DocumentManager.jar
CMD java -jar DocumentManager.jar