package com.carrotsearch.gradle.buildinfra.buildoptions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class OptionGroupingSpec {
  private List<OptionGroup> optionGroups = new ArrayList<>();
  private String ungroupedDescription = "Other options";

  public void group(String description, String pattern) {
    optionGroups.add(new OptionGroup(Pattern.compile(pattern), description));
  }

  public void otherOptions(String description) {
    this.ungroupedDescription = description;
  }

  List<OptionGroup> getOptionGroups() {
    return optionGroups;
  }

  public String getOtherOptions() {
    return ungroupedDescription;
  }
}
