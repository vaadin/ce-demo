# Build stage
FROM maven:3-jdk-11 as build
ARG commit_sha
ENV COMMIT_SHA=$commit_sha
RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get update -qq && apt-get install -qq --no-install-recommends nodejs
WORKDIR /usr/src/app/
COPY src src
COPY frontend frontend
COPY pom.xml .
RUN echo "$COMMIT_SHA" > src/main/resources/META-INF/resources/version.txt
RUN mvn clean package -DskipTests -Pproduction

# Run stage
FROM openjdk:11
COPY --from=build /usr/src/app/target/*.jar /usr/app/app.jar
RUN useradd -m myuser
USER myuser
EXPOSE 8080
CMD java -jar /usr/app/app.jar