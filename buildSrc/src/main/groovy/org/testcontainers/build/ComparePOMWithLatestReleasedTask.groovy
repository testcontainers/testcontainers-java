package org.testcontainers.build

import groovy.xml.XmlSlurper
import org.apache.maven.artifact.versioning.ComparableVersion
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class ComparePOMWithLatestReleasedTask extends DefaultTask {

    @Input
    Set<String> ignore = []

    @Input
    Map<String, String> minimumVersions = [:]

    @TaskAction
    def doCompare() {
        def rootNode = new XmlSlurper().parse(project.tasks.generatePomFileForMavenJavaPublication.destination)

        def artifactId = rootNode.artifactId.text()

        def latestRelease = new XmlSlurper()
            .parse("https://repo1.maven.org/maven2/org/testcontainers/${artifactId}/maven-metadata.xml")
            .versioning.release.text()

        def releasedRootNode = new XmlSlurper()
            .parse("https://repo1.maven.org/maven2/org/testcontainers/${artifactId}/${latestRelease}/${artifactId}-${latestRelease}.pom")

        Set<String> releasedDependencies = releasedRootNode.dependencies.children()
            .collect { "${it.groupId.text()}:${it.artifactId.text()}".toString() }

        Map<String, String> currentVersions = [:]
        for (dependency in rootNode.dependencies.children()) {
            def coordinates = "${dependency.groupId.text()}:${dependency.artifactId.text()}".toString()
            currentVersions[coordinates] = dependency.version.text()
            if (!releasedDependencies.contains(coordinates) && !ignore.contains(coordinates)) {
                throw new IllegalStateException("A new dependency '${coordinates}' has been added to 'org.testcontainers:${artifactId}' - if this was intentional please add it to the ignore list in ${project.buildFile}")
            }
        }

        for (entry in minimumVersions) {
            def coords = entry.key
            def minVersion = entry.value
            def currentVersion = currentVersions[coords]
            if (currentVersion == null || currentVersion.isEmpty()) {
                throw new IllegalStateException("Dependency '${coords}' has a minimum version '${minVersion}' declared in ${project.buildFile} but is missing from the generated POM of 'org.testcontainers:${artifactId}'")
            }
            if (new ComparableVersion(currentVersion) < new ComparableVersion(minVersion)) {
                throw new IllegalStateException("Dependency '${coords}' resolved to '${currentVersion}' in the POM of 'org.testcontainers:${artifactId}', which is below the required minimum '${minVersion}' declared in ${project.buildFile}")
            }
        }
    }
}
