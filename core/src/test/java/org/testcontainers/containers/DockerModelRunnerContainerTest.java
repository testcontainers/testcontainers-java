package org.testcontainers.containers;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.junit.Test;

public class DockerModelRunnerContainerTest {

    @Test
    public void pullsModelAndExposesInference() {
        String modelName = "ai/smollm2:360M-Q4_K_M";

        try (DockerModelRunnerContainer dmr = new DockerModelRunnerContainer()
            .withModel(modelName);) {
            dmr.start();

            OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(dmr.getOpenAIEndpoint())
                .build();

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage("Say this is a test")
                .model(modelName)
                .build();
            ChatCompletion chatCompletion = client.chat().completions().create(params);

            String answer = chatCompletion.toString();
            System.out.println(answer);

        }
    }

}
