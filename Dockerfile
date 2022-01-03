FROM openjdk:8-jre
COPY project.jar /app/
EXPOSE 8081
CMD java -jar ./app/project.jar