package org.testcontainers.containers;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;

final class KeyStoreBuilder {

    static KeyStore buildByDownloadingCertificate(String endpoint, String keyStorePassword) {
        OkHttpClient client = null;
        Response response = null;
        try {
            TrustManager[] trustAllManagers = buildTrustAllManagers();
            client = buildTrustAllClient(trustAllManagers);
            Request request = buildRequest(endpoint);
            response = client.newCall(request).execute();
            return buildKeyStore(response.body().byteStream(), keyStorePassword);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            closeResponseSilently(response);
            closeClientSilently(client);
        }
    }

    private static TrustManager[] buildTrustAllManagers() {
        return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };
    }

    private static OkHttpClient buildTrustAllClient(TrustManager[] trustManagers) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustManagers, new SecureRandom());
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        return new OkHttpClient.Builder()
                .sslSocketFactory(socketFactory, (X509TrustManager) trustManagers[0])
                .hostnameVerifier((s, sslSession) -> true)
                .build();
    }

    private static Request buildRequest(String endpoint) {
        return new Request.Builder()
                .get()
                .url(endpoint + "/_explorer/emulator.pem")
                .build();
    }

    private static KeyStore buildKeyStore(InputStream certificateStream, String keyStorePassword) throws Exception {
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(certificateStream);
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(null, keyStorePassword.toCharArray());
        keystore.setCertificateEntry("azure-cosmos-emulator", certificate);
        return keystore;
    }

    private static void closeResponseSilently(Response response) {
        try {
            if (Objects.nonNull(response)) {
                response.close();
            }
        } catch (Exception ignored) {
        }
    }

    private static void closeClientSilently(OkHttpClient client) {
        try {
            if (Objects.nonNull(client)) {
                client.dispatcher().executorService().shutdown();
                client.connectionPool().evictAll();
                Cache cache = client.cache();
                if (Objects.nonNull(cache)) {
                    cache.close();
                }
            }
        } catch (Exception ignored) {
        }
    }
}
