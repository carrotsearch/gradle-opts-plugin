package com.carrotsearch.gradle.opts

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

/**
 * Plugin entry.
 */
@CompileStatic
class OptsPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    if (GradleVersion.current() < GradleVersion.version("6.2")) {
      project.logger.error(
          "Requires Gradle >= 6.2")
    }

    project.extensions.add("opts", new OptsPluginExtension(project))
  }
}
