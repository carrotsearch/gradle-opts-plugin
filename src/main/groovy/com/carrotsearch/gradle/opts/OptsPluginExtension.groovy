package com.carrotsearch.gradle.opts

import groovy.transform.CompileStatic
import org.gradle.api.Project

/**
 * Extension methods.
 */
@CompileStatic
class OptsPluginExtension {
  private final Project project

  OptsPluginExtension(Project project) {
    this.project = project
  }

  // Project property, system property or default value (result of a closure call, if it's a closure).
  Object optOrDefault(String propName, Object defValue) {
    def result
    if (project.hasProperty(propName)) {
      result = project.property(propName)
    } else if (System.properties.containsKey(propName)) {
      result = System.properties.get(propName)
    } else {
      result = closureOrValue(defValue)
    }
    return result
  }

  // Either a project, system property, environment variable or default value.
  Object optOrEnvOrDefault(String propName, String envName, Object defValue) {
    return optOrDefault(propName, envOrDefault(envName, defValue));
  }

  // System environment variable or default.
  Object envOrDefault(String envName, Object defValue) {
    def result = System.getenv(envName)
    if (result != null) {
      return result
    } else {
      return closureOrValue(defValue)
    }
  }

  static Object closureOrValue(Object value) {
    if (value instanceof Closure) {
      return value.call()
    } else {
      return value
    }
  }
}