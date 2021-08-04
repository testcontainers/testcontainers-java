package org.testcontainers.containers;

import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;

final class KeyStoreBuilder {

    static KeyStore buildByDownloadingCertificate(String endpoint, String keyStorePassword) {
        TrustManager[] trustAllManagers = new TrustManager[] {
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
        OkHttpClient client = null;
        Response response = null;
        try {
            client = new OkHttpClient.Builder()
                    .sslSocketFactory(getTrustAllSSLSocketFactory(trustAllManagers), (X509TrustManager) trustAllManagers[0])
                    .hostnameVerifier((s, sslSession) -> true)
                    .build();
            Request request = new Request.Builder()
                    .get()
                    .url(endpoint + "/_explorer/emulator.pem")
                    .build();
            response = client.newCall(request).execute();
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(response.body().byteStream());
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(null, keyStorePassword.toCharArray());
            keystore.setCertificateEntry("azure-cosmos-emulator", certificate);
            return keystore;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            closeResponseSilently(response);
            closeClientSilently(client);
        }
    }

    private static SSLSocketFactory getTrustAllSSLSocketFactory(TrustManager[] trustManagers) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
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
                ConnectionPool connectionPool = client.connectionPool();
                connectionPool.evictAll();
                Cache cache = client.cache();
                if (Objects.nonNull(cache)) {
                    cache.close();
                }
            }
        } catch (Exception ignored) {
        }
    }
}
