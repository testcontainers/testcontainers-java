package org.testcontainers.containers;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.testcontainers.containers.Constants.EMULATOR_CERTIFICATE_ALIAS;
import static org.testcontainers.containers.Constants.EMULATOR_CERTIFICATE_ENDPOINT_URI;
import static org.testcontainers.containers.Constants.STORE_PASSWORD;
import static org.testcontainers.containers.Constants.STORE_TYPE;

/**
 * @author Onur Kagan Ozcan
 */
final class KeyStoreUtils {

    static void downloadPemFromEmulator(String endpoint, Path pemResourceOutput) {
        try {
            URLSSLCertificateCheck.disable();
            try (InputStream in = new URL(endpoint + EMULATOR_CERTIFICATE_ENDPOINT_URI).openStream()) {
                Files.copy(in, pemResourceOutput, REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            throw new IllegalStateException();
        } finally {
            URLSSLCertificateCheck.enable();
        }
    }

    static void importEmulatorCertificate(Path pemLocation, Path keyStoreOutput) {
        try {
            byte[] emulatorPemFile = readAllBytes(pemLocation);
            byte[] emulatorCertificate = parseDERFromPEM(emulatorPemFile);
            X509Certificate theCertificateObject = generateCertificateFromDER(emulatorCertificate);
            //
            KeyStore keystore = KeyStore.getInstance(STORE_TYPE);
            keystore.load(null, STORE_PASSWORD.toCharArray());
            keystore.setCertificateEntry(EMULATOR_CERTIFICATE_ALIAS, theCertificateObject);
            keystore.store(new FileOutputStream(keyStoreOutput.toFile()), STORE_PASSWORD.toCharArray());
        } catch (Exception ex) {
            throw new IllegalStateException();
        }
    }

    private static byte[] parseDERFromPEM(byte[] pem) {
        String data = new String(pem);
        String[] tokens = data.split("-----BEGIN CERTIFICATE-----");
        tokens = tokens[1].split("-----END CERTIFICATE-----");
        return Base64Helper.parseBase64Binary(tokens[0]);
    }

    private static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }
}
