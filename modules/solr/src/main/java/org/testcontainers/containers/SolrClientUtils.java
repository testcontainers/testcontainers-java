package org.testcontainers.containers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

/**
 * Utils class which can create collections and configurations.
 *
 * @author Simon Schneider
 */
public class SolrClientUtils {

    private static HttpClient httpClient = HttpClientBuilder.create().build();

    /**
     * Creates a new configuration and uploads the solrconfig.xml and schema.xml
     *
     * @param port              the Port on which solr is running
     * @param configurationName the name of the configuration which should be created
     * @param solrConfig        the url under which the solrconfig.xml can be found
     * @param solrSchema        the url under which the schema.xml can be found or null if the default schema should be used
     */
    public static void uploadConfiguration(int port, String configurationName, URL solrConfig, URL solrSchema) throws URISyntaxException, IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("action", "UPLOAD");
        parameters.put("name", configurationName);
        URI uri = generateSolrURI(port, Arrays.asList("admin", "configs"), parameters);

        byte[] configurationZipFile = generateConfigZipFile(solrConfig, solrSchema);
        executePost(uri, configurationZipFile);

    }

    /**
     * Creates a new collection
     *
     * @param port              The Port on which solr is running
     * @param collectionName    the name of the collection which should be created
     * @param configurationName the name of the configuration which should used to create the collection
     *                          or null if the default configuration should be used
     */
    public static void createCollection(int port, String collectionName, String configurationName) throws URISyntaxException, IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("action", "CREATE");
        parameters.put("name", collectionName);
        parameters.put("numShards", "1");
        parameters.put("replicationFactor", "1");
        parameters.put("wt", "json");
        if (configurationName != null) {
            parameters.put("collection.configName", configurationName);
        }
        URI uri = generateSolrURI(port, Arrays.asList("admin", "collections"), parameters);
        executePost(uri, null);
    }

    private static void executePost(URI uri, byte[] data) throws IOException {
        HttpPost request = new HttpPost(uri);
        if (data != null) {
            request.setEntity(new ByteArrayEntity(data, ContentType.create("application/octet-stream")));
        }
        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() != 200) {
            InputStream responseBody = ((HttpEntityWrapper) response.getEntity()).getContent();
            String textBody = IOUtils.toString(responseBody, StandardCharsets.UTF_8);
            throw new HttpResponseException(response.getStatusLine().getStatusCode(), "Unable to upload binary\n" + textBody);
        }
    }

    private static URI generateSolrURI(int port, List<String> pathSegments, Map<String, String> parameters) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http");
        builder.setHost("localhost");
        builder.setPort(port);
        // Path
        List<String> basePathSegments = new ArrayList<>();
        basePathSegments.add("solr");
        basePathSegments.addAll(pathSegments);
        builder.setPathSegments(basePathSegments);
        // Query Parameters
        builder.setParameters(parameters.entrySet()
            .stream()
            .map(item -> new BasicNameValuePair(item.getKey(), item.getValue()))
            .collect(Collectors.toList()));
        return builder.build();
    }

    private static byte[] generateConfigZipFile(URL solrConfiguration, URL solrSchema) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(bos);
        // SolrConfig
        zipOutputStream.putNextEntry(new ZipEntry("solrconfig.xml"));
        IOUtils.copy(solrConfiguration.openStream(), zipOutputStream);
        zipOutputStream.closeEntry();

        // Solr Schema
        if (solrSchema != null) {
            zipOutputStream.putNextEntry(new ZipEntry("schema.xml"));
            IOUtils.copy(solrSchema.openStream(), zipOutputStream);
            zipOutputStream.closeEntry();
        }

        zipOutputStream.close();
        return bos.toByteArray();
    }
}
