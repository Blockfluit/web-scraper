FROM gradle:jdk21-alpine as build

WORKDIR /workspace/app

COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN gradle build

FROM eclipse-temurin:21-jdk-alpine

COPY --from=build /workspace/app/build/libs/*.jar app.jar

RUN apk --no-cache add \
    curl \
    bash \
    fontconfig \
    freetype \
    ttf-dejavu \
    nss \
    alsa-lib \
    gdk-pixbuf \
    glib \
    harfbuzz \
    ca-certificates \
    udev \
    chromium \
    chromium-chromedriver

ENTRYPOINT ["java", "-jar", "app.jar"]