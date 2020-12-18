
# Gradle Opts Plugin

This simple plugin adds an extension <code>opts</code> with several
utility functions for accessing configurable project "options". These
options are keyed properties with defaults sourced from project properties, 
system properties or environment variables.

## Usage

You can add it to your top-level build script using the following configuration:

### `plugins` block:

```groovy
plugins {
  id "com.carrotsearch.gradle.opts" version "$version"
}
```
