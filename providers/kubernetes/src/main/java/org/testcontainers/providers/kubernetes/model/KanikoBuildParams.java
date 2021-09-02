package org.testcontainers.providers.kubernetes.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class KanikoBuildParams {

    private String tag;
    private Path dockerFile;
    private boolean pullEnabled = true; // TODO: Not supported - isn't it?
    private final Map<String, String> buildArgs = new HashMap<>();
    private boolean disabledCache;
    private Map<String, String> labels;
    private boolean disabledPush = false;


    public String[] createBuildCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(
            "executor"
        ));


        if(labels != null && !labels.isEmpty()) {
            labels.entrySet().forEach(e -> {
                cmd.add("--label");
                cmd.add(String.format("%s=%s", e.getKey(), e.getValue()));
            });
        }

        if(disabledPush) {
            cmd.add("--no-push");
        }

        if(!disabledCache) {
            cmd.add("--cache=true");
        }

        if(tag != null && !tag.isEmpty()) {
            cmd.add("--destination");
            cmd.add(tag);
        }

        if(dockerFile != null) {
            cmd.add("--dockerfile");
            cmd.add(dockerFile.toString());
        }

        if(buildArgs != null && !buildArgs.isEmpty()) {
            buildArgs.entrySet().forEach(e -> {
                cmd.add("--build-arg");
                cmd.add(String.format("%s=%s", e.getKey(), e.getValue()));
            });
        }

        cmd.add("--context");
        cmd.add("tar://./context.tar.gz");
        return cmd.toArray(new String[0]);
    }


}
