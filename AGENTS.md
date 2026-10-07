# Agent Instructions: Spring Boot + Maven + Spec-Driven BDD (Cucumber)

You are an expert Java and Spring Boot software engineer. Your objective is to develop features strictly following **BDD (Behavior-Driven Development)** methodology and Outside-In development principles.

---

## 🚀 Project Context & Stack
- **Application:** Java 21 Spring Boot 4.1.1 web application using `spring-boot-starter-webmvc`.
- **Main Class:** `com.example.demo.DemoApplication`
- **Output Artifact:** `target/demo-0.0.1-SNAPSHOT.jar` (repackaged, executable).
- **Maven Wrapper:** Always use `./mvnw` on Linux/macOS or `mvnw.cmd` on Windows. Do not use global `mvn`.

### 🚨 Critical File & System Constraints
- **POM Configuration:** `pom.xml` intentionally overrides `<licenses>`, `<developers>`, and `<scm>` with empty elements to prevent inheritance from the Spring Boot parent. **Do not remove or alter these empty elements** unless explicitly instructed to inherit parent values.
- **Line Endings:** `.gitattributes` forces **LF** for `mvnw` and **CRLF** for `*.cmd`. Do not change or override these line endings when modifying files.
- **Documentation:** `HELP.md` is auto-generated; `.gitignore` deliberately excludes it. Do not attempt to track it.

---

## 🔄 Spec-Driven Workflow
This repo uses the locally installed `/spec` and `/spec-impl` skills (see `.agents/skills/spec/SKILL.md` and `.agents/skills/spec-impl/SKILL.md`).

- **Before coding:** run `/spec <short feature description>` to design the feature. The skill writes the spec to `specs/NN-slug.md`.
- **State machine:** a spec must be in an **Approved** state (any language) before implementation.
- **Implementation:** run `/spec-impl NN-slug` to create a branch (`spec-NN-slug` by default) and implement step by step.
- **Branch creation:** controlled by `specs/.spec-config.yml` (`AutoCreateBranch: true` by default). Create this file if it does not exist and you want a non-default behavior.
- **Relation to BDD:** `/spec-impl` follows the implementation plan; for each step turn the acceptance criteria into Gherkin `.feature` files and step definitions using the layout below.

---

## 🛠️ Required BDD Workflow (Outside-In)
Never write production code without an initial failing test. Follow this exact lifecycle:
1. **Define Feature:** Create or update a `.feature` file in Gherkin syntax describing business behavior.
2. **Write Step Definitions:** Map the Gherkin steps to Java code. Ensure they initially fail or throw a `PendingException`.
3. **Write Minimum Code:** Implement only the production code in Spring Boot necessary to make the Cucumber scenarios pass.
4. **Refactor:** Clean and optimize code while ensuring tests remain green.

### Directory Layout
```text
src/
├── main/
│   └── java/com/example/demo/          # Production Source (Controllers, Services, Repositories)
└── test/
    ├── java/com/example/demo/
    │   ├── bdd/                        # BDD Context
    │   │   ├── CucumberTestRunner.java # JUnit 5 Cucumber Suite Runner
    │   │   └── steps/                  # Step definition classes (@Given, @When, @Then)
    └── resources/
        └── features/                   # Gherkin .feature files
```

### Base `CucumberTestRunner.java` Configuration
Use this structure to bind Cucumber with the Spring Boot application context using modern JUnit 5 Platform Suite Engine:
```java
package com.example.demo.bdd;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.example.demo.bdd.steps")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberTestRunner {
    // Acts as the JUnit 5 bridge to execute Cucumber features. Leaves class body empty.
}
```

---

## 📐 Development Rules & Best Practices

### 1. Gherkin Writing Guidelines (`.feature`)
- Write scenarios completely from the business or user perspective.
- **Exclude technical implementations** (e.g., do not write "database", "JSON payload", "HTTP 200", or "REST API").
- ❌ *Bad:* `When I send a POST request to /api/users with a user JSON`
-  *Good:* `When I register a new user with valid details`

### 2. Step Definitions & State Management
- Use **`TestRestTemplate`** to interact with the endpoints of the running app instance hosted on the random web test port.
- **State Sharing:** Store scenario state in instance variables inside the step classes. Cucumber safely instantiates a fresh class instance for every individual scenario to prevent data leakage.
- Clear or rollback test data between scenarios using Cucumber's `@Before`/`@After` hooks or Spring's `@Transactional`.

---

## 💻 Common Terminal Commands
Always execute build and verification tasks through the wrapper:
* **Build & Verify:** `./mvnw clean test package`
* **Run Local Dev Server:** `./mvnw spring-boot:run`
* **Run Specific Test Class:** `./mvnw test -Dtest=CucumberTestRunner` (or `DemoApplicationTests`)
* **Build OCI Container Image:** `./mvnw spring-boot:build-image`
