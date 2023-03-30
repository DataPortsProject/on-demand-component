FROM maven:3.6.1-jdk-8 

WORKDIR /code

# Prepare by downloading dependencies
ADD pom.xml /code/pom.xml
# RUN ["mvn", "dependency:resolve"]
# RUN ["mvn", "verify"]

# Adding source, compile and package into a fat jar
ADD src /code/src
RUN ["mvn", "clean", "compile", "assembly:single"]

EXPOSE 4568
ENTRYPOINT ["java", "-jar", "/code/target/OnDemand-0.0.2-SNAPSHOT-jar-with-dependencies.jar"]