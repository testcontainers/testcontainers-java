package org.testcontainers.containers;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * @author onurozcan
 */
final class KeyStoreUtils {

    private static final TrustManager[] TRUST_MANAGERS = new TrustManager[]{
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

    static KeyStore buildKeyStoreByDownloadingCertificate(String endpoint, String emulatorKey) {
        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(getSSLSocketFactory(), (X509TrustManager) TRUST_MANAGERS[0])
                .hostnameVerifier((s, sslSession) -> true)
                .build();
        Request request = new Request.Builder()
                .get()
                .url(endpoint + "/_explorer/emulator.pem")
                .build();
        try {
            Response response = client.newCall(request).execute();
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(response.body().byteStream());
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(null, emulatorKey.toCharArray());
            keystore.setCertificateEntry("azure-cosmos-emulator", certificate);
            return keystore;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, TRUST_MANAGERS, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
