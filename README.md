
Plugin: ```com.carrotsearch.gradle.buildinfra.buildoptions.BuildOptionsPlugin```
--

Adds the infrastructure for "build options". Build options are key-value pairs
(gradle Provider<String> types), with values sourced dynamically from (in order):
* system property (-Dfoo=value),
* gradle property (-Pfoo=value),
* environment variable (foo=value ./gradlew ...)
* a local, typically *not versioned*, root-project relative, ```build-options.local.properties``` property file,
* a versioned root project-relative ```build-options.properties"``` property file.

## Usage

You can add it to your top-level build script using the following configuration:

Typical usage in a build file:
```groovy
plugins {
    id "com.carrotsearch.gradle.opts" version "$version"
}

buildOptions {
  addOption("foo", "String option foo, no default value.")
  addOption("bar", "String option bar, with default value.", "baz")
}

// or:
Provider<String> bazOption = buildOptions.addOption("baz", "Option baz.")

// non-string options are also possible.
{
    Provider<Boolean> boolOpt = buildOptions.addBooleanOption("boolOpt", "Boolean option.", true)
    Provider<Integer> intOpt = buildOptions.addIntOption("intOpt", "integer option.", 42)
}

// property accessor retrieves the value provider for the option's value
// (always as a string provider).
{
  Provider<String> bar = buildOptions["bar"]
  Provider<String> foo = buildOptions["foo"]
}
```

The following commands display all current option values for
the project (compare the output of both commands):

```shell
./gradlew buildOptions
./gradlew buildOptions -Pfoo=xyz -Dbar=abc
```
