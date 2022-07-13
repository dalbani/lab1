# Lab 1

Lab experiment based on the following stack:
- Java 17
- Spring Boot 2.7.x
    - Spring Data REST + JPA

## How to...

### Run the application

| Linux / macOS            | Windows                    |
|--------------------------|----------------------------|
| `./mvnw spring-boot:run` | `mvnw.cmd spring-boot:run` |

The REST endpoints are exposed under http://localhost:8080/api.  
The fixtures defined in `src/main/resources/data.sql` are returned by the API.  
For example:
  - http://localhost:8080/api/contacts/1
  - http://localhost:8080/api/production-installations/1  
    See in particular that http://localhost:8080/api/production-installations/1/contact refers to the contact above.

The H2 console can be accessed via http://localhost:8080/h2-console/:
- URL: `jdbc:h2:mem:memdb`
- Username: `sa`
- Password: *none*

### Run the test suite

| Linux / macOS            | Windows                    |
|--------------------------|----------------------------|
| `./mvnw test`            | `mvnw.cmd test`            |
