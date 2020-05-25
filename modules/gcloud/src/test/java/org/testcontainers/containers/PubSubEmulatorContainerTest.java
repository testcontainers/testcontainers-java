package org.testcontainers.containers;

import java.io.IOException;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.Topic;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PubSubEmulatorContainerTest {

	@Rule
	public PubSubEmulatorContainer emulator = new PubSubEmulatorContainer();

	@Test
	public void testSimple() throws IOException {
		String hostport = emulator.getContainerIpAddress() + ":" + emulator.getMappedPort(8085);
		ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
		try {
			TransportChannelProvider channelProvider =
					FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
			CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

			TopicAdminClient topicClient =
					TopicAdminClient.create(
							TopicAdminSettings.newBuilder()
									.setTransportChannelProvider(channelProvider)
									.setCredentialsProvider(credentialsProvider)
									.build());
			Topic topic = Topic.newBuilder().setName("projects/my-project-id/topics/my-topic-id").build();
			topicClient.createTopic(topic);

			Publisher publisher = Publisher.newBuilder(topic.getName()).build();
			PubsubMessage message = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("test message")).build();
			ApiFuture<String> future = publisher.publish(message);
			ApiFutures.addCallback(future, new ApiFutureCallback<String>() {
				@Override
				public void onFailure(Throwable t) {

				}

				@Override
				public void onSuccess(String result) {
					assertThat(result).isNotNull();
				}
			}, MoreExecutors.directExecutor());
		} finally {
			channel.shutdown();
		}
	}

}
