# ☕ Laboratory 2 – Java Basics & a Simple HTTP Server


## 🎯 Objective
In this lab you’ll learn the **essentials of Java** (syntax, OOP, packages), understand **HTTP request/response** basics (GET/POST/PUT/DELETE), and build a **minimal HTTP server** in Java (no REST framework yet). You’ll also see how to handle **concurrency** with a **fixed thread pool** and persist simple data to a file.


## 🧭 Overview
By the end of this lab, you will be able to:
- Organize a small Java project using **packages** and **single-responsibility classes**.
- Explain how **HTTP** works (methods, status codes, headers, body).
- Implement and run a **file-backed** student manager with endpoints for listing and adding students.
- Use a **thread pool** to handle multiple requests concurrently.
- Test endpoints using **browser / curl / PowerShell**.

> This lab sets the foundation for **Lab 3**, where we’ll evolve the app into a **proper REST API** (JSON payloads, status codes, DTOs).


## 📦 Prerequisites
- JDK **17+**
- IntelliJ IDEA (or any IDE)
- `curl` / PowerShell for simple HTTP testing or Postman (preffered)


## 🗂️ Table of Contents
1. [Introduction: Why Java in DevOps](#1-introduction-why-java-in-devops)
2. [Java Quickstart](#2-java-quickstart)
3. [HTTP Fundamentals](#3-http-fundamentals)
4. [App Design (Before Coding)](#4-app-design-before-coding)
5. [Step-by-Step Implementation](#5-step-by-step-implementation)
6. [Thread Pools Explained](#6-thread-pools-explained)
7. [Testing the Server](#7-testing-the-server)
8. [Lab Work (Student Tasks)](#8-lab-work-student-tasks)
9. [Optional Challenges](#9-optional-challenges)
10. [Troubleshooting & Tips](#10-troubleshooting--tips)
11. [Summary](#11-summary)
12. [Homework](#12-homework)


## 1. Introduction: Why Java in DevOps

Java remains one of the most widely used languages in industry for **microservices**, **enterprise backends**, and **DevOps pipelines**. It’s mature, fast enough, highly portable, and supported by a rich ecosystem (tooling, libraries, cloud platforms).

### 1.1 The JVM & Portability

**JVM (Java Virtual Machine)** is the runtime that executes Java bytecode. You compile `.java` → `.class` files once, and the same bytecode runs on **any OS** with a JVM (Windows, macOS, Linux, containers).

- **Write once, run anywhere**: the same JAR can run on different machines and in Docker containers without recompilation.
- **JIT (Just-In-Time) compilation**: JVM optimizes hot code paths at runtime for performance.
- **GC (Garbage Collection)**: automatic memory management reduces a whole class of memory bugs.

**Important terms:**
- **JDK** (Java Development Kit): compiler (`javac`), tooling, and the runtime. This is what developers install.
- **JRE** (Java Runtime Environment): just the runtime (for running apps). In practice, you’ll use a JDK.
- **Java SE** (Standard Edition): the core language & standard library.

**LTS versions**: Prefer **Java 17 (LTS)** or newer LTS for stability and long support.

### 1.2 Java Ecosystem & Tooling (DevOps-friendly)

Java’s ecosystem provides first-class support for **builds**, **testing**, **dependency management**, and **CI/CD**.

- **Build tools**:  
  - **Maven** (XML `pom.xml`) — deterministic builds, dependency management, standard project layout.  
  - **Gradle** (Groovy/Kotlin scripts) — flexible and fast, popular in modern projects.
- **Testing**: JUnit, Mockito (unit tests, integration tests).
- **Packaging**: JARs (libraries/apps) and **fat JARs** (include dependencies).
- **Observability**: rich logging (SLF4J/Logback), metrics, profiling tools.
- **Containerization**: JARs run well in containers; base images (e.g., Eclipse Temurin) exist for multiple versions.
- **CI/CD**: Well-supported in GitLab CI, GitHub Actions, Jenkins, Azure DevOps, etc.

> For this lab we keep it **framework-free** (no Spring). We’ll use the **built-in HTTP server** to understand fundamentals first.

### 1.3 Where Java fits in Microservices & CI/CD

Java is a common choice for **microservices**:
- **Clear modularity** (packages, interfaces) → easy to split into services.
- **Mature HTTP/REST tooling** (even without frameworks you can serve HTTP easily).
- **Strong typing** + IDE support → large teams can evolve code safely.
- **Great for pipelines**: deterministic builds, versioned artifacts, automated tests, container images.

**Typical lifecycle in DevOps with Java:**
1. Code (IDE)  
2. Build (Maven)
3. Test (JUnit)
4. Package (JAR)  
5. Containerize (Docker/Podman) 
6. Deploy (Kubernetes)  
7. Observe (logs/metrics) 
8. Iterate (CI/CD)

In Lab 2 we’re at steps **1–3** (code/run/test locally). In later labs we’ll containerize and deploy.

### 1.4 Java Project Anatomy (Quick Tour)

A conventional Maven project looks like this:

```
student-manager-server/
  pom.xml                  # Build + dependencies
  src/
    main/
      java/
        unitbv/devops/... # Your packages (code)
      resources/          # Non-code assets (config files, etc.)
    test/
      java/               # Unit tests (optional now)
```

**Packages** (folders under `src/main/java`) organize code and prevent name clashes:
- `unitbv.devops.models` — domain classes (pure data, e.g., `Student`)
- `unitbv.devops.ports` — interfaces/abstractions (e.g., `StudentStore`)
- `unitbv.devops.data` — implementations (e.g., `FileStudentStore`)
- `unitbv.devops.http` or `unitbv.devops.handlers` — request handlers
- `unitbv.devops.util` — tiny helpers (I/O, parsing)

> Follow **single responsibility**: each class has one clear purpose.

### 1.5 Minimal Java: Class, `main`, and Build

**A tiny “Hello Java”**:

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, Java!");
    }
}
```

- `class` groups code.
- `public static void main` is the entry point for the program.
- `System.out.println` prints to console.

In a **Maven** project, you’ll typically run via the IDE’s run button or:
```
mvn -q -DskipTests package
java -jar target/your-app.jar
```

(We won’t package a fat JAR in this lab; running directly from the IDE is fine.)

### 1.6 OOP in Java (fast recap)

Students know OOP; here’s how it looks in Java:

- **Class & Object**: blueprint vs instance.
- **Fields & Methods**: data + behavior.
- **Access modifiers**: `public`, `private`, `protected`, (package-private).
- **Encapsulation**: hide internals, expose a safe API.
- **Immutability**: make objects unchangeable to reduce bugs (e.g., our `Student` is immutable).
- **Interfaces**: contracts (e.g., `StudentStore`); allows swapping implementations (file, memory, DB).

**Example (immutable model + interface):**
```java
// models/Student.java
public final class Student {
    private final String firstName;
    private final String lastName;
    public Student(String first, String last) {
        this.firstName = first.trim();
        this.lastName = last.trim();
    }
    public String firstName() { return firstName; }
    public String lastName()  { return lastName; }
}

// ports/StudentStore.java
public interface StudentStore {
    java.util.List<Student> list() throws java.io.IOException;
    int count() throws java.io.IOException;
    void addAll(java.util.List<String> lines) throws java.io.IOException;
    void deleteByIndex(int index) throws java.io.IOException;
    void clear() throws java.io.IOException;
}
```

This separation keeps our **HTTP layer** thin and focused on **requests/responses**, not storage details.

### 1.7 Why start without a framework?

Starting with plain Java (no Spring) helps you:
- See the **raw HTTP mechanics** (methods, headers, status codes).
- Understand **threading** with explicit **thread pools**.
- Keep the codebase small and readable for beginners.
- Build intuition that translates well to any framework later.


## 2. Java Quickstart

This chapter gets you productive in Java quickly. We set up a small Maven project, explain the standard folder layout and packages, and cover the minimal language features you will use in this lab.

### 2.1 Create the Project (IntelliJ, Maven, JDK 17)

1. **Open IntelliJ IDEA** → *New Project*.
2. Select **Maven** (no archetype is fine) and pick **JDK 17** (or newer LTS).
3. Fill the coordinates:
   - **GroupId:** `unitbv.devops`
   - **ArtifactId:** `student-manager-server`
   - **Version:** `0.1.0-SNAPSHOT`
4. Finish and wait for indexing to complete.

> Tip: If Maven asks to import changes, accept. IntelliJ will generate a `pom.xml` and a `src/` tree.

### 2.2 Standard Project Layout

Maven projects follow a predictable structure. We will use these packages to keep responsibilities separated:

```
student-manager-server/
  pom.xml
  src/
    main/
      java/
        unitbv/devops/
          App.java
          models/       # domain classes (e.g., Student)
          ports/        # interfaces (e.g., StudentStore)
          data/         # implementations (e.g., FileStudentStore)
          http/         # http handlers (e.g., RootHandler)
          util/         # helpers (e.g., HttpUtils)
      resources/        # config files if needed
    test/
      java/             # unit tests (not used in this lab)
```

**Why this matters:** tools, CI/CD, and teammates will expect this layout. It also makes it easy to grow the project later.

### 2.3 Packages & Naming Conventions

- **Package names:** all lowercase, dot-separated: `unitbv.devops.models`
- **Class names:** `PascalCase` (e.g., `Student`, `FileStudentStore`)
- **Methods/fields:** `camelCase` (e.g., `addAll`, `firstName`)
- **Constants:** `UPPER_SNAKE_CASE`

Use **one class per file** and keep classes **single-responsibility**. Example:

```java
// models/Student.java
package unitbv.devops.models;

public final class Student {
    private final String firstName;
    private final String lastName;

    public Student(String firstName, String lastName) {
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }
    public String firstName() { return firstName; }
    public String lastName()  { return lastName; }
}
```

### 2.4 Minimal Language Tour (What You Need Today)

**Entry point**

```java
public class App {
    public static void main(String[] args) {
        System.out.println("Hello!");
    }
}
```

**Variables & types**

```java
int n = 3;
double pi = 3.14;
boolean ok = true;
String name = "Ana";
var count = 5; // 'var' infers the type (Java 10+), still statically typed
```

**Control flow**

```java
if (n > 0) { /* ... */ }
for (int i = 0; i < 3; i++) { /* ... */ }
switch (args.length) {
    case 0 -> System.out.println("no args");
    case 1 -> System.out.println("one arg");
    default -> System.out.println("many");
}
```

**Collections**

```java
java.util.List<String> names = new java.util.ArrayList<>();
names.add("Ana");
for (String s : names) System.out.println(s);
```

**Exceptions**

```java
try {
    // code that may fail
} catch (IOException e) {
    System.err.println("IO failed: " + e.getMessage());
}
```

This lab uses standard library classes from `java.net`, `java.nio.file`, `java.util.concurrent`, and I/O (`java.io` / `java.nio`).

### 2.5 Minimal `pom.xml` (Maven)

If IntelliJ created a skeleton, you can keep it. A minimal `pom.xml` for JDK 17 looks like this:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>unitbv.devops</groupId>
  <artifactId>student-manager-server</artifactId>
  <version>0.1.0-SNAPSHOT</version>

  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
  </properties>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
          <release>17</release>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

> We are not adding external dependencies in this lab. The built-in `com.sun.net.httpserver.HttpServer` is part of the JDK.

### 2.6 Running the App

**From IntelliJ:** open `App.java` and click the green ▶ next to `main`.

**From terminal:**

```bash
mvn -q -DskipTests package
java -cp target/classes unitbv.devops.App
```
or if you configured a JAR with a `Main-Class` manifest:
```bash
java -jar target/student-manager-server-0.1.0-SNAPSHOT.jar
```

### 2.7 I/O & File Paths (relevant to this lab)

We store data in a text file inside the project directory. Use the process working directory to construct the path so it is always visible during development:

```java
String projectDir = System.getProperty("user.dir");
java.nio.file.Path dataFile = java.nio.file.Path.of(projectDir, "data", "students.txt");
System.out.println("Students file: " + dataFile.toAbsolutePath());
```

The `FileStudentStore` ensures the directory exists and writes students as semicolon-separated values:

```
Ana;Popescu
Bogdan;Ionescu
```

### 2.8 Basic Logging (println is fine for this lab)

For now, `System.out.println` is enough. We print:
- server start message with URL
- thread name handling a request (to observe concurrency)
- the absolute path of the data file

In later labs, we can add a logging framework (SLF4J + Logback).

### 2.9 Coding Style Tips

- Keep classes small and focused (one responsibility).
- Prefer immutability for simple models (no setters).
- Validate inputs at the edge (HTTP layer), keep the core strict and simple.
- Use `try-with-resources` for streams/sockets to avoid leaks.
- Document public classes/methods with short Javadoc comments.

### 2.10 Quick Checklist

- [ ] JDK 17 installed and configured in IntelliJ
- [ ] Maven project created with the standard layout
- [ ] Packages created: `models`, `ports`, `data`, `http`, `util`
- [ ] `App` runs and prints the server URL
- [ ] Data file path printed and created under `<project-root>/data/students.txt`


## 3. HTTP Fundamentals

This chapter explains how **HTTP** works so you can reason about requests/responses before touching any framework. We’ll use our simple server as the running example and test with a web browser, `curl`, or PowerShell.

### 3.1 Client–Server Model (Big Picture)

```
Client (Browser / curl / PowerShell)  --->  HTTP Request  --->  Server (our Java app)
Client (Browser / curl / PowerShell)  <---  HTTP Response <---  Server (our Java app)
```

- The **client** initiates a connection and sends an **HTTP request**.
- The **server** reads the request, processes it, and sends back an **HTTP response**.
- HTTP is **stateless**: each request is independent (no implicit memory). The server can store state (file, DB), but the protocol itself does not.

### 3.2 HTTP Request Anatomy

A minimal request looks like this:

```
GET /students HTTP/1.1
Host: localhost:8080
User-Agent: curl/8.0
Accept: */*
```

**Key parts:**
- **Method**: `GET`, `POST`, `PUT`, `DELETE`, ...
- **Path**: `/students` (may include query string: `/students?i=1`)
- **Version**: `HTTP/1.1` (common in simple servers)
- **Headers**: Key–value pairs that add metadata (e.g., `Content-Type`, `Content-Length`)
- **Body**: Optional (present in POST/PUT typically). For us: text/plain with student names (one per line).

**Query string** (after `?`): `key=value&another=1`  
Example: `/students?i=2` → key `i` has value `2`.

### 3.3 HTTP Response Anatomy

A minimal response looks like:

```
HTTP/1.1 200 OK
Content-Type: text/plain; charset=utf-8
Content-Length: 19

# Students (0)
(empty)
```

**Key parts:**
- **Status line**: `HTTP/1.1 200 OK`
- **Headers**: metadata (e.g., content type/length)
- **Body**: the actual content

### 3.4 Common Methods (What They Mean)

| Method  | Typical Use                                 | Has Body? | Safe? | Idempotent? |
|---------|----------------------------------------------|-----------|-------|-------------|
| GET     | Retrieve data                                | No        | ✅    | ✅          |
| POST    | Create / submit data                         | Yes       | ❌    | ❌          |
| PUT     | Replace a resource at a given URI            | Yes       | ❌    | ✅          |
| PATCH   | Partial update                               | Yes       | ❌    | ❌ (usually)|
| DELETE  | Remove a resource                            | No/Yes*   | ❌    | ✅          |
| HEAD    | Same as GET but without body (metadata only) | No        | ✅    | ✅          |

> **Safe** = does not change server state (in theory).  
> **Idempotent** = multiple identical requests have the same effect as one.

In our lab **(pre-REST)**:
- `GET /students` → list current students
- `POST /students` → add students from a text body
- (Student tasks) `GET /count`, `DELETE /students`

### 3.5 Status Codes (What the Numbers Mean)

- **2xx Success**
  - `200 OK` – Request succeeded
  - `201 Created` – Resource created (often with POST; optional in this lab)
- **3xx Redirection** (not used in our lab)
- **4xx Client Errors**
  - `400 Bad Request` – Invalid input / missing data
  - `404 Not Found` – Unknown path
  - `405 Method Not Allowed` – Wrong method for a known path
- **5xx Server Errors**
  - `500 Internal Server Error` – Bug or unexpected condition

We will mainly use `200`, `400`, `404`, `405` in this lab.

### 3.6 Headers You’ll See

- **Content-Type** – tells the client what the body is (`text/plain; charset=utf-8`, `application/json`, etc.)
- **Content-Length** – size of the body in bytes
- **Host** – target host and port
- **User-Agent** – who is calling (browser, curl)

For our server we set `Content-Type: text/plain; charset=utf-8` for readability.

### 3.7 Path, Query, and Body (What Goes Where)

- **Path** – identifies *what* you’re addressing: `/students`, `/count`
- **Query parameters** – small, structured inputs for filtering/selection: `?i=2` (delete index 2)  
  Parsed from the URL after `?`. Multiple params can be joined with `&`.
- **Body** – larger inputs, often used with POST/PUT. In this lab: plain text lines with student names.

Examples in our app:
- `GET /students` – no body
- `POST /students` – body is text with lines: `"Ana Popescu\nBogdan Ionescu"`
- (Student task) `DELETE /students?i=1` – query parameter `i`

### 3.8 Testing with Browser, curl, and PowerShell

**Browser** (GET only):
- `http://localhost:8080/`
- `http://localhost:8080/students`

**curl (macOS/Linux)**:
```bash
# List
curl http://localhost:8080/students

# Add
curl -X POST http://localhost:8080/students -H "Content-Type: text/plain" --data-binary $'Ana Popescu
Bogdan Ionescu'
```

**PowerShell (Windows)**:
```powershell
# List
curl.exe http://localhost:8080/students

# Add (robust one-liner)
curl.exe -X POST http://localhost:8080/students -H "Content-Type: text/plain" --data-binary ([string]::Join("`n", @("Ana Popescu","Bogdan Ionescu")))
```

> Tip: In later labs you can use Postman/Insomnia, but these built-in tools are enough for now.

### 3.9 Content Types (Why They Matter)

- `text/plain` – human-readable text; easy for this lab
- `application/json` – standard for REST APIs (we’ll use it in Lab 3)
- `application/x-www-form-urlencoded` – web forms
- `multipart/form-data` – file uploads

The **client** must send the right `Content-Type` so the **server** knows how to parse the body.

### 3.10 Connections, Keep-Alive, and Concurrency (Briefly)

- **HTTP/1.1** defaults to **keep-alive** connections (reuse TCP connection for multiple requests). Our basic server will typically handle one request per connection, which is fine for learning.
- We use a **fixed thread pool** to serve multiple requests simultaneously. Each incoming request is processed by a worker thread.
- You can observe concurrency by logging `Thread.currentThread().getName()` in handlers and sending multiple requests quickly.

### 3.11 How This Maps to Our App

- **Methods**: We implement `GET` and `POST` now; students add `DELETE` and `GET /count`.
- **Paths**: `/`, `/students`, `/count`
- **Status codes**: `200` for success; `400` for invalid input; `405` for unsupported method; `404` for unknown path (if no context matches).
- **Headers**: We set `Content-Type: text/plain; charset=utf-8` consistently.
- **Body**: For `POST /students`, plain text with one student per line (FirstName LastName).

### 3.12 Quick Reference (Cheat Sheet)

```
REQUEST
<Method> <Path>?<Query> HTTP/1.1
Header: Value
Header: Value

<optional body>

RESPONSE
HTTP/1.1 <StatusCode> <Reason>
Header: Value
Header: Value

<body>
```

**Examples**

```
GET /students HTTP/1.1
Host: localhost:8080

HTTP/1.1 200 OK
Content-Type: text/plain; charset=utf-8

# Students (2)
0: Ana Popescu
1: Bogdan Ionescu
```

```
POST /students HTTP/1.1
Host: localhost:8080
Content-Type: text/plain; charset=utf-8
Content-Length: 27

Ana Popescu
Bogdan Ionescu

HTTP/1.1 200 OK
Content-Type: text/plain; charset=utf-8

ADDED 2
```


## 4. App Design (Before Coding)

This section defines the small architecture we’ll implement: clear **layers**, **single-responsibility** classes, and a **file-backed** store so data survives restarts.

### 4.1 Goals & Non‑Goals
**Goals**
- Keep responsibilities separated (model, storage, HTTP).
- Be easy to test with browser/curl.
- Show basic **concurrency** via a thread pool.
- Persist data to a **project-local file**.

**Non‑Goals (for now)**
- No frameworks (e.g., Spring).
- No JSON or REST DTOs yet.
- No database; a simple text file is enough.

### 4.2 High-Level Structure

```
unitbv.devops/
  App.java                # boot server, register contexts, thread pool
  models/                 # domain (pure data)
    Student.java
  ports/                  # abstractions
    StudentStore.java
  data/                   # adapters (implement ports)
    FileStudentStore.java
  http/ (or handlers/)    # request handlers (I/O at the edge)
    RootHandler.java
    StudentHandler.java   # GET/POST now; DELETE later (lab task)
  util/
    HttpUtils.java        # small helpers: body, text, query params
```

**Why this layout?**
- Keeps **domain** independent from HTTP and storage.
- Lets us swap the store later (e.g., memory → DB) without changing handlers.
- Matches how real apps grow toward REST/CI/CD.

### 4.3 Data Model & Persistence

**Student (immutable)**
- Fields: `firstName`, `lastName`
- Methods: accessors, `toFileLine()`, `fromFileLine(String)`

**StudentStore (port)**
- `list()`, `count()`, `addAll(lines)`, `deleteByIndex(i)`, `clear()`

**FileStudentStore (adapter)**
- File path: `<project>/data/students.txt` (or a custom path you pass in)
- Format: `FirstName;LastName` per line (UTF-8)
- Thread‑safe via a simple **lock**
- **Atomic writes** (write temp + move) to avoid corruption

### 4.4 HTTP Layer & Endpoints

| Path         | Method | Purpose                          | Returned as |
|--------------|--------|----------------------------------|-------------|
| `/`          | GET    | Help page / usage hints          | text/plain  |
| `/students`  | GET    | List students with indices       | text/plain  |
| `/students`  | POST   | Add students (one per line body) | text/plain  |
| `/count`     | GET    | (Lab task) total number          | text/plain  |
| `/students`  | DELETE | (Lab task) clear or `?i=<idx>`   | text/plain  |

**Handlers**
- `RootHandler` – returns help text
- `StudentHandler` – implements GET/POST now; DELETE later (students)
- `CountHandler` – student exercise (GET only)

### 4.5 Concurrency Model

- Server uses `Executors.newFixedThreadPool(4)` and `server.setExecutor(pool)`.
- Each request runs on a worker thread; log `Thread.currentThread().getName()` to visualize.
- The store serializes write operations with a lock; reads are also synchronized to keep it simple and safe.

### 4.6 Typical Flow (Sequence)

```
Client --> HTTP /students (POST "Ana Popescu\n...")
HTTP Handler --> parse body (lines) --> StudentStore.addAll([...])
StudentStore --> FileStudentStore --> writes data file atomically
HTTP Handler --> 200 OK "ADDED N"
```

```
Client --> HTTP /students (GET)
HTTP Handler --> StudentStore.list()
StudentStore --> FileStudentStore --> reads file
HTTP Handler --> 200 OK (indexed plain-text list)
```

### 4.7 Configuration & Paths

- Determine project root at runtime: `System.getProperty("user.dir")`
- Compose data file path: `Path.of(projectDir, "data", "students.txt")`
- Print the absolute path on startup so students can find it easily.

### 4.8 Design Trade‑offs (Why This Way)

- **File vs DB**: file is transparent and easy to inspect; perfect for a learning lab. We can swap to DB later by adding a new adapter.
- **Plain text vs JSON**: plain text keeps HTTP mechanics clear now; JSON comes in Lab 3.
- **Manual handlers vs framework**: shows fundamentals and keeps the code small; framework comes later.
- **Single lock in store**: simple and safe for a lab; adequate for low traffic.


## 5. Step‑by‑Step Implementation

This chapter walks through the minimal implementation so you can run and test the server quickly. We keep it **framework‑free** and aligned with the design from Chapter 4.

### 5.1 Bootstrapping the Server (`App` + thread pool)

Create `unitbv.devops.App`:

```java
package unitbv.devops;

import com.sun.net.httpserver.HttpServer;
import unitbv.devops.data.FileStudentStore;
import unitbv.devops.http.RootHandler;
import unitbv.devops.handlers.StudentHandler;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {
    public static void main(String[] args) throws Exception {
        int port = 8080;

        String projectDir = System.getProperty("user.dir");
        Path dataFile = Path.of(projectDir, "students.txt");
        System.out.println("Students file: " + dataFile.toAbsolutePath());

        var store = new FileStudentStore(dataFile);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());                 // help page
        server.createContext("/students", new StudentHandler(store)); // list/add

        ExecutorService pool = Executors.newFixedThreadPool(4);
        server.setExecutor(pool);

        System.out.println("Server running on http://localhost:" + port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            server.stop(0);
            pool.shutdown();
        }));
    }
}
```

**Why:** `HttpServer` is in the JDK; the fixed thread pool lets us handle multiple requests concurrently.

### 5.2 Root Help Endpoint (`/`)

Create `unitbv.devops.http.RootHandler`:

```java
package unitbv.devops.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RootHandler implements HttpHandler {
    @Override public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.getResponseHeaders().add("Allow", "GET");
            ex.sendResponseHeaders(405, -1);
            ex.close();
            return;
        }
        String body = "Student Manager (Lab 2)\n"
                + "Endpoints:\n"
                + "- GET  /students\n"
                + "- POST /students  (text/plain; one per line: \"FirstName LastName\")\n"
                + "(Lab tasks next): GET /count, DELETE /students\n";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
        System.out.println("[/] GET on " + Thread.currentThread().getName());
    }
}
```

Open `http://localhost:8080/` to verify the server is up.

### 5.3 Utilities (`HttpUtils`)

Create `unitbv.devops.util.HttpUtils`:

```java
package unitbv.devops.util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class HttpUtils {
    private HttpUtils() {}

    public static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void sendText(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    public static Map<String, List<String>> queryParams(HttpExchange ex) {
        String raw = ex.getRequestURI().getRawQuery();
        Map<String, List<String>> map = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return map;
        for (String pair : raw.split("&")) {
            if (pair.isBlank()) continue;
            String[] kv = pair.split("=", 2);
            String k = urlDecode(kv[0]);
            String v = kv.length > 1 ? urlDecode(kv[1]) : "";
            map.computeIfAbsent(k, _k -> new ArrayList<>()).add(v);
        }
        return map;
    }

    public static Optional<String> firstQueryParam(HttpExchange ex, String key) {
        var vals = queryParams(ex).get(key);
        return (vals == null || vals.isEmpty()) ? Optional.empty() : Optional.ofNullable(vals.get(0));
    }

    private static String urlDecode(String s) { return URLDecoder.decode(s, StandardCharsets.UTF_8); }
}
```

**Why:** small helpers keep handlers short and readable.

### 5.4 `/students` – GET (list with indices)

Create `unitbv.devops.handlers.StudentHandler` (GET first):

```java
package unitbv.devops.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import unitbv.devops.models.Student;
import unitbv.devops.ports.StudentStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StudentHandler implements HttpHandler {
    private final StudentStore store;
    public StudentHandler(StudentStore store) { this.store = store; }

    @Override public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase();
        switch (method) {
            case "GET" -> handleGet(ex);
            case "POST" -> handlePost(ex);
            default -> {
                ex.getResponseHeaders().add("Allow", "GET, POST");
                ex.sendResponseHeaders(405, -1);
                ex.close();
            }
        }
        System.out.println("[/students " + method + "] on " + Thread.currentThread().getName());
    }

    private void handleGet(HttpExchange ex) throws IOException {
        List<Student> students = store.list();
        StringBuilder sb = new StringBuilder("# Students (").append(students.size()).append(")\n");
        if (students.isEmpty()) {
            sb.append("(empty)\n");
        } else {
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                sb.append(i).append(": ").append(s.firstName()).append(" ").append(s.lastName()).append("\n");
            }
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    // POST added in the next subsection
    private void handlePost(HttpExchange ex) throws IOException { /* ... */ }
}
```

Run and visit `http://localhost:8080/students` — it should show an empty list initially.

### 5.5 `/students` – POST (add students from body)

Complete the `handlePost` method above. We accept **text/plain**, one student per line: `FirstName LastName`.

```java
// inside StudentHandler
private void handlePost(HttpExchange ex) throws IOException {
    String raw = unitbv.devops.util.HttpUtils.readBody(ex);
    String[] lines = raw.replace("\r\n", "\n").split("\n");
    java.util.List<String> toAdd = new java.util.ArrayList<>();
    for (String line : lines) {
        if (line == null) continue;
        String t = line.trim();
        if (!t.isEmpty()) toAdd.add(t);
    }
    if (toAdd.isEmpty()) {
        unitbv.devops.util.HttpUtils.sendText(ex, 400, "ERROR: empty body\n");
        return;
    }
    store.addAll(toAdd);
    unitbv.devops.util.HttpUtils.sendText(ex, 200, "ADDED " + toAdd.size() + "\n");
}
```

**Test (PowerShell one‑liner):**
```powershell
curl.exe -X POST http://localhost:8080/students -H "Content-Type: text/plain" --data-binary ([string]::Join("`n", @("Ana Popescu","Bogdan Ionescu")))
```

Then list:
```powershell
curl.exe http://localhost:8080/students
```

### 5.6 File Location & Persistence

We print the absolute file path at startup:
```java
String projectDir = System.getProperty("user.dir");
Path dataFile = Path.of(projectDir, "data", "students.txt");
System.out.println("Students file: " + dataFile.toAbsolutePath());
```

The `FileStudentStore` ensures the directory exists and writes in UTF‑8 as:
```
Ana;Popescu
Bogdan;Ionescu
```

You can inspect the file after POST requests to confirm persistence.

### 5.7 What Students Implement Next (Preview)

- **`GET /count`** (new `CountHandler`) → returns the number of students as plain text.
- **`DELETE /students`** in `StudentHandler`:
  - no query → clear all
  - `?i=<index>` → delete by index, return 400 if out of range

We keep these as exercises to practice method routing, query parameters, and simple validation.


## 6. Thread Pools Explained

This chapter explains why we use a thread pool, how requests are processed concurrently, and how to observe and tune it.

### 6.1 Why a Thread Pool?

Servers must handle multiple clients at the same time. Creating a new thread per request is simple but wasteful: threads are expensive to create, can grow unbounded, and may exhaust memory. A thread pool:
- Reuses a fixed number of worker threads
- Limits concurrency to protect the machine
- Provides a central place to shut down cleanly

In our app we use a fixed pool with 4 workers:
```java
ExecutorService pool = Executors.newFixedThreadPool(4);
server.setExecutor(pool);
```

### 6.2 Request Lifecycle With a Pool

1) Client connects and sends an HTTP request  
2) The `HttpServer` accepts the connection and dispatches to a worker thread from the pool  
3) The handler executes on that thread and writes the response  
4) The worker returns to the pool, ready for another request

Visualize by logging the thread name:
```java
System.out.println("[/students GET] on " + Thread.currentThread().getName());
```

Send several requests quickly (e.g., open multiple terminals or use curl in a loop) and watch different worker names handle them.

### 6.3 Demonstrating Concurrency

Add a temporary slow endpoint (optional) to simulate work:
```java
// In a demo handler
long ms = 1000;
try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
System.out.println("Slept " + ms + "ms on " + Thread.currentThread().getName());
```
Call it 4–8 times in quick succession. With a pool size of 4, you should see about 4 requests handled in parallel, then the rest queued.

### 6.4 Choosing Pool Size

Rules of thumb:
- CPU-bound tasks → number of CPU cores (e.g., Runtime.getRuntime().availableProcessors())
- I/O-bound tasks → more threads than cores because threads spend time waiting for I/O

For this lab, 4 is fine. In production, measure and tune.

### 6.5 Shutdown and Cleanup

Always shut down the pool and server gracefully to avoid resource leaks:
```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    System.out.println("Shutting down...");
    server.stop(0);
    pool.shutdown();
}));
```
You can optionally wait for termination and fall back to `shutdownNow()` if the app must exit quickly.

### 6.6 Thread Safety in Our App

The file-backed store protects its critical sections with a simple lock so concurrent writes are serialized and consistent. Handlers do not share mutable state; they call the store’s API. This is enough for our learning goals and small workloads.

### 6.7 Quick Checklist

- [ ] Thread pool created and set on the server  
- [ ] Log thread names in handlers to visualize concurrency  
- [ ] Optional slow endpoint to see parallelism in action  
- [ ] Graceful shutdown closes the server and the executor


## 7. Testing the Server

This chapter shows how to verify endpoints with a browser, curl (macOS/Linux), and PowerShell (Windows). It also includes a tiny Java client and a troubleshooting guide.

### 7.1 Start the Server

Run `unitbv.devops.App` from IntelliJ. The console prints the URL and the absolute path of the data file, for example:
```
Server running on http://localhost:8080
Students file: C:\path\to\project\data\students.txt
```

### 7.2 Test with a Web Browser (GET only)

Open these URLs:
- `http://localhost:8080/` (help)
- `http://localhost:8080/students` (list)

### 7.3 Test with curl (macOS/Linux)

List students:
```bash
curl http://localhost:8080/students
```

Add students (one-liner, text/plain body):
```bash
curl -X POST http://localhost:8080/students -H "Content-Type: text/plain" --data-binary $'Ana Popescu
Bogdan Ionescu
Carla Matei'
```

List again:
```bash
curl http://localhost:8080/students
```

### 7.4 Test with PowerShell (Windows)

List students:
```powershell
curl.exe http://localhost:8080/students
```

Add students (robust one-liner):
```powershell
curl.exe -X POST http://localhost:8080/students -H "Content-Type: text/plain" --data-binary ([string]::Join("`n", @("Ana Popescu","Bogdan Ionescu","Carla Matei")))
```

List again:
```powershell
curl.exe http://localhost:8080/students
```

### 7.5 Verify the Data File

From the project root, check the file contents.

macOS/Linux:
```bash
cat students.txt
```

Windows PowerShell:
```powershell
Get-Content .\students.txt
```

Expected format (semicolon-separated):
```
Ana;Popescu
Bogdan;Ionescu
Carla;Matei
```

### 7.6 Tiny Java Test Client (optional)

Create `unitbv.devops.TestClient` to script requests without external tools:
```java
package unitbv.devops;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

public class TestClient {
    private static final HttpClient http = HttpClient.newHttpClient();
    private static final String BASE = "http://localhost:8080";

    public static void main(String[] args) throws Exception {
        get("/students");
        post("/students", "Ana Popescu
Bogdan Ionescu");
        get("/students");
    }

    static void get(String path) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(BASE + path)).GET().build();
        var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("GET " + path + " -> " + res.statusCode());
        System.out.println(res.body());
    }

    static void post(String path, String body) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("POST " + path + " -> " + res.statusCode());
        System.out.println(res.body());
    }
}
```

Run it after starting the server.

### 7.7 Troubleshooting

- **Nothing appears in browser**  
  Ensure the server is running in IntelliJ and the console shows the URL.

- **404 Not Found**  
  Check the path (`/students`) and the port (`8080`).

- **405 Method Not Allowed**  
  Only GET and POST are implemented now. DELETE and `/count` are student tasks.

- **No file created**  
  Make sure `App` prints the absolute path. In Run/Debug Configuration, set Working directory to `$ProjectFileDir$`.

- **Windows: POST adds only 1 student**  
  Use the PowerShell one-liner with `[string]::Join("`n", @(...))` so newlines are preserved.

- **Encoding issues**  
  The server sets `text/plain; charset=utf-8`. Ensure your client sends UTF-8 (curl and PowerShell defaults are fine).


## 8. Lab Work (Student Tasks)

In this section you will extend the server with two features using only core Java and the existing structure. Keep responses as **text/plain** and follow the existing style (status codes, headers, short messages). Do **not** add external libraries.

### 8.1 Task A — Implement GET /count

Goal: Return the total number of students as plain text.

1) Create a new handler class `unitbv.devops.handlers.CountHandler`.
2) The handler should accept **only** `GET`. For any other method, respond with **405** and an `Allow` header.
3) Use `StudentStore.count()` to obtain the number.
4) Set `Content-Type: text/plain; charset=utf-8` and return the number as the body (e.g., `3`).

Registration (in `App`):
```java
// Add this next to the other contexts
// server.createContext("/count", new CountHandler(store));
```
(You must create the class and import it correctly.)

Edge cases to consider:
- Method ≠ GET → **405**
- Normal path → **200** with a single line containing the integer

### 8.2 Task B — Implement DELETE /students

Goal: Support two behaviors on the same path:
- `DELETE /students` (no query) → clear all students → respond `CLEARED`
- `DELETE /students?i=<index>` → delete by **zero-based index**
  - On success → **200** with `DELETED <index>`
  - If index invalid or out of range → **400** with `ERROR: index out of range`
  - If index is not a number → **400** with `ERROR: invalid index`

Steps:
1) In `StudentHandler.handle(...)`, route `DELETE` to a new `handleDelete(...)` method.
2) Parse the optional query parameter `i`. Use `HttpUtils.firstQueryParam(ex, "i")`.
3) If `i` is absent → call `store.clear()` → respond `CLEARED`.
4) If `i` is present → parse to `int`, validate, and call `store.deleteByIndex(i)`.
5) Return short, human-readable messages as in the rest of the app.

Hints:
- Reuse `HttpUtils.sendText(...)` and `HttpUtils.firstQueryParam(...)`.
- Use `try/catch` to handle `NumberFormatException` and respond with **400**.
- Use `try/catch` to handle `IndexOutOfBoundsException` and respond with **400**.

### 8.3 Validation & Return Codes

- **200 OK** for success
- **400 Bad Request** for invalid or out-of-range index
- **405 Method Not Allowed** for unsupported methods on known paths
- Responses should be plain text, one or two short lines

### 8.4 Testing Commands

macOS/Linux:
```bash
curl http://localhost:8080/students
curl -X POST http://localhost:8080/students -H "Content-Type: text/plain" --data-binary $'Ana Popescu
Bogdan Ionescu
Carla Matei'
curl http://localhost:8080/count
curl -X DELETE "http://localhost:8080/students?i=1"
curl -X DELETE http://localhost:8080/students
```

Windows PowerShell:
```powershell
curl.exe http://localhost:8080/students
curl.exe -X POST http://localhost:8080/students -H "Content-Type: text/plain" --data-binary ([string]::Join("`n", @("Ana Popescu","Bogdan Ionescu","Carla Matei")))
curl.exe http://localhost:8080/count
curl.exe -X DELETE "http://localhost:8080/students?i=1"
curl.exe -X DELETE http://localhost:8080/students
```

### 8.5 Hints

- Keep handlers **stateless**; all state is in `StudentStore`.
- Log the current thread name to see concurrency: `Thread.currentThread().getName()`.
- Keep error messages consistent: small, clear, and the same wording as above.

### 8.6 Success Criteria (Checklist)

- [ ] `GET /count` returns a single integer as text
- [ ] `DELETE /students` clears all when no `i` is provided
- [ ] `DELETE /students?i=<index>` removes the correct entry
- [ ] Invalid index or parse errors return **400** with the required message
- [ ] No crashes with repeated or parallel calls


## 9. Optional Challenges

These exercises are optional and intended for students who finish early. Keep responses as text/plain. Do not add external libraries.

### 9.1 PUT /students?i=<index> — Update an entry
Goal: Replace the name at the given zero-based index with the body value ("FirstName LastName").
Hints:
- Accept only PUT; respond 405 otherwise.
- Validate the index and body; return 400 on errors.
- Reuse parsing logic for splitting first/last name.
- Consider returning a short confirmation message (e.g., UPDATED <index>).

### 9.2 GET /search?name=<substr> — Case-insensitive search
Goal: Return only students whose full name contains the substring.
Hints:
- Accept only GET.
- Normalize with `toLowerCase()` before comparing.
- Return an indexed list like the GET /students endpoint, but only matches.

### 9.3 GET /sleep?ms=N — Simulate latency (concurrency demo)
Goal: Sleep for N milliseconds, then return a confirmation line.
Hints:
- Accept only GET.
- Parse ms from the query string; guard against invalid or very large values.
- Log the current thread name to visualize parallel handling.

### 9.4 HEAD /students — Metadata-only
Goal: Support HEAD to return headers (e.g., Content-Length) without a body.
Hints:
- Accept only HEAD.
- Compute the same response length you would send for GET /students, but send no body.
- Useful to understand HTTP semantics beyond GET/POST.

### 9.5 Basic Input Validation
Goal: Make the server more robust.
Hints:
- Trim whitespace aggressively.
- Reject empty names or names with digits (if you want stricter rules).
- Return clear, consistent error messages and proper status codes.

### 9.6 Friendly Help Page (/)
Goal: Make the root page more informative.
Hints:
- List all available endpoints and examples.
- Add one-click test links for GET endpoints to help debugging.

### 9.7 Minimal Metrics (manual counters)
Goal: Track simple counters (requests handled, adds, deletes).
Hints:
- Keep counters in memory using `AtomicLong`.
- Expose them via `GET /metrics` as plain text.
- This is a stepping stone toward observability in later labs.

### 9.8 Graceful Shutdown Improvements
Goal: Ensure clean shutdown and quick restarts.
Hints:
- Await executor termination.
- Close any open streams robustly.
- Print a final “Server stopped” line.


## 10. Troubleshooting & Tips

This chapter lists common issues you might hit and quick ways to solve them. Keep it nearby while testing.

### 10.1 Server won’t start
- **Port 8080 already in use**  
  - macOS/Linux: `lsof -i :8080` then kill the PID.  
  - Windows PowerShell: `netstat -ano | findstr :8080` then `taskkill /PID <pid> /F`.
- **Nothing printed**  
  - Run `unitbv.devops.App` (not tests). Check the Run Configuration is set to use **JDK 17+**.

### 10.2 File not created / wrong location
- Ensure `App` prints the absolute file path on startup:
  ```java
  String projectDir = System.getProperty("user.dir");
  Path dataFile = Path.of(projectDir, "students.txt");
  System.out.println("Students file: " + dataFile.toAbsolutePath());
  ```
- IntelliJ Run/Debug Configuration → **Working directory** → `$ProjectFileDir$`.
- Windows AV/security tools can lock files. If you see access errors, try running IntelliJ as admin just to diagnose (not required normally).

### 10.3 POST adds only 1 student (Windows)
- PowerShell collapses newlines in some forms. Use the robust one-liner:
  ```powershell
  curl.exe -X POST http://localhost:8080/students -H "Content-Type: text/plain" --data-binary ([string]::Join("`n", @("Ana Popescu","Bogdan Ionescu","Carla Matei")))
  ```

### 10.4 404 / 405 errors
- **404 Not Found**: Wrong path or missing context. Use `/students`, `/`, (later `/count`).  
- **405 Method Not Allowed**: You used a method the handler doesn’t support (e.g., DELETE before you implement it). Check the `Allow` header in the response.

### 10.5 Encoding / weird characters
- Server sets `Content-Type: text/plain; charset=utf-8`.  
- Ensure the client sends UTF-8. curl and PowerShell are fine by default. Don’t force ANSI/Windows-1252.

### 10.6 Concurrency observations
- Log thread names inside handlers:
  ```java
  System.out.println("[/students] on " + Thread.currentThread().getName());
  ```
- To simulate load, add a temporary `Thread.sleep(1000)` in a test handler and fire several requests quickly. With a pool of 4, you’ll see ~4 concurrent requests.

### 10.7 Index out of range (DELETE)
- If you implemented `DELETE /students?i=<index>` and see `IndexOutOfBoundsException`, handle it and return **400** with a clear message:
  ```java
  try {
      store.deleteByIndex(i);
  } catch (IndexOutOfBoundsException ex) {
      HttpUtils.sendText(exch, 400, "ERROR: index out of range\n");
      return;
  }
  ```

### 10.8 Resetting data for a clean test
- Stop the server and delete `data/students.txt`, or use your (implemented) `DELETE /students` to clear.  
- Re-run the server and POST fresh data.

### 10.9 Quick sanity commands
- macOS/Linux:
  ```bash
  curl http://localhost:8080/
  curl http://localhost:8080/students
  curl -X POST http://localhost:8080/students -H "Content-Type: text/plain" --data-binary $'Ana Popescu\nBogdan Ionescu'
  curl http://localhost:8080/students
  ```
- Windows PowerShell:
  ```powershell
  curl.exe http://localhost:8080/
  curl.exe http://localhost:8080/students
  curl.exe -X POST http://localhost:8080/students -H "Content-Type: text/plain" --data-binary ([string]::Join("`n", @("Ana Popescu","Bogdan Ionescu")))
  curl.exe http://localhost:8080/students
  ```

### 10.10 Tips for clean code
- Keep classes **single-responsibility**. Don’t mix HTTP parsing with file I/O.
- Validate inputs at the edge (handlers), keep the core strict and simple.
- Prefer **immutable** models (`Student`) to avoid accidental shared state.
- Use small helper methods (`HttpUtils`) to avoid copy–paste.
- Add short Javadoc comments to public classes and methods.

### 10.11 When things still break
- Read the full stack trace in the IntelliJ Run window; it points to the exact line.
- Add temporary `System.out.println` logs around suspicious code paths.
- Reduce the problem: comment out parts until you isolate the failing piece.
- Ask a teammate to reproduce with your commands and file.


## 11. Summary

In this lab you:
- Reviewed the **Java basics** you need for backend work (JDK/JVM, packages, classes, interfaces).
- Learned the **HTTP fundamentals**: requests, responses, methods, status codes, headers, and bodies.
- Designed a small, layered application with clear responsibilities:
  - `models` (domain), `ports` (interfaces), `data` (file-backed store), `http/handlers` (endpoints), `util` (helpers).
- Implemented and ran a **minimal HTTP server** using the JDK’s `HttpServer`:
  - `/` for help
  - `/students` with **GET** (list) and **POST** (add from text body)
- Used a **fixed thread pool** to handle multiple requests concurrently and observed it via logs.
- Tested endpoints with browser, curl, and PowerShell, and verified **file persistence** in `data/students.txt`.
- Practiced extending the server in the **Lab Work** section (adding `/count` and `DELETE /students`).

This foundation prepares you for the next lab, where you will:
- Introduce **JSON** payloads and proper **REST** semantics.
- Return structured responses (status + JSON body).
- Add more validation and error handling.
- Prepare for **containerization** and CI/CD.

Keep your code small, your responsibilities clear, and your tests repeatable.


## 📚 12. Homework
No homework is assigned for this lab.

