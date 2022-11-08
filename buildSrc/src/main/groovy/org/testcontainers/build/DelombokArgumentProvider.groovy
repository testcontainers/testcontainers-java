package org.testcontainers.build

import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

/**
 * Allows build cache relocatability for Delombok task
 */
class DelombokArgumentProvider implements CommandLineArgumentProvider {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    Set<File> srcDirs

    @OutputDirectory
    File outputDir

    @Override
    Iterable<String> asArguments() {
        return [srcDirs.collect { it.absolutePath }.join(" "), "-d", outputDir.absolutePath, "-f", "generateDelombokComment:skip"]
    }

}
