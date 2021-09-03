package org.testcontainers.providers.kubernetes;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamespaceTemplateRenderer {

    private static final Pattern EXPR = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}");
    private static final char[] ALPHANUMERIC_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

    private final Map<String, String> environmentMap = new HashMap<>();
    private final Random random = new Random();

    public NamespaceTemplateRenderer() {
        init();
    }

    private void init() {

        environmentMap.putAll(System.getenv());

        String randomString = randomAlphanumericChars(5);
        environmentMap.put("RANDOM", randomString);
        environmentMap.put("random", randomString);
    }

    private String randomAlphanumericChars(int length) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<length; i++) {
            sb.append(ALPHANUMERIC_CHARS[random.nextInt(ALPHANUMERIC_CHARS.length)]);
        }
        return sb.toString();
    }

    public String render(String namespaceTemplate) {
        System.getenv();


        Matcher matcher = EXPR.matcher(namespaceTemplate);
        while (matcher.find()) {
            String envValue = environmentMap.get(matcher.group(1).toUpperCase());
            if (envValue == null) {
                envValue = "";
            } else {
                envValue = envValue.replace("\\", "\\\\");
            }
            Pattern subExpression = Pattern.compile(Pattern.quote(matcher.group(0)));
            namespaceTemplate = subExpression.matcher(namespaceTemplate).replaceAll(envValue);
        }

        return namespaceTemplate;
    }

}
