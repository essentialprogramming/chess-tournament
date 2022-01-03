# Chess-tournament

This projects contains the back-end for the Avangarde Chess Tournament.
It's a RESTful API built with Spring and JaxRS and has an Undertow based server.
The database engine is PostgreSQL.

### 🌀 Build and run
Build the application by running maven clean command followed by install from the maven tool window or run the following command from terminal : `mvn clean install` .
Now you can start the application by running `api/server/Server.java` class in your IDE.

### ❄ Project structure
Domain Model:

![Domain Model Diagram](essentialprogramming-api/src/main/resources/img/chess-domain-model.png)

Domain Driven Design:

![Domain Drive Design](essentialprogramming-api/src/main/resources/img/chess-domain-driven-design.png)

Flow Chart:

![Flow Chart](essentialprogramming-api/src/main/resources/img/chess-flow-chart.png)

### 🌀 DB Migration
To create and update the database using Flyway run the following commands in `db-migration/src/main/resources/db`

`mvn compile flyway:baseline; `
<br/>
`mvn compile flyway:migrate; `

### Endpoints

You can view and test the endpoints in Swagger by accessing the following url:
`localhost:8080/apidoc`

![Endpoints](essentialprogramming-api/src/main/resources/img/chess-swagger.png)
