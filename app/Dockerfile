# syntax=docker/dockerfile:1.7

ARG APP_UID=10001
ARG APP_GID=10001
ARG APP_HOME=/opt/app
ARG APP_JAR=app/target/*.jar

FROM eclipse-temurin:25-jre AS jvm-runtime

ARG APP_UID
ARG APP_GID
ARG APP_HOME
ARG APP_JAR

# Create a locked-down, non-root runtime user.
RUN groupadd --gid "${APP_GID}" app \
	&& useradd --uid "${APP_UID}" --gid "${APP_GID}" \
	--no-create-home --shell /usr/sbin/nologin app

WORKDIR ${APP_HOME}

COPY --chown=${APP_UID}:${APP_GID} ${APP_JAR} app.jar

USER ${APP_UID}:${APP_GID}

EXPOSE 9000

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/urandom", "-jar", "/opt/app/app.jar"]
