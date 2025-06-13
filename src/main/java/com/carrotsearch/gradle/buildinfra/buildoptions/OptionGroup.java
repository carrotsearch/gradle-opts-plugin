package com.carrotsearch.gradle.buildinfra.buildoptions;

import java.util.regex.Pattern;

public record OptionGroup(Pattern matcher, String description) {}
