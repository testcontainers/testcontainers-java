package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

/**
 * A Spanner container. Default ports: 9010 for GRPC and 9020 for HTTP.
 *
 * @author Eddú Meléndez
 */
public class SpannerEmulatorContainer extends GCloudGenericContainer<SpannerEmulatorContainer> {

	private static final int GRPC_PORT = 9010;
	private static final int HTTP_PORT = 9020;

	public SpannerEmulatorContainer(String image) {
		super(image);
		withExposedPorts(GRPC_PORT, HTTP_PORT);
		setWaitStrategy(new LogMessageWaitStrategy()
				.withRegEx(".*Cloud Spanner emulator running\\..*"));
	}

	public SpannerEmulatorContainer() {
		this("gcr.io/cloud-spanner-emulator/emulator:0.7.28");
	}
}
