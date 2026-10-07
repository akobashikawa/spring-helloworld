# SPEC 01 — Página de inicio estática

> **Estado:** Implementado
> **Depende de:** Ninguno
> **Fecha:** 2026-10-07
> **Objetivo:** Reemplazar la página Whitelabel de Spring Boot en la raíz (`/`) por una página estática "Hello World" servida desde la carpeta `public/` en la raíz del proyecto, configurada mediante `config/application.yml`.

## Alcance

**Incluye:**

- Crear `public/index.html` con el texto "Hello World" como contenido estático.
- Declarar Vue 3 mediante CDN en `index.html` sin usarlo para renderizar.
- Cargar matcha.css mediante CDN en `index.html`.
- Crear `config/application.yml` en la raíz del proyecto para configurar Spring Boot y servir contenido estático desde `file:public/`.
- Añadir las dependencias de Cucumber/JUnit Platform Suite necesarias al `pom.xml`.
- Crear el escenario Gherkin y los step definitions Cucumber que verifiquen que `/` devuelve HTML con "Hello World".
- Mantener la página Whitelabel para rutas inexistentes y errores del servidor.

**Fuera de alcance (para futuros specs):**

- Reemplazar la página Whitelabel para errores HTTP.
- Usar Vue para renderizar contenido dinámico.
- Añadir endpoints de API REST.
- Implementar autenticación, persistencia o cualquier lógica de negocio.

## Modelo de datos

Esta funcionalidad no introduce nuevas estructuras de datos.

## Plan de implementación

1. **Añadir dependencias de Cucumber.** Modificar `pom.xml` para incluir `io.cucumber:cucumber-java`, `io.cucumber:cucumber-spring` y `io.cucumber:cucumber-junit-platform-engine`, junto con `org.junit.platform:junit-platform-suite`. Dejar el build funcional.
2. **Crear `config/application.yml`.** Añadir la propiedad `spring.web.resources.static-locations: file:public/` para que Spring Boot sirva archivos estáticos desde la carpeta `public/` en la raíz del proyecto.
3. **Crear `public/index.html`.** Escribir un HTML mínimo que incluya:
   - El texto estático "Hello World" dentro del `<body>`.
   - La declaración de Vue 3 mediante CDN (sin montar ninguna aplicación ni renderizar el texto con Vue).
   - La hoja de estilos matcha.css mediante CDN.
4. **Crear el runner de Cucumber.** Añadir `src/test/java/com/example/demo/bdd/CucumberTestRunner.java` con la configuración base del proyecto.
5. **Crear el feature Gherkin.** Añadir `src/test/resources/features/homepage.feature` con el escenario "Un usuario accede a la página de inicio".
6. **Crear los step definitions.** Añadir `src/test/java/com/example/demo/bdd/steps/HomePageSteps.java` que use `TestRestTemplate` para llamar a `/` y verifique que el cuerpo contiene "Hello World".
7. **Verificar.** Ejecutar `./mvnw test -Dtest=CucumberTestRunner` y comprobar que el escenario pasa. Luego ejecutar `./mvnw clean test package` para validar todo el proyecto.

## Criterios de aceptación

- [x] Al iniciar la aplicación y acceder a `/`, se devuelve `public/index.html`.
- [x] El HTML devuelto contiene el texto "Hello World".
- [x] `index.html` declara Vue 3 mediante CDN.
- [x] `index.html` carga matcha.css mediante CDN.
- [x] `config/application.yml` existe en la raíz del proyecto y configura `spring.web.resources.static-locations: file:public/`.
- [x] Al acceder a una ruta inexistente, sigue apareciendo la página Whitelabel de Spring Boot.
- [x] El escenario Cucumber "Un usuario accede a la página de inicio" pasa.
- [x] `./mvnw clean test package` finaliza con éxito.

## Decisiones tomadas y descartadas

- **Sí:** `config/application.yml` en la raíz del proyecto. Es la ubicación estándar que Spring Boot carga automáticamente como configuración externa.
- **Sí:** `public/` en la raíz del proyecto. El usuario lo pidió explícitamente; la ruta se referencia como `file:public/`.
- **No:** Vue renderiza el contenido. Por ahora solo se declara el CDN; el texto "Hello World" es estático para que los tests con `TestRestTemplate` sean directos y fiables.
- **No:** Reemplazo global de la página Whitelabel. Se mantiene para errores HTTP como 404 y 500.
- **No:** Empaquetar los archivos estáticos dentro del JAR. Se sirven desde el sistema de archivos para respetar la ubicación `public/` en la raíz.

### Decisiones técnicas durante la implementación

- **Sí:** `CucumberSpringConfiguration` en el glue (`com.example.demo.bdd.steps`) con `@CucumberContextConfiguration` + `@SpringBootTest(RANDOM_PORT)` + `@AutoConfigureTestRestTemplate`. Sin ella, cucumber-spring no inyecta beans en los steps; en Spring Boot 4 la auto-configuración de `TestRestTemplate` es opt-in.
- **Sí:** `spring-boot-starter-restclient` en scope test. En Spring Boot 4, `TestRestTemplate` vive en el paquete `org.springframework.boot.resttestclient` y su auto-configuración necesita `RestTemplateBuilder` del módulo `spring-boot-restclient`.
- **Observación:** Surefire no descubre `CucumberTestRunner` en `mvn test` (el nombre no cumple los patrones por defecto `*Test`/`*Tests`). La suite BDD se ejecuta con `./mvnw test -Dtest=CucumberTestRunner`, tal como documenta `AGENTS.md`.

## Riesgos identificados

| Riesgo | Mitigación |
| --- | --- |
| `file:public/` es relativa al directorio desde el que se ejecuta el JAR, no al proyecto. | Documentar que en producción el JAR espera una carpeta `public/` junto a él. Durante desarrollo y tests el directorio de trabajo es la raíz del proyecto, así que funciona. |

## Qué **no** entra en este spec

- Reemplazo de la página Whitelabel para errores.
- Renderizado dinámico con Vue.
- Endpoints de API REST.
- Autenticación o persistencia.
