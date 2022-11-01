package org.testcontainers.build

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class ComparePOMWithLatestReleasedTask extends DefaultTask {

    @Input
    Set<String> ignore = []

    @TaskAction
    def doCompare() {
        def rootNode = new XmlSlurper().parse(project.tasks.generatePomFileForMavenJavaPublication.destination)

        def artifactId = rootNode.artifactId.text()

        def latestRelease = new XmlSlurper()
            .parse("https://repo1.maven.org/maven2/org/testcontainers/${artifactId}/maven-metadata.xml")
            .versioning.release.text()

        def releasedRootNode = new XmlSlurper()
            .parse("https://repo1.maven.org/maven2/org/testcontainers/${artifactId}/${latestRelease}/${artifactId}-${latestRelease}.pom")

        Set<String> dependencies = releasedRootNode.dependencies.children()
            .collect { "${it.groupId.text()}:${it.artifactId.text()}".toString() }

        for (dependency in rootNode.dependencies.children()) {
            def coordinates = "${dependency.groupId.text()}:${dependency.artifactId.text()}".toString()
            if (!dependencies.contains(coordinates) && !ignore.contains(coordinates)) {
                throw new IllegalStateException("A new dependency '${coordinates}' has been added to 'org.testcontainers:${artifactId}' - if this was intentional please add it to the ignore list in ${project.buildFile}")
            }
        }
    }
}
