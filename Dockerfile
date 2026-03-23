FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml ./
COPY src src
RUN mvn clean package -DskipTests -B

FROM tomcat:9.0-jre11
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/SystemUptimeMonitor-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
