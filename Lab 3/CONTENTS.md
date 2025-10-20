# 🧪 Laboratory 3 – Inside HTTP Servers & Thread Pools

## 🎯 Objective

In this lab we’ll **build our own mini HTTP server from scratch**, using:
- raw Java sockets (`ServerSocket`, `Socket`);
- a **thread pool** (`Executors.newFixedThreadPool`) to handle multiple clients;
- simple text parsing for HTTP requests and responses;
- the **same student storage system** from Lab 2 (`FileStudentStore`, `Student`, etc.).

By the end of this session, you’ll understand **what happens behind `HttpServer.create(...)`**  
and how concurrency and I/O work together to serve multiple HTTP requests in parallel.

## 📘 Prerequisites
- Complete Lab 2 (HTTP server with `HttpServer`).
- Understand basic HTTP request/response format.
- Know how to run Java apps from IntelliJ / VS Code / terminal.

## 🗂️ Table of Contents

| # | Section | Description |
|:-:|:--|:--|
| 1 | [Introduction: From Lab 2 to Lab 3](#1-introduction-from-lab-2-to-lab-3) | Why we go “lower level” |
| 2 | [Understanding the HTTP Protocol](#2-understanding-the-http-protocol) | Start-line, headers, body |
| 3 | [Building a Simple Raw Server](#3-building-a-simple-raw-server) | `ServerSocket` + thread pool |
| 4 | [Routing & Handlers](#4-routing--handlers) | Map URL paths to actions |
| 5 | [Connecting with StudentStore](#5-connecting-with-studentstore) | Reuse Lab 2 storage layer |
| 6 | [Concurrency in Action](#6-concurrency-in-action) | Thread pool visualization |
| 7 | [Lab Tasks](#7-lab-tasks) | What to implement |
| 8 | [Testing & Debugging](#8-testing--debugging) | curl examples & thread logs |
| 9 | [Optional Challenges](#9-optional-challenges) | For curious minds ✨ |

## 1. Introduction: From Lab 2 to Lab 3

In **Lab 2**, we used Java’s built-in `HttpServer` — a convenient API that hides the internal details:
- connection handling,
- thread creation,
- HTTP parsing and response formatting.

Now, in **Lab 3**, you’ll **re-implement the essentials manually**, using:
- `ServerSocket` to accept TCP clients;
- manual reading of request lines (`GET /students HTTP/1.1`);
- writing back raw HTTP responses (`HTTP/1.1 200 OK`...).

You’ll keep the **same application logic** (student listing, adding, counting),
but see **how the lower layer works**.

---

> 💡 **Goal:** demystify how a web server works under the hood.  
> Once you understand this, frameworks like Spring Boot or Express.js will make much more sense.

---

# 🧩 1. Introduction: From Lab 2 to Lab 3

In **Lab 2**, you learned how to use Java’s built-in **`HttpServer`** to quickly expose endpoints like `/students`.  
That server hid most of the complexity for you — it:
- accepted connections automatically,
- parsed HTTP requests,
- dispatched to handlers,
- managed threads in the background.

<div align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/c/c9/Client-server-model.svg/1200px-Client-server-model.svg.png" width="400"/>
  <p><i>In Lab 2 you were “above” this layer — now we’ll go inside it.</i></p>
</div>

## 🧠 Why Lab 3?

Because we don’t want to be “framework users” forever —  
we want to **see what happens under the hood**.

In **Lab 3**, you’ll build your own *mini HTTP server*, starting from the lowest level:
- using a `ServerSocket` to accept clients,  
- reading raw bytes (`GET /students HTTP/1.1\r\n...`),  
- sending back text responses,  
- managing concurrency with a **thread pool** you control.

---

<details>
<summary>💬 <b>Quick recap – Lab 2 architecture (click to expand)</b></summary>

```
App.java
 ├── FileStudentStore.java   ← file-based storage
 ├── StudentHandler.java     ← handles /students
 ├── RootHandler.java        ← welcome page
 └── HttpServer (built-in)   ← creates routes, threads, etc.
```

➡️ When a request like `GET /students` arrived,  
`HttpServer` automatically called your `StudentHandler.handle()` method.  
You just wrote the logic — not the networking.
</details>

## 🧩 What Changes in Lab 3?

We’ll rebuild **the core of HttpServer manually**, piece by piece:

| Layer | Lab 2 | Lab 3 |
|:--|:--|:--|
| Network | Hidden inside `HttpServer` | Implemented with `ServerSocket` |
| Routing | `server.createContext("/students", ...)` | Custom router map |
| Threads | Auto-managed | `Executors.newFixedThreadPool(4)` |
| Parsing | Automatic | Manual string parsing |
| Storage | File-based `FileStudentStore` | Reused as-is ✅ |

<a id="demo-mini-server"></a>

### 🔧 Demo: Minimal Server (preview)

Here’s what the **simplified version** looks like — it’s only a few lines:

```java
ServerSocket server = new ServerSocket(8080);
ExecutorService pool = Executors.newFixedThreadPool(4);
System.out.println("Server running on http://localhost:8080");

while (true) {
    Socket client = server.accept();
    pool.execute(() -> handle(client));
}
```

Each accepted connection (`Socket`) is handed to the **thread pool**.  
Your `handle()` function will read the HTTP text, process it, and reply.

<details>
<summary>💡 Why a thread pool?</summary>

Without one, each client would **block** the server until it finished.
A fixed pool lets multiple requests run in parallel,
while keeping the CPU usage predictable.
</details>

## 🎯 Learning outcomes

After completing this lab, you will:
- understand the structure of an HTTP request and response;
- implement a raw Java socket server;
- manage concurrency explicitly;
- integrate it with your existing Lab 2 logic;
- and observe multi-threaded execution in action.

# 2. Understanding the HTTP Protocol

The **HTTP protocol** (HyperText Transfer Protocol) is the language your browser speaks with servers.  
It’s a **text-based** protocol — meaning every request and response is human-readable.

Let’s open the box and see what really travels between your browser and your Java server 👇

<details>
<summary>📦 <b>Click to reveal a real HTTP request example</b></summary>

```
GET /students HTTP/1.1
Host: localhost:8080
User-Agent: curl/8.4.0
Accept: */*
```

That’s it! Four lines of plain text:
1. The **start line**: `GET /students HTTP/1.1`
2. A few **headers**: `Host`, `User-Agent`, `Accept`
3. An empty line → marks the end of headers  
4. (optional) **body** → used in POST, PUT requests
</details>

## 🧩 Anatomy of a Request

| Part | Example | Description |
|:--|:--|:--|
| **Method** | `GET`, `POST`, `DELETE`, `PUT` | Action the client wants |
| **Path** | `/students` | Which resource |
| **Version** | `HTTP/1.1` | Protocol version |
| **Headers** | `Host: localhost:8080` | Metadata about the request |
| **Body** | `Ana Popescu` | Data sent by client (for POST) |

> 💡 A request ends with a **blank line** (`\r\n\r\n`), after which the body (if any) starts.

### ✉️ Example – POST request

If a user adds a new student via POST:

```
POST /students HTTP/1.1
Host: localhost:8080
Content-Type: text/plain
Content-Length: 12

Ana Popescu
```

The server must:
1. Read the headers to detect `Content-Length: 12`
2. Then read exactly **12 bytes** from the stream for the body  
3. Parse and store the student name

<details>
<summary>🧠 <b>What if Content-Length is wrong?</b></summary>

If `Content-Length` is smaller than the body → part of the data is lost.  
If it’s too large → the server keeps waiting forever.  
That’s why parsing headers correctly is critical!
</details>

## 🔁 Anatomy of a Response

Now the server replies with another simple text message:

```
HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: 19

Hello from server!
```

| Part | Example | Meaning |
|:--|:--|:--|
| **Status Line** | `HTTP/1.1 200 OK` | Protocol + status code |
| **Headers** | `Content-Type: text/plain` | Response metadata |
| **Body** | `Hello from server!` | Actual content sent to client |

<div align="center">
  <button onclick="document.location='#curl-practice'">💻 Try it yourself</button>
</div>

<a id="curl-practice"></a>

### 💻 Quick Practice (using `curl`)

If your Lab 2 or Lab 3 server is running locally:

```bash
curl -v http://localhost:8080/students
```

This shows both the **request** and **response** — just like a conversation:

```
> GET /students HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.4.0
>
< HTTP/1.1 200 OK
< Content-Type: text/plain
< Content-Length: 25
<
# Students (3)
0: Ana Popescu
1: Bogdan Ionescu
```

Try also:
```bash
curl -X POST http://localhost:8080/students -d "Ion Vasilescu"
```

Each request you send is just **text**, and your server simply **reads it from the socket**.

## 🧭 Visual Summary

<div align="center">
  <img src="https://algomaster.io/_next/image?url=https%3A%2F%2Fpayload.algomaster.io%2Fapi%2Fmedia%2Ffile%2Flsd-1302-1-dark.png&w=3840&q=90" width="450"/>
  <p><i>HTTP = client speaks first → server replies → connection closes.</i></p>
</div>

# 3. Building a Simple Raw Server

So far, you’ve seen how HTTP messages look.  
Now it’s time to **build a server that speaks HTTP** — using only `ServerSocket` and `ExecutorService`.

You’ll start small: a server that always replies  
`"Hello from Java HTTP Server!"`, no matter the request.

Then, step by step, you’ll evolve it to handle real routes (`/`, `/students`, `/count`, etc.).

## ⚙️ Step 1 – Accepting Connections

Every HTTP server begins with a **TCP socket** that listens for clients.

```java
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class SimpleHttpServer {
    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(8080);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        System.out.println("Server running on http://localhost:8080");

        while (true) {
            Socket client = server.accept(); // waits for connection
            pool.execute(() -> handle(client)); // handle in thread pool
        }
    }

    static void handle(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream())) {

            String line = in.readLine(); // first line = request
            System.out.println("Request: " + line);

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/plain");
            out.println();
            out.println("Hello from Java HTTP Server!");
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

💡 Try running it and then open  
👉 [http://localhost:8080](http://localhost:8080) in your browser.

<details>
<summary>💬 <b>What’s happening step-by-step?</b></summary>

1. `ServerSocket.accept()` blocks until a client connects.  
2. When it does, you get a new `Socket`.  
3. You send it to a **thread pool** (`pool.execute()`), so multiple clients can connect at once.  
4. Inside `handle()`, you:
   - read the request line (`GET / HTTP/1.1`);
   - write back an HTTP response (status, headers, blank line, body);
   - flush and close the stream.
</details>

## 🧩 Step 2 – Reading and Understanding the Request

Try to print more than just the first line.  
After `in.readLine()`, continue reading **until you reach an empty line** (that marks the end of headers):

```java
String line;
while ((line = in.readLine()) != null && !line.isEmpty()) {
    System.out.println("Header: " + line);
}
```

This will show you the real headers sent by your browser or `curl`.

> 🧠 Pro tip: combine this with `Thread.currentThread().getName()`  
> to see which thread handles which request!

## 🧪 Step 3 – Testing in Parallel

Run your server and execute these in separate terminals:

```bash
curl http://localhost:8080/
curl http://localhost:8080/
curl http://localhost:8080/
```

You’ll see multiple `handle()` calls happening **on different threads**.

Example output:
```
Request: GET / HTTP/1.1
Request: GET / HTTP/1.1
Request: GET / HTTP/1.1
[/pool-1-thread-1]
[/pool-1-thread-2]
[/pool-1-thread-3]
```

<details>
<summary>🧠 <b>Why not one thread per client?</b></summary>

Creating a new thread per client may work for a few connections,  
but it’s inefficient for hundreds or thousands of requests.  
Using a **fixed thread pool** keeps resource usage stable and predictable.
</details>

## 🧰 Step 4 – Experiment and Extend

Now that your server works, try extending it:

| Task | Hint |
|:--|:--|
| Return a custom message | Replace `"Hello from Java HTTP Server!"` |
| Return current time | `out.println(LocalTime.now());` |
| Count requests | Use an `AtomicInteger` |
| Sleep endpoint | Handle `/sleep?ms=2000` and `Thread.sleep(ms)` |

Each experiment helps you understand **how a real HTTP server behaves internally**.

# 4. Routing & Handlers

At this point, your server can accept multiple connections and reply to simple requests.  
Now it’s time to make it **smart enough to handle different paths** — like `/`, `/students`, `/count`, etc.

We’ll introduce a lightweight concept called a **Router**, which maps paths to Java functions.

## 🧭 Step 1 – Manual Routing

For now, let’s use a simple `if` chain inside your `handle()` method.

```java
static void handle(Socket client) {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
         PrintWriter out = new PrintWriter(client.getOutputStream())) {

        String requestLine = in.readLine(); // e.g. "GET /students HTTP/1.1"
        System.out.println("Request: " + requestLine);

        // Extract method and path
        String[] parts = requestLine.split(" ");
        String method = parts[0];
        String path = parts[1];

        String body;
        if (path.equals("/")) {
            body = "Welcome to the Mini Student Server!";
        } else if (path.equals("/students")) {
            body = "Here we will list students.";
        } else if (path.equals("/count")) {
            body = "There are currently X students.";
        } else {
            body = "404 Not Found";
        }

        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/plain; charset=utf-8");
        out.println("Content-Length: " + body.length());
        out.println();
        out.println(body);
        out.flush();

    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

💡 This is **manual routing** — fast to write, but not scalable.

<details>
<summary>⚙️ <b>How routing works conceptually</b></summary>

Every HTTP server follows this logic:

1. Parse the **path** from the request line (`/students`, `/count`, etc.);
2. Match it against a list of known routes;
3. Call the corresponding **handler** function.

We’ll build a reusable Router next to make this cleaner.
</details>

## 🧱 Step 2 – A Minimal Router Class

Create a helper class to manage routes cleanly.

```java
import java.util.*;

public class Router {
    private final Map<String, Handler> getRoutes = new HashMap<>();

    public void get(String path, Handler handler) {
        getRoutes.put(path, handler);
    }

    public Handler find(String method, String path) {
        if ("GET".equalsIgnoreCase(method)) {
            return getRoutes.getOrDefault(path, null);
        }
        return null;
    }
}
```

And define a functional interface:

```java
@FunctionalInterface
public interface Handler {
    void handle(PrintWriter out);
}
```

Now your server setup becomes:

```java
Router router = new Router();
router.get("/", out -> out.println("Hello from /"));
router.get("/students", out -> out.println("List of students"));
router.get("/count", out -> out.println("3 students total"));
```

Then in `handle()`:

```java
Handler h = router.find(method, path);
if (h != null) {
    out.println("HTTP/1.1 200 OK");
    out.println("Content-Type: text/plain");
    out.println();
    h.handle(out);
} else {
    out.println("HTTP/1.1 404 Not Found");
    out.println("Content-Type: text/plain");
    out.println();
    out.println("Not Found");
}
out.flush();
```

✅ Congratulations — you now have a **tiny routing system**.

<details>
<summary>🧩 <b>Advantages of a router</b></summary>

- Keeps `handle()` clean and short  
- Easier to add new endpoints  
- Makes testing individual routes simpler  
- Looks more like real frameworks (Spring Boot, Express.js)
</details>

## 🧰 Step 3 – Integrating with StudentStore

Once your routing works, you can reuse the logic from Lab 2 for the `/students` and `/count` routes.

Example:

```java
router.get("/count", out -> {
    try {
        int n = store.count();
        out.println("Total students: " + n);
    } catch (Exception e) {
        out.println("Error reading file");
    }
});
```

> 💡 Tip: keep `FileStudentStore` synchronized, since multiple threads may call it at once.

## 🧠 Step 4 – Observe Thread Behavior

Add a debug line to each handler:

```java
System.out.println("[Thread] " + Thread.currentThread().getName());
```

Then open multiple tabs in your browser to `http://localhost:8080/students`.  
You’ll see different threads processing requests in parallel.

# 5. Connecting with StudentStore

Now that your router works, let’s **connect it with the Student storage logic** from Lab 2.  
This is where your mini HTTP server starts becoming a real web service!

## 🧩 Step 1 – Reuse Lab 2 Classes

You already have these from Lab 2:
- `Student` (model)
- `FileStudentStore` (file-based storage)
- `StudentStore` (interface)

All you need is to create the store and pass it to your handlers.

```java
String projectDir = System.getProperty("user.dir");
Path dataFile = Path.of(projectDir, "students.txt");
StudentStore store = new FileStudentStore(dataFile);
```

Now your server has persistent storage across requests.

<details>
<summary>💡 <b>Why reuse FileStudentStore?</b></summary>
Because it’s already thread-safe (synchronized) and uses atomic file writes.  
This means multiple threads can add or read students safely.
</details>

## ⚙️ Step 2 – Add Real Handlers

Replace the simple print handlers with logic that uses your `store`.

```java
router.get("/students", out -> {
    try {
        var students = store.list();
        if (students.isEmpty()) {
            out.println("(empty)");
        } else {
            for (int i = 0; i < students.size(); i++) {
                var s = students.get(i);
                out.println(i + ": " + s.firstName() + " " + s.lastName());
            }
        }
    } catch (Exception e) {
        out.println("Error reading students: " + e.getMessage());
    }
});

router.get("/count", out -> {
    try {
        out.println("Total students: " + store.count());
    } catch (Exception e) {
        out.println("Error: " + e.getMessage());
    }
});
```

✅ You now have two working endpoints that serve real data!

<details>
<summary>🧠 <b>Optional – Add POST and DELETE support</b></summary>

You can handle `POST /students` manually inside `handle()`:

```java
if ("POST".equals(method) && path.equals("/students")) {
    String line;
    List<String> names = new ArrayList<>();
    while ((line = in.readLine()) != null && !line.isEmpty()) {
        names.add(line.trim());
    }
    store.addAll(names);
    out.println("HTTP/1.1 200 OK");
    out.println("Content-Type: text/plain");
    out.println();
    out.println("Added " + names.size() + " students.");
    out.flush();
}
```

You can also implement `DELETE /students?i=<index>` to remove entries.
</details>

## 🧠 Step 3 – Test the Integration

Run your server, then use `curl` to test all routes:

```bash
curl http://localhost:8080/students
curl http://localhost:8080/count
curl -X POST http://localhost:8080/students -d "Ana Popescu"
curl http://localhost:8080/students
```

You should see the new student added and counted.

> Tip: you can open the `students.txt` file while the server runs to see it updating live!

## 🧮 Step 4 – Observe Concurrency

To simulate concurrent requests, open several terminals:

```bash
curl http://localhost:8080/count &
curl http://localhost:8080/students &
curl -X POST http://localhost:8080/students -d "Ion Vasilescu" &
```

You’ll see that your thread pool handles all these in parallel without corrupting data.

<details>
<summary>🔍 <b>How FileStudentStore stays safe</b></summary>

It uses a private lock object and synchronizes file operations:

```java
private final Object lock = new Object();

public List<Student> list() throws IOException {
    synchronized (lock) {
        // safely read file here
    }
}
```

This ensures that even if 3 users add students at the same time, the file never breaks.
</details>

## ✅ Step 5 – What You Achieved

By now, your custom HTTP server can:
- accept real connections over TCP,
- parse HTTP requests,
- route them to the right handlers,
- access persistent data safely through FileStudentStore,
- and serve multiple clients in parallel.

You have built a **mini web framework** — completely from scratch! 🎉

# 6. Concurrency in Action

Your server now serves multiple clients, but **how exactly does concurrency work** behind the scenes?  
In this chapter, we’ll explore **thread pools**, how they share work, and how to visualize what’s happening.

## ⚙️ Step 1 – Thread Pools Refresher

In your main server setup, you wrote:

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
```

This creates a pool of **4 worker threads** that will handle client connections in parallel.

Each time a new request arrives:
1. The main thread accepts the socket;
2. It submits a new task to the pool (`pool.execute(() -> handle(client));`);
3. One of the 4 threads takes the job and runs `handle()`;
4. When finished, that thread becomes free for another client.

<details>
<summary>💡 <b>Visual model of the thread pool</b></summary>

```
Incoming requests
   ↓
+--------------------+
|  Executor Service  |
+--------------------+
   ↓     ↓     ↓     ↓
[worker-1] [worker-2] [worker-3] [worker-4]
       ↳ handle(socket1)
             ↳ handle(socket2)
```
</details>

## 🧠 Step 2 – Identify Threads in Logs

Add this line to each handler:

```java
System.out.println("[Thread] " + Thread.currentThread().getName());
```

Then open multiple browser tabs or run:

```bash
curl http://localhost:8080/students &
curl http://localhost:8080/count &
curl http://localhost:8080/ &
```

You’ll see log lines like:

```
[Thread] pool-1-thread-1
[Thread] pool-1-thread-3
[Thread] pool-1-thread-2
```

Each request runs on a different thread, yet the main process never blocks.

> 💡 You can adjust the number of threads: try 2, 4, or 8 and observe how it affects response times.

## 🧩 Step 3 – Simulate Workload (Sleep Endpoint)

To really see concurrency, add a `/sleep` route that pauses for a few seconds.

```java
router.get("/sleep", out -> {
    try {
        out.println("Starting sleep on " + Thread.currentThread().getName());
        Thread.sleep(3000); // 3 seconds
        out.println("Woke up on " + Thread.currentThread().getName());
    } catch (InterruptedException e) {
        out.println("Interrupted");
    }
});
```

Now run these at the same time:

```bash
curl http://localhost:8080/sleep &
curl http://localhost:8080/sleep &
curl http://localhost:8080/sleep &
```

They’ll all sleep **concurrently**, handled by different threads.

<details>
<summary>🧩 <b>What’s the lesson?</b></summary>

Even if one request is slow, others still get processed in parallel.  
That’s the power of thread pools in a web server — responsiveness and scalability.
</details>

## 🔍 Step 4 – Controlling Shutdown

You can stop your server gracefully with a **shutdown hook**.

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    System.out.println("Shutting down...");
    pool.shutdown();
}));
```

When you press `Ctrl+C`, the server stops accepting new connections and cleans up active threads.

## 🧮 Step 5 – Optional: Count Active Threads

To inspect the pool’s current workload:

```java
if (pool instanceof ThreadPoolExecutor exec) {
    System.out.println("Active threads: " + exec.getActiveCount());
}
```

This helps you visualize how many threads are busy at a time.

## ✅ Summary

By now you have seen how:
- each request runs on its own thread,
- the thread pool limits concurrency safely,
- and your server stays responsive even under multiple connections.

You’ve implemented a **mini parallel web server** — just like the foundation of real frameworks!

# 7. Lab Tasks

Now that you’ve built and understood your own raw HTTP server, it’s time to **apply what you learned**.  
In this lab, you’ll extend your mini framework with real features and experiment with concurrency.

---

## 🎯 Task 1 – Implement `GET /count`

Create a route that returns the number of students stored in the file.

```java
router.get("/count", out -> {
    try {
        out.println("Total students: " + store.count());
    } catch (Exception e) {
        out.println("Error: " + e.getMessage());
    }
});
```

✅ **Expected output:**
```
Total students: 3
```

<details>
<summary>💡 Hint</summary>
You can reuse the logic from your `FileStudentStore` and just call `.count()`.  
Remember: this method is synchronized internally, so it’s thread-safe.
</details>

---

## ✏️ Task 2 – Implement `DELETE /students`

Add support for removing students:

- `DELETE /students` → clears all students  
- `DELETE /students?i=<index>` → deletes only one student

You can read query parameters manually from the path string.

✅ **Example:**
```
DELETE /students?i=1 HTTP/1.1
→ Student at index 1 removed.
```

💡 Hint: Use `store.deleteByIndex()` or `store.clear()` accordingly.

---

## 🧩 Task 3 – Improve the Welcome Page (`/`)

Make the root route (`/`) display a help message for users.

```java
router.get("/", out -> {
    out.println("Student Manager - Mini HTTP Server");
    out.println("Available endpoints:");
    out.println("GET  /students   - list all students");
    out.println("POST /students   - add students (plain text)");
    out.println("GET  /count      - total number of students");
    out.println("DELETE /students - remove one or all students");
});
```

This helps you test everything more easily in the browser.

---

## 🧮 Task 4 – Add Logging per Thread

Add a small log line for every handled request:

```java
System.out.println("[Thread] " + Thread.currentThread().getName() + " handled " + path);
```

Then open multiple terminals to make concurrent requests with `curl` and observe how threads work together.

---

## 🚀 Task 5 – Bonus: `HEAD` and `PUT`

### a) `HEAD /students`
Send only headers, no body — for checking if an endpoint is available.

### b) `PUT /students?i=<index>`
Update the student at the given index (e.g. replace name).

Both are optional, but give you a taste of more HTTP verbs!

---

## 🧠 Task 6 – Stress Test the Thread Pool

Try changing your thread pool size and see how it affects concurrency:

```java
ExecutorService pool = Executors.newFixedThreadPool(2); // or 8
```

Then run 5–10 curl commands at once:

```bash
for i in {1..10}; do curl http://localhost:8080/sleep & done
```

Observe how only a few run at a time — that’s how **load limiting** works!

# 8. Testing & Debugging

Once your server is functional, it’s important to verify that all routes behave correctly — and to understand how to debug typical HTTP issues.  
This chapter will help you **test your server like a DevOps engineer** using `curl`, browsers, and logs.

## 🧩 Step 1 – Using `curl`

`curl` is a powerful command-line tool for making HTTP requests.

| Action | Example Command | Description |
|:--|:--|:--|
| List students | `curl http://localhost:8080/students` | GET request |
| Add student | `curl -X POST http://localhost:8080/students -d "Ana Popescu"` | Sends body text |
| Delete student | `curl -X DELETE http://localhost:8080/students?i=0` | Deletes index 0 |
| Clear all | `curl -X DELETE http://localhost:8080/students` | Removes all students |
| Count students | `curl http://localhost:8080/count` | Shows number of entries |

Try running `curl -v` (verbose mode) to see the raw HTTP exchange:

```bash
curl -v http://localhost:8080/students
```

You’ll see something like this:

```
> GET /students HTTP/1.1
> Host: localhost:8080
>
< HTTP/1.1 200 OK
< Content-Type: text/plain
<
0: Ana Popescu
1: Bogdan Ionescu
```

<details>
<summary>💡 <b>Tip – Save response to a file</b></summary>

```bash
curl http://localhost:8080/students -o result.txt
```
This saves the server’s output for inspection or debugging later.
</details>

## 🧠 Step 2 – Browser Testing

You can also test GET routes directly in your browser:

- [http://localhost:8080/](http://localhost:8080/) → should show help text  
- [http://localhost:8080/students](http://localhost:8080/students) → list students  
- [http://localhost:8080/count](http://localhost:8080/count) → show count  

> 💡 For POST and DELETE, use `curl` or Postman since browsers can’t easily send those verbs manually.

## 🔍 Step 3 – Debugging Common Issues

| Symptom | Possible Cause | Fix |
|:--|:--|:--|
| No response in browser | Missing blank line before body | Ensure `out.println()` adds empty line after headers |
| garbled text | Missing `charset=utf-8` | Add `Content-Type: text/plain; charset=utf-8` |
| server crash | `ArrayIndexOutOfBounds` | Check request parsing (`split(" ")`) |
| connection refused | Port already in use | Stop old process or change port |
| file corrupted | race conditions | Ensure `FileStudentStore` remains synchronized |

<details>
<summary>⚙️ <b>How to inspect running threads</b></summary>

In IntelliJ or VSCode debugger, set a breakpoint in `handle()` and inspect:
- thread name (`Thread.currentThread().getName()`),
- number of open sockets,
- active requests in the pool.
</details>

## 🧩 Step 4 – Testing Concurrent Requests

To test concurrency, run:

```bash
for i in {1..5}; do curl http://localhost:8080/sleep & done
wait
```

Expected output (interleaved):

```
Starting sleep on pool-1-thread-2
Starting sleep on pool-1-thread-1
Starting sleep on pool-1-thread-3
Woke up on pool-1-thread-1
Woke up on pool-1-thread-3
Woke up on pool-1-thread-2
```

This confirms your thread pool handles multiple clients simultaneously.

## ✅ Step 5 – Clean Exit

Press `Ctrl + C` to stop the server.  
If you implemented the shutdown hook:

```
Shutting down...
```
will appear — confirming a graceful shutdown and resource cleanup.

## 🧠 Step 6 – Debug Checklist

✅ Every request prints the thread name.  
✅ Headers include a blank line before the body.  
✅ File operations are synchronized.  
✅ No exception is left unhandled.  
✅ `students.txt` is readable and updated.

Once all these pass, your raw HTTP server is stable and production-ready for demo.

# 9. Optional Challenges

You’ve reached the end of Lab 3 — congratulations! 🎉  
At this point, you’ve built a fully working **HTTP server with routing, storage, and concurrency**, entirely from scratch.

This final chapter offers a few **optional extensions** for those who want to explore more advanced ideas.

## 🧩 Challenge 1 – Add JSON Support

So far, your server has used plain text.  
Try returning JSON instead, which is the standard for APIs.

Example:

```java
router.get("/students.json", out -> {
    try {
        var students = store.list();
        out.println("[");
        for (int i = 0; i < students.size(); i++) {
            var s = students.get(i);
            out.print("  {\"firstName\": \"" + s.firstName() + "\", \"lastName\": \"" + s.lastName() + "\"}");
            if (i < students.size() - 1) out.println(",");
        }
        out.println("\n]");
    } catch (Exception e) {
        out.println("[]");
    }
});
```

Then access it from your browser:  
👉 [http://localhost:8080/students.json](http://localhost:8080/students.json)

<details>
<summary>💡 <b>Tip</b></summary>
Change the header to JSON format:
```java
out.println("Content-Type: application/json; charset=utf-8");
```
</details>

## ⚙️ Challenge 2 – Implement `HEAD` and `OPTIONS`

- `HEAD` → should send headers but **no body**.  
- `OPTIONS` → list supported HTTP methods.

This helps mimic behavior of real servers (e.g., for REST clients).

Example response for `OPTIONS /students`:

```
HTTP/1.1 204 No Content
Allow: GET, POST, DELETE, OPTIONS
```

## 📦 Challenge 3 – Add Configuration via `config.properties`

Instead of hardcoding the port and thread count, load them from a file:

```
port=8080
threads=4
```

Then in Java:

```java
Properties p = new Properties();
p.load(new FileInputStream("config.properties"));
int port = Integer.parseInt(p.getProperty("port", "8080"));
int threads = Integer.parseInt(p.getProperty("threads", "4"));
```

This simulates how real microservices use configuration files.

## 🧠 Challenge 4 – Static File Server

Serve local files like a real web server:

```java
router.get("/files", out -> {
    try {
        Path p = Path.of("students.txt");
        Files.lines(p).forEach(out::println);
    } catch (IOException e) {
        out.println("File not found");
    }
});
```

> 💡 Bonus: handle query parameters like `/files?name=data.txt` for dynamic file serving.

## 🔍 Challenge 5 – Benchmark and Compare

Use a tool like **ab** (Apache Benchmark) or **wrk** to test how many requests per second your server can handle.

Example:

```bash
ab -n 100 -c 10 http://localhost:8080/students
```

Compare performance with:
- your raw server,
- Java’s built-in `HttpServer`,
- and maybe a tiny framework like Spark Java.

This gives you a sense of **how real servers scale**.

## 🧩 Challenge 6 – Visualize Thread Usage (Bonus)

Add a route `/debug/threads` that prints all thread names currently alive:

```java
router.get("/debug/threads", out -> {
    Set<Thread> threads = Thread.getAllStackTraces().keySet();
    threads.forEach(t -> out.println(t.getName()));
});
```

It’s a cool way to observe your runtime environment.

---

## ✅ End of Lab 3

You’ve implemented a fully functional HTTP server with:
- sockets and I/O streams,  
- routing and handlers,  
- thread pools and concurrency,  
- persistent file storage,  
- and live debugging techniques.

That’s a huge step toward understanding **how frameworks like Spring Boot or Express.js work internally.**

🎓 **Homework:** No homework for this lab.  
Enjoy your well-earned break — you’ve built a web server from scratch! 😎


