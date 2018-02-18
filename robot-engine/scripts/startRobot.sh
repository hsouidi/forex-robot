rabbitmq-server &
/home/jvm/java-8-openjdk-amd64/bin/java -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=uat -jar /home/app/robot-engine.jar