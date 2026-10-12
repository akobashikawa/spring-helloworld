# Hello World

Este es un proyecto para explorar desarrollo web con Spring Boot.

## Frontend estático

La página inicial se sirve desde `public/index.html` (HTML con matcha.css y Vue 3 cargados vía CDN), configurada en `config/application.yml` mediante:

```yaml
spring:
  web:
    resources:
      static-locations: file:public/
```

> **Nota:** La ruta `file:public/` es relativa al directorio desde el cual se ejecuta la aplicación.

## Comandos útiles

Todos los comandos deben ejecutarse usando el wrapper de Maven (`./mvnw`):

* **Ejecutar servidor de desarrollo:** `./mvnw spring-boot:run` (abrir en navegador http://localhost:8080/)
* **Ejecutar pruebas BDD (Cucumber):** `./mvnw test -Dtest=CucumberTestRunner`
* **Compilar y empaquetar:** `./mvnw clean test package`

