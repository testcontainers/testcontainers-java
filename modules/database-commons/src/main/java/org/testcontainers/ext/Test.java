package org.testcontainers.ext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Test {
    public static void main(String[] args) throws IOException {
        Path p = Paths.get("C:\\ideaspace\\synthesized\\testing-suite\\engine\\executor\\src\\e2e\\resources\\testspecs\\mysql\\prepare_test_data.sql");

        String s = Files.lines(p).collect(Collectors.joining("\n"));
        ArrayList<String> l = new ArrayList<>();
        ScriptUtils.splitSqlScript("a", s, ";",
            ScriptUtils.DEFAULT_COMMENT_PREFIX, ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER, l);
        l.forEach(line -> {
            System.out.println(line);
            System.out.println("----");
        });

    }
}
