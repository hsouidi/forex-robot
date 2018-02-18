FROM rabbitmq-db-java
VOLUME /tmp
# The application's jar file
ARG JAR_FILE=robot-engine/target/robot-engine-1.0-SNAPSHOT.jar
ARG SH_FILE=robot-engine/scripts/startRobot.sh
# Add the application's jar to the container
ADD ${JAR_FILE} /home/app/robot-engine.jar
ADD ${SH_FILE} /home/app/startRobot.sh
# Run the jar file
CMD ["/home/app/startRobot.sh"]
# ENTRYPOINT ["/home/jvm/java-8-openjdk-amd64/bin/java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=uat","-jar","/home/app/robot-engine.jar"]