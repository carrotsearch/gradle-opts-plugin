package com.carrotsearch.gradle.buildinfra.buildoptions

import org.gradle.testkit.runner.TaskOutcome

class BuildOptionsPluginSpec extends AbstractIntegTest {
    def "provides buildOptions extension and configures basic options"() {
        given:
        buildFile(
                """
        plugins {
          id('com.carrotsearch.gradle.opts')
        }

        Provider<String> stringOption = buildOptions.addOption("a01", "a01 description", "default-value-a01")
        Provider<Boolean> boolOption  = buildOptions.addBooleanOption("a02", "a02 description", false)

        buildOptions {
          addOption("a99", "a99 description")
        }
        
        tasks.register("printOptions", {
          doLast {
            logger.lifecycle("a01: " + buildOptions['a01'].get())
            logger.lifecycle("a02: " + buildOptions['a02'].get())
            logger.lifecycle("a99: " + buildOptions['a99'].isPresent())
          }
        })
        """)

        when:
        def result = gradleRunner()
                .withArguments("printOptions")
                .run()

        then:
        containsLines(result.output, """
          a01: default-value-a01
          a02: false
          a99: false
        """)
        result.task(":printOptions").outcome == TaskOutcome.SUCCESS
    }

    def "provides buildOptions task that shows all options, their sources and values"() {
        given:
        buildFile(
        """
        plugins {
          id('com.carrotsearch.gradle.opts')
        }

        def stringValueProv = project.providers.provider { 'default-value-a03' }
        def boolValueProv = project.providers.provider { false }
        def intValueProv = project.providers.provider { 14 }
        def dirValue = project.layout.buildDirectory.dir("a11").get()
        def dirValueProv = project.layout.buildDirectory.dir("a12")
        def fileValue = project.layout.buildDirectory.file("a14").get()
        def fileValueProv = project.layout.buildDirectory.file("a15")
        buildOptions {
            addOption("a01", "a01 description", "default-value-a01")
            addOption("a02", "a02 description")
            addOption("a03", "a03 description", stringValueProv)
            addBooleanOption("a04", "a04 description", true)
            addBooleanOption("a05", "a05 description")
            addBooleanOption("a06", "a06 description", boolValueProv)
            addIntOption("a07", "a07 description", 13)
            addIntOption("a08", "a08 description")
            addIntOption("a09", "a09 description", intValueProv)
            addDirOption("a10", "a10 description")
            addDirOption("a11", "a11 description", dirValue)
            addDirOption("a12", "a12 description", dirValueProv)
            addFileOption("a13", "a13 description")
            addFileOption("a14", "a14 description", fileValue)
            addFileOption("a15", "a15 description", fileValueProv)
        }
        """)

        when:
        def result = gradleRunner()
                .withArguments("buildOptions")
                .run()

        then:
        containsLines(result.output, """
        Configurable build options in : (the root project)
         
        a01 = default-value-a01 # a01 description
        a02 = [empty]  # a02 description
        a03 = default-value-a03 # a03 description (source: computed value)
        a04 = true     # a04 description (type: boolean)
        a05 = [empty]  # a05 description (type: boolean)
        a06 = false    # a06 description (type: boolean, source: computed value)
        a07 = 13       # a07 description (type: integer)
        a08 = [empty]  # a08 description (type: integer)
        a09 = 14       # a09 description (type: integer, source: computed value)
        a10 = [empty]  # a10 description (type: directory)
        a11 = build/a11 # a11 description (type: directory)
        a12 = build/a12 # a12 description (type: directory, source: computed value)
        a13 = [empty]  # a13 description (type: file)
        a14 = build/a14 # a14 description (type: file)
        a15 = build/a15 # a15 description (type: file, source: computed value)
        """)
        result.task(":buildOptions").outcome == TaskOutcome.SUCCESS
    }

    def "allows buildOptions task to be configured to group build options"() {
        given:
        buildFile(
                """
        plugins {
          id('com.carrotsearch.gradle.opts')
        }

        buildOptions {
            addOption("a01", "a01 description")
            addOption("a02", "a02 description")
            addOption("a03", "a03 description")
            addOption("a04", "a04 description")
        }
        
        tasks.matching { it.name == "buildOptions" }.configureEach {
          optionGroups {
            group("Options a01 and a03", "(a0[1|3].*)")
            group("Options a04", "(a04)")
            otherOptions("Other options")
          }
        }
        """)

        when:
        def result = gradleRunner()
                .withArguments("buildOptions")
                .run()

        then:
        containsLines(result.output, """
        Options a01 and a03
        ===================
        a01 = [empty]  # a01 description
        a03 = [empty]  # a03 description
       
        Options a04
        ===========
        a04 = [empty]  # a04 description
        
        Other options
        =============
        a02 = [empty]  # a02 description
        """)
        result.task(":buildOptions").outcome == TaskOutcome.SUCCESS
    }

    def "boolean options should be set to true on -Pxyz or -Dxyz"() {
        given:
        buildFile(
                """
        plugins {
          id('com.carrotsearch.gradle.opts')
        }

        Provider<Boolean> a01  = buildOptions.addBooleanOption("a01", "a01 description", false)
        Provider<Boolean> a02  = buildOptions.addBooleanOption("a02", "a02 description")
        Provider<Boolean> a03  = buildOptions.addBooleanOption("a03", "a03 description", false)
        Provider<Boolean> a04  = buildOptions.addBooleanOption("a04", "a04 description")

        tasks.register("printOptions", {
          doLast {
            logger.lifecycle("a01: " + buildOptions['a01'].getOrElse("--"))
            logger.lifecycle("a02: " + buildOptions['a02'].getOrElse("--"))
            logger.lifecycle("a03: " + buildOptions['a03'].getOrElse("--"))
            logger.lifecycle("a04: " + buildOptions['a04'].getOrElse("--"))
          }
        })
        """)

        when:
        def result = gradleRunner()
                .withArguments("printOptions", "buildOptions", "-Pa01", "-Da02")
                .run()

        then:
        containsLines(result.output, """
          a01: true
          a02: true
          a03: false
          a04: --
        """)
        result.task(":printOptions").outcome == TaskOutcome.SUCCESS
    }

    def "allOptions task should display all options from all subprojects"() {
        given:
        settingsFile("""
        include("subproject-1")
        include("subproject-2")
        """)
        buildFile(
                """
        plugins {
          id('com.carrotsearch.gradle.opts')
        }

        subprojects {
          apply plugin: 'com.carrotsearch.gradle.opts'
          buildOptions {
            addOption("a03", "a03 description")
          }
        }
        
        configure(project("subproject-2")) {
          buildOptions {
            addOption("a04", "a04 description")
          }
        }

        buildOptions {
            addOption("a01", "a01 description")
            addOption("a02", "a02 description")
        }
   
        allprojects {     
            tasks.withType(com.carrotsearch.gradle.buildinfra.buildoptions.BuildOptionsTask).configureEach {
              optionGroups {
                group("Options a01 and a03", "(a0[1|3].*)")
                group("Options a04", "(a04)")
                otherOptions("Other options")
              }
            }
        }
        """)

        when:
        def result = gradleRunner()
                .withArguments(":allOptions")
                .build()

        then:
        println result.tasks
        containsLines(result.output, """
Configurable build options in 3 projects:
 
Options a01 and a03
===================
a01 = [empty]  # a01 description (in ':')
a03 = [empty]  # a03 description (in 2 projects)
 
Options a04
===========
a04 = [empty]  # a04 description (in ':subproject-2')
 
Other options
=============
a02 = [empty]  # a02 description (in ':')
        """)
    }
}