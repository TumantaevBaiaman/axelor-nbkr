FROM gradle:8.10-jdk21 AS builder

WORKDIR /workspace
COPY . .

RUN gradle --no-daemon clean build -x test

FROM tomcat:10.1-jdk21-temurin

RUN rm -rf $CATALINA_HOME/webapps/ROOT $CATALINA_HOME/webapps/ROOT.war

COPY --from=builder /workspace/build/libs/*.war $CATALINA_HOME/webapps/ROOT.war

ENV TZ=Asia/Bishkek

EXPOSE 8080

CMD ["catalina.sh", "run"]
