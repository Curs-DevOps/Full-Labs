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
        System.out.println("Students file path: " + dataFile.toAbsolutePath());

        var store = new FileStudentStore(dataFile);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/students", new StudentHandler(store));

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
