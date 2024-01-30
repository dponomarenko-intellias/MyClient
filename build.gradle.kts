// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(pluginLibs.plugins.android.application) apply false
    alias(pluginLibs.plugins.kotlin.android) apply false
    alias(pluginLibs.plugins.ksp) apply false
    alias(pluginLibs.plugins.hilt) apply false
}
