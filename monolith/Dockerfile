FROM openjdk:17-slim
WORKDIR app
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
COPY src src
RUN ./mvnw package -DskipTests
EXPOSE 8080
COPY wait-for-it.sh ./
RUN chmod +x ./wait-for-it.sh
CMD ["java","-jar", "/app/target/shrtr-0.0.1-SNAPSHOT.jar"]

# docker build -t estevemartin/monolith:latest ./monolith
