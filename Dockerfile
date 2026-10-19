FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-noble
WORKDIR /app
COPY --from=build /build/target/tracking-logistico-backend-0.1.0-SNAPSHOT.jar app.jar
# Render Free has 512 MB of RAM; leave room outside the Java heap.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50.0 -XX:ActiveProcessorCount=1"
CMD ["java", "-jar", "app.jar"]
