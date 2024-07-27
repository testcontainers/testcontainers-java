///usr/bin/env jbang "$0" "$@" ; exit $?

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class EnvVariableRestEndpoint {
    private static final String ENV_VARIABLE_NAME = "MY_ENV_VARIABLE";
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/env", new EnvVariableHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + PORT);
    }

    static class EnvVariableHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String envValue = System.getenv(ENV_VARIABLE_NAME);
                String response = envValue != null
                    ? ENV_VARIABLE_NAME + ": " + envValue
                    : "Environment variable " + ENV_VARIABLE_NAME + " not found";

                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                String response = "Method not allowed";
                exchange.sendResponseHeaders(405, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}
