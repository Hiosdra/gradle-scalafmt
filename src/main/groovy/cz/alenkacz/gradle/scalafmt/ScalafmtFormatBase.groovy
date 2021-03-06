package cz.alenkacz.gradle.scalafmt

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.scalafmt.interfaces.Scalafmt

class ScalafmtFormatBase extends DefaultTask {
    SourceSet sourceSet
    ClassLoader cl = this.class.getClassLoader()
    PluginExtension pluginExtension

    def globalFormatter = Scalafmt.create(cl)
            .withRespectVersion(false)
            .withDefaultVersion("1.5.1")

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    def getSourceSet() {
        return sourceSet.allSource
    }

    @OutputFile
    def getOutputFile() {
        return new File(project.getBuildDir(), "scalaFmtResults.txt")
    }

    def runScalafmt(boolean testOnly = false) {
        if (project.plugins.withType(JavaBasePlugin).empty) {
            logger.info("Java or Scala gradle plugin not available in this project, nothing to format")
            return
        }

        formatFiles(getSourceSet().getFiles(), testOnly)
    }

    def formatFiles(Set<File> files, testOnly = false) {
        def configpath = ConfigFactory.get(logger,project,pluginExtension.configFilePath)
        def formatter = globalFormatter.withMavenRepositories(*getRepositoriesUrls())

        def misformattedFiles = new ArrayList<String>()

        files.findAll { File f -> canBeFormatted(f) }.each { File f ->
            String contents = f.text
            logger.debug("Formatting '$f'")
            def formattedContents = formatter.format(configpath.toPath(), f.toPath(), contents)
            if (testOnly) {
                if (contents != formattedContents) {
                    misformattedFiles.add(f.absolutePath)
                }
            } else {
                f.write(formattedContents)
            }

            if (testOnly && !misformattedFiles.empty) {
                getOutputFile().write(misformattedFiles.join("\n"))
                throw new ScalafmtFormatException(misformattedFiles)
            } else {
                getOutputFile().write("OK")
            }
        }
    }

    private List<String> getRepositoriesUrls() {
        project.repositories.findAll { it.hasProperty('url') }.collect { it.url.toString() }
    }

    boolean canBeFormatted(File file) {
        !file.getAbsolutePath().startsWith(project.buildDir.absolutePath) && (file.getAbsolutePath().endsWith(".scala") || file.getAbsolutePath().endsWith(".sbt"))
    }
}
