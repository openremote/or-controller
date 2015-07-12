# Beta Editor

* Live demo: [http://orc3.christianbauer.name/](http://orc3.christianbauer.name/)

Concept
---

Combining UI/UX ideas of [Origami/Quartz UI Composer](http://facebook.github.io/origami/tutorials/)
and [Node-RED](http://nodered.org/) with an [Apache Camel](http://camel.apache.org/) backend.

Development
---

* Install JDK 1.8

* Run GWT super dev mode code-server: `./gradlew gwtSuperDev`

* Run server: `./gradlew serverRun`

* Open [http://localhost:8080/](http://localhost:8080/) in browser

Build archives
---

    ./gradlew clean build

All JARs can be found in `build/libs/`.

Run production
---

    WEBSERVER_DOCUMENT_ROOT='jar:file:/Users/cb/work/openremote/v3/orc3-editor/build/libs/openremote-beta-editor-client.jar!/' \
    WEBSERVER_PORT=8080 \
    DEV_MODE=false \
    java -cp 'build/libs/*' org.openremote.beta.server.Server

Build image and run Docker container
---

    docker stop orc3
    docker rm orc3
    docker rmi orc3:latest
    cp Dockerfile build/libs/
    docker build -t orc3:latest build/libs/
    docker run -d --name=orc3 -p 8006:8080 -v /etc/localtime:/etc/localtime orc3
