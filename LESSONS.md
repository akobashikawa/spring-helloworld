# LESSONS.md — Guía técnica de la SPEC 01 (Homepage estática)

Este documento explica los conceptos, la estrategia y las lecciones aprendidas durante la implementación de `specs/01-homepage.md`. Está pensado como guía de estudio para subir de nivel en Spring Boot 4 y BDD.

---

## 1. Objetivo de la especificación

**El problema:** Spring Boot muestra por defecto una página de error ("Whitelabel") cuando no encuentra un controlador para `/`. Eso hace que la aplicación parezca rota o no terminada.

**La meta:** Servir una página web estática real en `/` desde la carpeta `public/` del proyecto, con configuración externa en `config/application.yml`, sin empaquetar los estáticos dentro del JAR.

**Criterio de éxito:** una solución limpia y reproducible: `/` devuelve `index.html` con "Hello World", la Whitelabel sigue activa para errores 404/500, y todo está verificado con un escenario Cucumber.

---

## 2. Conceptos arquitectónicos clave

### 2.1 Configuración externa en Spring Boot

Spring Boot busca `application.yml` o `application.properties` en este orden de prioridad:

1. `./config/` (directorio `config` en el directorio de trabajo)
2. `./` (directorio raíz de trabajo)
3. `classpath:/config/`
4. `classpath:/`

Nuestro `config/application.yml` en la raíz del proyecto tiene **prioridad sobre** `src/main/resources/application.properties`. Esto permite cambiar la configuración de despliegue (ubicación de estáticos, puerto, perfiles) sin recompilar el JAR. El frontend queda así **desacoplado del código fuente compilado**: puedes editar el HTML sin redeployar un artefacto.

```yaml
spring:
  web:
    resources:
      static-locations: file:public/
```

### 2.2 `file:public/` vs `classpath:`

| Modo | Ubicación | Uso típico |
|---|---|---|
| `classpath:/static/` | Dentro del JAR (compilado) | Estáticos empaquetados con la app |
| `file:public/` | Sistema de archivos, relativa al CWD | Estáticos editables sin recompilar |

**Implicación práctica:** `file:public/` es relativa al directorio desde el que se ejecuta `java -jar`. Si lanzas el JAR desde otro directorio, Spring no encontrará `public/`. En desarrollo y tests el directorio de trabajo es la raíz del proyecto, así que funciona sin problemas.

### 2.3 Welcome Page automática

Cuando `spring.web.resources.static-locations` incluye un directorio que contiene `index.html`, Spring Boot registra automáticamente un `WelcomePageHandlerMapping`. El log lo confirma:

```
Adding welcome page: URL [file:public/index.html]
```

No hace falta crear un `@Controller` para `/`.

### 2.4 Frontend minimalista con CDN

- **matcha.css:** CSS minimalista *classless*, sin build step. Solo un `<link>`.
- **Vue 3 declarado pero inactivo:** Se incluye el `<script>` del CDN para que el futuro crecimiento no requiera cambiar el HTML base. El texto "Hello World" es estático en el HTML para que los tests con `TestRestTemplate` (que no ejecutan JavaScript) puedan verificarlo directamente.

---

## 3. Estrategia BDD (Outside-In)

### 3.1 Principio

**Nunca escribir código de producción sin un test que falle primero.** El flujo es:

1. Definir el `.feature` en Gherkin (qué espera el usuario).
2. Escribir los step definitions (que inicialmente fallan o lanzan `PendingException`).
3. Escribir el mínimo código de producción necesario para que pasen.
4. Refactorizar manteniendo los tests en verde.

### 3.2 Gherkin desde la perspectiva de negocio

```gherkin
# language: es
Característica: Página de inicio

  Escenario: Un usuario accede a la página de inicio
    Cuando un usuario accede a la página de inicio
    Entonces ve el mensaje "Hello World"
```

**Regla:** no mencionar detalles técnicos (HTTP, JSON, base de datos, rutas). El escenario describe **comportamiento de negocio**, no implementación.

Esta es la diferencia entre un **test técnico** y un **test de comportamiento**: el técnico verifica *cómo* funciona la app (status 200, payload JSON, endpoints); el de comportamiento verifica *qué* percibe el usuario desde su contexto de negocio.

### 3.3 Arquitectura Cucumber + Spring Boot

