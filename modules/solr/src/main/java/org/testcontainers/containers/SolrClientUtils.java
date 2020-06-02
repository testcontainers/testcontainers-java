package org.testcontainers.containers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Utils class which can create collections and configurations.
 *
 * @author Simon Schneider
 */
public class SolrClientUtils {

    private static OkHttpClient httpClient = new OkHttpClient();

    /**
     * Creates a new configuration and uploads the solrconfig.xml and schema.xml
     *
     * @param hostname          the Hostname under which solr is reachable
     * @param port              the Port on which solr is running
     * @param configuration     the configuration object for the solr container
     */
    public static void uploadConfiguration(String hostname, int port, SolrContainerConfiguration configuration) throws URISyntaxException, IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("action", "UPLOAD");
        parameters.put("name", configuration.getConfigurationName());
        HttpUrl url = generateSolrURL(hostname, port, Arrays.asList("admin", "configs"), parameters);

        byte[] configurationZipFile = generateConfigZipFile(configuration);
        executePost(url, configurationZipFile);

    }

    /**
     * Creates a new collection
     *
     * @param hostname          the Hostname under which solr is reachable
     * @param port              The Port on which solr is running
     * @param collectionName    the name of the collection which should be created
     * @param configurationName the name of the configuration which should used to create the collection
     *                          or null if the default configuration should be used
     */
    public static void createCollection(String hostname, int port, String collectionName, String configurationName) throws URISyntaxException, IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("action", "CREATE");
        parameters.put("name", collectionName);
        parameters.put("numShards", "1");
        parameters.put("replicationFactor", "1");
        parameters.put("wt", "json");
        if (configurationName != null) {
            parameters.put("collection.configName", configurationName);
        }
        HttpUrl url = generateSolrURL(hostname, port, Arrays.asList("admin", "collections"), parameters);
        executePost(url, null);
    }

    private static void executePost(HttpUrl url, byte[] data) throws IOException {

        RequestBody requestBody = data == null ?
            RequestBody.create(MediaType.parse("text/plain"), "") :
            RequestBody.create(MediaType.parse("application/octet-stream"), data);
        ;

        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
            .build();
        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            String responseBody = "";
            if (response.body() != null) {
                responseBody = response.body().string();
                response.close();
            }
            throw new SolrClientUtilsException(response.code(), "Unable to upload binary\n" + responseBody);
        }
        if (response.body() != null) {
            response.close();
        }
    }

    private static HttpUrl generateSolrURL(String hostname, int port, List<String> pathSegments, Map<String, String> parameters) throws URISyntaxException {
        HttpUrl.Builder builder = new HttpUrl.Builder();
        builder.scheme("http");
        builder.host(hostname);
        builder.port(port);
        // Path
        builder.addPathSegment("solr");
        if (pathSegments != null) {
            pathSegments.forEach(builder::addPathSegment);
        }
        // Query Parameters
        parameters.forEach(builder::addQueryParameter);
        return builder.build();
    }

    private static byte[] generateConfigZipFile(SolrContainerConfiguration configuration) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(bos);
        Map<String, URL> resources = configuration.getResources();
        List<String> directories = resources.keySet().stream()
                                           .map(item -> item.split("/"))
                                           .filter(item -> item.length >= 2)
                                           .map(Arrays::asList)
                                           .map(item -> item.subList(0, item.size() - 1))
                                           .flatMap(Collection::stream)
                                           .map(item -> item + "/")
                                           .distinct()
                                           .collect(Collectors.toList());
        for (String directory : directories) {
            zipOutputStream.putNextEntry(new ZipEntry(directory));
        }

        for (Map.Entry<String, URL> entry : resources.entrySet()) {
            zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
            IOUtils.copy(entry.getValue().openStream(), zipOutputStream);
            zipOutputStream.closeEntry();
        }
        zipOutputStream.close();
        return bos.toByteArray();
    }
}
