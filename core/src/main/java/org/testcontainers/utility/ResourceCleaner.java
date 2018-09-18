package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.util.FiltersBuilder;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.*;

public class ResourceCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCleaner.class);
    private static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");
    private static final byte[] ACK = "ACK\n".getBytes();

    public static void main(String[] args) {
        if (args.length != 1) {
            LOGGER.error("Invalid number of arguments: should be specified port number");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        DockerClient client = DockerClientFactory.instance().client();
        List<Map<String, List<String>>> registeredFilters = new ArrayList<>();

        Set<String> removedContainers = new HashSet<>();
        Set<String> removedNetworks = new HashSet<>();
        Set<String> removedVolumes = new HashSet<>();

        try (Socket socket = new Socket("localhost", portNumber);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream outputStream = socket.getOutputStream()) {
            LOGGER.info("Connected to testcontainers on 'localhost:{}'", portNumber);
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) continue;

                List<NameValuePair> pairs = URLEncodedUtils.parse(line, CHARSET_UTF_8);
                FiltersBuilder filtersBuilder = new FiltersBuilder();
                pairs.forEach(entry -> filtersBuilder.withFilter(entry.getName(), entry.getValue()));
                registeredFilters.add(filtersBuilder.build());

                outputStream.write(ACK);
                outputStream.flush();
            }
        } catch (IOException e) {
            LOGGER.info("Connection to 'localhost:{}' was closed: {}", args[0], e.getMessage());
        } finally {
            // Cleanup resources and exit
            LOGGER.debug("Cleaning resources...");
            registeredFilters.forEach(filter -> {
                LOGGER.debug("Deleting resources matching filter {}", filter);
                DockerClientUtil.removeResources(client, filter, removedContainers, removedNetworks, removedVolumes);
            });
            LOGGER.info(
                "Removed {} container(s), {} network(s), {} volume(s)\n",
                removedContainers.size(),
                removedNetworks.size(),
                removedVolumes.size()
            );
        }
    }
}