```
CucumberTestRunner          → Puente JUnit 5 (Suite, engine cucumber, select features/)
CucumberSpringConfiguration → Puente Spring (@CucumberContextConfiguration + @SpringBootTest)
HomePageSteps               → Step definitions con @Autowired TestRestTemplate
homepage.feature            → Escenario Gherkin en español
```

**Estado compartido:** cada escenario usa una instancia fresca de `HomePageSteps`. La respuesta HTTP se guarda en una variable de instancia (`response`), lo que evita fugas de datos entre escenarios sin necesidad de hooks de limpieza.

---

## 4. Lecciones aprendidas (gotchas de Spring Boot 4)

### Lección 1: Modularización en Spring Boot 4

En Spring Boot 3.x: `org.springframework.boot.test.web.client.TestRestTemplate`
En Spring Boot 4.1.x: `org.springframework.boot.resttestclient.TestRestTemplate`

El paquete cambió: Spring Boot 4 desacopló los starters y movió los clientes de test a módulos propios. Compilar con el paquete antiguo falla con `package does not exist`.

### Lección 2: Auto-configuración de test opt-in

En Spring Boot 4, `@SpringBootTest` **no** habilita automáticamente `TestRestTemplate`. Hay que añadir explícitamente:

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class CucumberSpringConfiguration {
}
```

Sin la anotación, la inyección falla con `No qualifying bean of type TestRestTemplate`.

### Lección 3: Dependencia mínima: `spring-boot-starter-restclient`

La auto-configuración de `TestRestTemplate` necesita `RestTemplateBuilder`, que vive en el módulo `spring-boot-restclient`. `spring-boot-starter-webmvc-test` **no lo incluye**. Solución: añadir en `pom.xml` (scope test):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-restclient</artifactId>
    <scope>test</scope>
</dependency>
```

Sin esto, el contexto falla con `ClassNotFoundException: org.springframework.boot.restclient.RestTemplateBuilder`.

### Lección 4: Patrón de descubrimiento de Surefire

Surefire solo ejecuta por defecto clases cuyo nombre cumple `*Test`, `*Tests` o `*TestCase`. `CucumberTestRunner` no coincide, así que **no se ejecuta** en un `mvn test` normal. Se invoca explícitamente:

```bash
./mvnw test -Dtest=CucumberTestRunner
```

Si quieres que Cucumber corra en cada build, configura `<includes>` en el plugin `maven-surefire-plugin`.

### Lección 5: puente obligatorio `@CucumberContextConfiguration` en el glue

El `@SpringBootTest` en `CucumberTestRunner` **no basta**: cucumber-spring solo lee clases del paquete glue (`com.example.demo.bdd.steps`). Sin una clase con `@CucumberContextConfiguration` ahí, los steps no reciben beans de Spring. La solución es `CucumberSpringConfiguration` en ese paquete.

### Lección 6: Trampas de entorno híbrido WSL2 + Windows

Una ruta Windows (`D:/java/maven-repo/.m2/repository`) en el `settings.xml` global de Maven, al ejecutarse desde WSL, se interpreta como **relativa** y crea un directorio `D:` dentro del proyecto. La configuración de usuario (`~/.m2/settings.xml`) tiene prioridad sobre la global y aísla el entorno Linux del de Windows.

---

## 5. Buenas prácticas para subir de nivel

1. **Diagnosticar por la causa raíz.** Cuando un contexto de Spring falla, busca el `Caused by:` más profundo en el stack trace. El primer error visible rara vez es la causa real.

2. **Verificar el comportamiento real con `curl`.** La Whitelabel no aparece con `Accept: */*` (Spring devuelve JSON de error). Con `Accept: text/html` (como un navegador) sí aparece. Siempre simula el contexto real del usuario.

3. **La configuración de usuario manda.** Tanto en Maven como en IDEs, la jerarquía de configuración (user > global > proyecto) es tu aliada para aislar entornos sin tocar configuraciones compartidas.

4. **Respeta el spec.** Si algo del spec parece mejorable, anótalo como observación y coméntalo, pero implementa lo acordado. Los cambios van al spec, no al código por sorpresa.

5. **Cada paso deja el sistema funcional.** El plan de implementación estaba dividido en 7 pasos, cada uno ejecutable y verificable por separado. Esto permite hacer rollback granular y detectar errores en el paso exacto en que ocurren.
