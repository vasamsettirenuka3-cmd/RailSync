package railsync.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import railsync.engine.Util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Starts the RailSync dashboard: run this class, then open http://localhost:8080 */
public final class Main {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static String webDir;

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) port = Integer.parseInt(args[0]);

        if (new File("web").isDirectory()) webDir = "web";
        else if (new File("../web").isDirectory()) webDir = "../web";
        else {
            System.err.println("Cannot find the 'web' folder. Run from the project root (the folder that contains 'web' and 'src').");
            return;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        server.createContext("/api/", new HttpHandler() {
            public void handle(HttpExchange ex) throws IOException {
                String out;
                int code = 200;
                try {
                    out = Api.handle(ex.getRequestURI().getPath(), new Params(ex.getRequestURI().getRawQuery()));
                } catch (Throwable t) {
                    code = 500;
                    out = "{\"error\":\"" + Util.esc(String.valueOf(t)) + "\"}";
                }
                send(ex, code, "application/json; charset=utf-8", out.getBytes(UTF8));
            }
        });

        server.createContext("/", new HttpHandler() {
            public void handle(HttpExchange ex) throws IOException {
                String path = ex.getRequestURI().getPath();
                if (path.equals("/")) path = "/index.html";
                if (path.contains("..")) {
                    send(ex, 400, "text/plain", "Bad path".getBytes(UTF8));
                    return;
                }
                File f = new File(webDir + path);
                if (!f.isFile()) {
                    send(ex, 404, "text/plain", "Not found".getBytes(UTF8));
                    return;
                }
                send(ex, 200, mime(path), Files.readAllBytes(Paths.get(f.getPath())));
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("RailSync is running -> http://localhost:" + port);
    }

    private static String mime(String p) {
        if (p.endsWith(".html")) return "text/html; charset=utf-8";
        if (p.endsWith(".css")) return "text/css; charset=utf-8";
        if (p.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, body.length);
        OutputStream os = ex.getResponseBody();
        os.write(body);
        os.close();
    }
}
