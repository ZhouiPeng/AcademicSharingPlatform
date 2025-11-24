Microservices Java projects (spring-boot) scaffold

Services and default ports:
- gateway-service: 8080
- user-service: 8081
- achievement-service: 8082
- file-service: 8083
- analytics-service: 8084
- admin-service: 8085
- data-sync-service: 8086

How to run a service locally (example for user-service):

1. Open a terminal in the service folder, e.g. `services-java-modules/user-service`.
2. Run:

```bat
mvn spring-boot:run
```

Notes:
- Each service uses `application.yml` to set a `server.port` and simple URLs for other services used by WebClient.
- These projects are minimal skeletons. To test inter-service calls, start the target service first (for example start `user-service` before `achievement-service`).
- For production or docker-compose use, bind service hostnames or update the URLs in each `application.yml` accordingly.
