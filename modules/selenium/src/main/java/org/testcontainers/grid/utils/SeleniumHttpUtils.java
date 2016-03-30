package org.testcontainers.grid.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.openqa.selenium.remote.internal.HttpClientFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.Objects.nonNull;

public final class SeleniumHttpUtils {

	private static final Logger LOGGER = Logger.getLogger(SeleniumHttpUtils.class.getName());

	public static boolean checkHostState(final HttpHost host, final BasicHttpRequest basicHttpRequest) {
		return Optional.ofNullable(basicHttpRequest)
				.map(request -> execute(host, request))
				.filter(response -> nonNull(response) && response.getStatusLine().getStatusCode() == 200)
				.map(SeleniumHttpUtils::extractObject)
				.filter(json -> nonNull(json) && json.get("success").getAsBoolean())
				.isPresent();
	}

	public static HttpResponse execute(final HttpHost host, final BasicHttpRequest request) {
		HttpResponse response;

		try {
			response = new HttpClientFactory().getHttpClient().execute(host, request);
		} catch (Exception ex) {
			LOGGER.severe("Unable to execute http request: " + ex.getMessage());
			response = null;
		}

		return response;
	}

	public static JsonObject extractObject(final HttpResponse response) {
		JsonObject jsonObject;

		try (final BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(response.getEntity().getContent()))) {
			final StringBuilder buffer = new StringBuilder();
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				buffer.append(line);
			}

			jsonObject = new JsonParser().parse(buffer.toString()).getAsJsonObject();
		} catch (Exception ex) {
			LOGGER.severe("Unable to extract json object: " + ex.getMessage());
			jsonObject = null;
		}

		return jsonObject;
	}

	private SeleniumHttpUtils() {
		throw new UnsupportedOperationException("Illegal access to private constructor");
	}
}
