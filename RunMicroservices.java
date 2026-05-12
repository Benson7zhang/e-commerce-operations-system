import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RunMicroservices {

    private static final String HOST = "127.0.0.1";
    private static final int FRONTEND_PORT = 5173;
    private static final String APP_VERSION = "0.0.1-SNAPSHOT";

    private static final Path PROJECT_ROOT = locateProjectRoot();
    private static final Path JAVA_BACKEND_DIR = PROJECT_ROOT.resolve("java-backend");
    private static final Path FRONTEND_DIR = PROJECT_ROOT.resolve("frontend");
    private static final Path LOG_DIR = PROJECT_ROOT.resolve("logs");
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    private static final String JAVA_EXE = Path.of(System.getProperty("java.home"), "bin", WINDOWS ? "java.exe" : "java").toString();
    private static final String MAVEN_WRAPPER = WINDOWS ? "mvnw.cmd" : "./mvnw";

    private record Service(String name, int port) {
    }

    private record ManagedProcess(String name, Process process) {
    }

    private static final List<Service> SERVICES = List.of(
            new Service("product-service", 9001),
            new Service("order-service", 9002),
            new Service("inventory-service", 9003),
            new Service("return-service", 9004),
            new Service("supplier-service", 9005),
            new Service("warehouse-service", 9006),
            new Service("stats-service", 9007),
            new Service("gateway-service", 9000)
    );

    public static void main(String[] args) {
        List<ManagedProcess> managed = new ArrayList<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> terminateAll(managed)));

        try {
            ensurePrerequisites();
            buildBackend();

            for (int index = 0; index < SERVICES.size() - 1; index++) {
                Service service = SERVICES.get(index);
                ManagedProcess process = startService(service);
                if (process != null) {
                    managed.add(process);
                }
                sleep(Duration.ofSeconds(1));
            }

            ManagedProcess gateway = startService(SERVICES.get(SERVICES.size() - 1));
            if (gateway != null) {
                managed.add(gateway);
            }
            ManagedProcess frontend = startFrontend();
            if (frontend != null) {
                managed.add(frontend);
            }

            System.out.println();
            System.out.println("Java 微服务联调环境已启动");
            System.out.println("前端:  http://" + HOST + ":" + FRONTEND_PORT);
            System.out.println("网关:  http://" + HOST + ":9000");
            System.out.println("健康:  http://" + HOST + ":9000/health");
            System.out.println("按 Ctrl+C 停止本次启动的进程");

            while (true) {
                sleep(Duration.ofSeconds(1));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("\n正在停止 Java 微服务联调环境...");
        } catch (Exception e) {
            System.err.println("[error] " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            terminateAll(managed);
        }
    }

    private static void ensurePrerequisites() {
        try {
            Files.createDirectories(LOG_DIR);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建日志目录: " + LOG_DIR, e);
        }
        if (!Files.exists(JAVA_BACKEND_DIR.resolve(MAVEN_WRAPPER))) {
            throw new IllegalStateException("未找到 Maven Wrapper: " + JAVA_BACKEND_DIR.resolve(MAVEN_WRAPPER));
        }
        if (commandExists("npm")) {
            return;
        }
        throw new IllegalStateException("未检测到 npm，请先安装 Node.js");
    }

    private static Path locateProjectRoot() {
        Path cwd = Paths.get("").toAbsolutePath();
        Path direct = cwd.resolve("java-backend");
        if (Files.exists(direct)) {
            return cwd;
        }

        Path nested = cwd.resolve("E_mall");
        if (Files.exists(nested.resolve("java-backend"))) {
            return nested;
        }

        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("E_mall").resolve("java-backend"))) {
            return parent.resolve("E_mall");
        }

        return cwd;
    }

    private static void buildBackend() throws IOException, InterruptedException {
        System.out.println("[build] 编译 Java 微服务...");
        ProcessBuilder builder = WINDOWS
                ? new ProcessBuilder("cmd.exe", "/c", MAVEN_WRAPPER, "-s", ".mvn\\settings.xml", "clean", "package", "-DskipTests")
                : new ProcessBuilder(MAVEN_WRAPPER, "-s", ".mvn/settings.xml", "clean", "package", "-DskipTests");
        builder.directory(JAVA_BACKEND_DIR.toFile());
        builder.inheritIO();
        Process process = builder.start();
        if (process.waitFor() != 0) {
            throw new IllegalStateException("Java 微服务构建失败");
        }
    }

    private static ManagedProcess startFrontend() throws IOException, InterruptedException {
        Path packageJson = FRONTEND_DIR.resolve("package.json");
        if (!Files.exists(packageJson)) {
            System.out.println("[warn] 未找到前端 package.json: " + packageJson);
            return null;
        }
        if (isPortOpen(HOST, FRONTEND_PORT)) {
            System.out.println("[skip] 前端开发服务器已运行: http://" + HOST + ":" + FRONTEND_PORT);
            return null;
        }

        System.out.println("[start] frontend -> http://" + HOST + ":" + FRONTEND_PORT);
        ProcessBuilder builder = WINDOWS
                ? new ProcessBuilder("cmd.exe", "/c", "npm", "run", "dev", "--", "--host", HOST, "--port", String.valueOf(FRONTEND_PORT))
                : new ProcessBuilder("npm", "run", "dev", "--", "--host", HOST, "--port", String.valueOf(FRONTEND_PORT));
        builder.directory(FRONTEND_DIR.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(LOG_DIR.resolve("frontend.log").toFile()));
        Process process = builder.start();
        if (!waitForPort(HOST, FRONTEND_PORT, Duration.ofSeconds(30))) {
            throw new IllegalStateException("前端开发服务器启动超时");
        }
        return new ManagedProcess("frontend", process);
    }

    private static ManagedProcess startService(Service service) throws IOException, InterruptedException {
        if (isPortOpen(HOST, service.port())) {
            System.out.println("[skip] " + service.name() + " 已运行: http://" + HOST + ":" + service.port());
            return null;
        }

        Path jarPath = JAVA_BACKEND_DIR.resolve(service.name()).resolve("target").resolve(service.name() + "-" + APP_VERSION + ".jar");
        if (!Files.exists(jarPath)) {
            throw new IllegalStateException("未找到可执行 jar: " + jarPath);
        }

        Files.createDirectories(LOG_DIR);
        System.out.println("[start] " + service.name() + " -> http://" + HOST + ":" + service.port());
        ProcessBuilder builder = new ProcessBuilder(JAVA_EXE, "-jar", jarPath.toString());
        builder.directory(JAVA_BACKEND_DIR.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(LOG_DIR.resolve(service.name() + ".log").toFile()));
        Process process = builder.start();
        if (!waitForPort(HOST, service.port(), Duration.ofSeconds(60))) {
            throw new IllegalStateException(service.name() + " 启动超时: " + HOST + ":" + service.port());
        }
        return new ManagedProcess(service.name(), process);
    }

    private static boolean commandExists(String command) {
        try {
            ProcessBuilder builder = WINDOWS
                    ? new ProcessBuilder("cmd.exe", "/c", "where", command)
                    : new ProcessBuilder("sh", "-lc", "command -v " + command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isPortOpen(String host, int port) {
        try (var socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 800);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean waitForPort(String host, int port, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (isPortOpen(host, port)) {
                return true;
            }
            Thread.sleep(500);
        }
        return false;
    }

    private static void terminateAll(List<ManagedProcess> managed) {
        for (int index = managed.size() - 1; index >= 0; index--) {
            ManagedProcess entry = managed.get(index);
            Process process = entry.process();
            if (process != null && process.isAlive()) {
                process.destroy();
                try {
                    if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    process.destroyForcibly();
                }
            }
        }
    }

    private static void sleep(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }
}
