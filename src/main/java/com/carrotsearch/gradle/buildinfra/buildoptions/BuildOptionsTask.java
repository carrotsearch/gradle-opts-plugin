package com.carrotsearch.gradle.buildinfra.buildoptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.logging.text.StyledTextOutput;
import org.gradle.internal.logging.text.StyledTextOutput.Style;
import org.gradle.internal.logging.text.StyledTextOutputFactory;

/** Displays a list of all build options and their current values declared in the project. */
public abstract class BuildOptionsTask extends DefaultTask {
  public static final String BUILD_OPTIONS_TASK_GROUP = "Build options";
  public static final String NAME = "buildOptions";

  private final String projectName;

  static final Style normal = Style.Normal;
  static final Style computed = Style.Identifier;
  static final Style overridden = Style.FailureHeader;
  static final Style comment = Style.ProgressStatus;
  static final Style extras = Style.SuccessHeader;
  static final Style optionGroupHeader = Style.Header;

  @Inject
  protected abstract StyledTextOutputFactory getOutputFactory();

  @Input
  public abstract SetProperty<BuildOption> getAllBuildOptions();

  @Inject
  public BuildOptionsTask(Project project) {
    setDescription("Shows configurable options");
    setGroup(BUILD_OPTIONS_TASK_GROUP);
    getAllBuildOptions()
        .convention(
            getProject().getExtensions().getByType(BuildOptionsExtension.class).getAllOptions());
    this.projectName =
        (project == project.getRootProject() ? ": (the root project)" : project.getPath());
  }

  /** Option grouping spec. */
  private final OptionGroupingSpec groupingSpec = new OptionGroupingSpec();

  /** Configures option grouping. */
  public void optionGroups(Action<OptionGroupingSpec> action) {
    action.execute(groupingSpec);
  }

  @TaskAction
  public void exec() {
    var out = getOutputFactory().create(this.getClass());

    Set<BuildOption> allBuildOptions = getAllBuildOptions().get();

    int sourceProjectCount =
        allBuildOptions.stream()
            .map(BuildOption::getProjectPath)
            .collect(Collectors.toSet())
            .size();

    out.append("Configurable build options in ")
        .withStyle(Style.Identifier)
        .append(sourceProjectCount <= 1 ? projectName : sourceProjectCount + " projects:")
        .append("\n\n");

    final int keyWidth =
        allBuildOptions.stream().mapToInt(opt -> opt.getName().length()).max().orElse(1);
    final String keyFmt = "%-" + keyWidth + "s = ";

    List<BuildOption> sortedOptions =
        allBuildOptions.stream().sorted(Comparator.comparing(BuildOption::getName)).toList();

    boolean includeSourceProjectRef = sourceProjectCount > 1;

    if (groupingSpec.getOptionGroups().isEmpty()) {
      printOptionList(sortedOptions, out, keyFmt, includeSourceProjectRef);
    } else {
      var ungrouped = new LinkedHashSet<>(sortedOptions);
      for (OptionGroup group : groupingSpec.getOptionGroups()) {
        var matchingOptions =
            sortedOptions.stream()
                .filter(opt -> group.matcher().matcher(opt.getName()).matches())
                .toList();

        if (matchingOptions.isEmpty()) {
          continue;
        }

        printOptionGroupHeader(out, group.description());
        printOptionList(matchingOptions, out, keyFmt, includeSourceProjectRef);
        ungrouped.removeIf(matchingOptions::contains);
        out.println();
      }

      if (!ungrouped.isEmpty() && groupingSpec.getOtherOptions() != null) {
        printOptionGroupHeader(out, groupingSpec.getOtherOptions());
        printOptionList(ungrouped, out, keyFmt, includeSourceProjectRef);
      }
    }

    out.println();
    printLegend(out);
  }

  record OptionKey(String name, BuildOptionType type, String value, String description) {}

  private void printOptionList(
      Collection<BuildOption> sortedOptions,
      StyledTextOutput out,
      String keyFmt,
      boolean includeProjectRef) {
    var grouped =
        sortedOptions.stream()
            .collect(
                Collectors.groupingBy(
                    (BuildOption option) -> {
                      return new OptionKey(
                          option.getName(),
                          option.getType(),
                          option.asStringProvider().getOrElse(""),
                          option.getDescription());
                    },
                    LinkedHashMap::new,
                    Collectors.toList()));

    for (var entry : grouped.entrySet()) {
      printOptionInfo(entry.getValue().getFirst(), out, keyFmt, includeProjectRef, entry.getValue().size());
    }
  }

  private static void printOptionInfo(
      BuildOption opt,
      StyledTextOutput out,
      String keyFmt,
      boolean includeProjectRef,
      int projectRefs) {
    var value = opt.getValue();

    String valueSource = null;
    var valueStyle = normal;
    if (!value.isPresent()) {
      valueStyle = comment;
    } else {
      var optionValue = value.get();
      if (optionValue.source() == BuildOptionValueSource.COMPUTED_VALUE) {
        valueStyle = computed;
        valueSource = "computed value";
      } else if (!opt.isEqualToDefaultValue()) {
        valueStyle = overridden;
        valueSource =
            switch (optionValue.source()) {
              case GRADLE_PROPERTY -> "project property";
              case SYSTEM_PROPERTY -> "system property";
              case ENVIRONMENT_VARIABLE -> "environment variable";
              case EXPLICIT_VALUE -> "explicit value";
              case COMPUTED_VALUE -> throw new RuntimeException("Unreachable");
              case BUILD_OPTIONS_FILE -> BuildOptionsPlugin.BUILD_OPTIONS_FILE + " file";
              case LOCAL_BUILD_OPTIONS_FILE ->
                  BuildOptionsPlugin.LOCAL_BUILD_OPTIONS_FILE + " file";
            };
      }
    }

    out.format(keyFmt, opt.getName());
    out.withStyle(valueStyle).format("%-8s", value.isPresent() ? value.get() : "[empty]");
    out.withStyle(comment).append(" # ").append(opt.getDescription());

    List<String> extraInfo = new ArrayList<>();
    if (opt.getType() != BuildOptionType.STRING) {
      extraInfo.add("type: " + opt.getType().toString().toLowerCase(Locale.ROOT));
    }
    if (valueSource != null) {
      extraInfo.add("source: " + valueSource);
    }
    if (includeProjectRef) {
      if (projectRefs > 1) {
        extraInfo.add("in " + projectRefs + " projects");
      } else {
        extraInfo.add("in '" + opt.getProjectPath() + "'");
      }
    }
    if (!extraInfo.isEmpty()) {
      out.withStyle(extras).append(" (").append(String.join(", ", extraInfo)).append(")");
    }
    out.append("\n");
  }

  private void printOptionGroupHeader(StyledTextOutput out, String description) {
    out.withStyle(optionGroupHeader).append(description).append("\n").append("=".repeat(description.length()));
    out.println();
  }

  private static void printLegend(StyledTextOutput out) {
    out.append("Option values color coded: ");
    out.withStyle(normal).append("default value");
    out.append(", ");
    out.withStyle(computed).append("computed value");
    out.append(", ");
    out.withStyle(overridden).append("overridden value");
    out.append(", ");
    out.withStyle(comment).append("no value");
    out.println();
  }
}
