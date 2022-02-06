/*
 * Copyright (c) 2021. The Meowool Organization Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * In addition, if you modified the project, you must include the Meowool
 * organization URL in your code file: https://github.com/meowool
 *
 * 如果您修改了此项目，则必须确保源文件中包含 Meowool 组织 URL: https://github.com/meowool
 */
@file:Suppress("SpellCheckingInspection", "UNCHECKED_CAST")

import com.github.gmazzo.gradle.plugins.BuildConfigExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.devtools.ksp.gradle.KspExtension
import com.meowool.sweekt.firstCharTitlecase

plugins {
  `kotlin-dsl` apply false
  id("com.google.devtools.ksp") apply false
  id("com.github.gmazzo.buildconfig") apply false
  id("com.github.johnrengelman.shadow") apply false
}

subprojects {
  optIn("com.meowool.meta.internal.InternalCompilerApi")
}

registerLogic {
  dependencies {
    implementation(Libs.Meowool.Toolkit.Sweekt)
  }

  project("meta-group") {
    val groupName = project.name
    subprojects {
      extra["meta.group"] = groupName
      apply(plugin = Plugins.BuildConfig)
      configure<BuildConfigExtension> {
        className(groupName.firstCharTitlecase() + "Info")
        buildConfigField("String", "Version", "\"$version\"")
        buildConfigField("String", "GroupId", "\"$group\"")
        buildConfigField("String", "ArtifactId", "\"$groupName-compiler\"")
        buildConfigField("String", "CompilerId", "\"com.meowool.$groupName.compiler\"")
      }
    }
  }

  project("meta-gradle") {
    apply(plugin = Plugins.Gradle.Kotlin.Dsl)
    publication {
      data {
        artifactId = "$metaGroup-gradle"
        pluginId = "com.meowool.$metaGroup"
        pluginClass = "$pluginId.GradlePlugin"
      }
    }
    dependencies.implementation(Libs.Kotlin.Gradle.Plugin)
  }

  project("meta-compiler") {
    apply(plugin = "kotlin")
    apply(plugin = Plugins.Google.Devtools.Ksp)

    dependencies {
      compileOnlyOf(
        Libs.Kotlin.Compiler,
        Libs.Kotlin.Stdlib.Common,
      )
      apiProject(Projects.Compiler.Base)
      "ksp"(project(Projects.Compiler.Base.Ksp))
    }

    sourceSets {
      main {
        derivatives.forEach {
          java.srcDir("$it/main/kotlin")
          resources.srcDir("$it/main/resources")
        }
      }
      // Set the test source set to empty because we can't use the Intellij internal compiler API for testing.
      //  see: 'meta-compiler-embedded'
      test {
        java.setSrcDirs(emptyList<File>())
        resources.setSrcDirs(emptyList<File>())
      }
    }

    configure<KspExtension> {
      arg("meta.plugin.package", "com.meowool.meta.$metaGroup")
    }

    publication.data.artifactId = "$metaGroup-compiler"
  }

  project("meta-compiler-embedded") {
    apply(plugin = "kotlin")
    apply(plugin = Plugins.Shadow)

    dependencies {
      testImplementationProject(Projects.Compiler.Base.Testing)

      // Import the parent project, i.e. compiler-plugin project
      parent!!.path.also { fullCompiler ->
        // Because they will all be flattened together by shadowJar in the end,
        //   all need to use 'compileOnly' to avoid its jars of dependencies participating in the runtime classpath.
        compileOnlyProjects(fullCompiler)
        testCompileOnlyProject(fullCompiler)
      }
    }

    publication.data.artifactId = "$metaGroup-compiler-embedded"

    // We move the test source set dir into the parent project (i.e. used intellij compiler project) dir.
    //  see: 'meta-compiler'
    sourceSets.test {
      parent?.derivatives?.forEach {
        java.srcDir("../$it/test/kotlin")
        resources.srcDir("../$it/test/resources")
      }
    }

    val shadowJar = tasks.named<ShadowJar>("shadowJar") {
      configurations = listOf(project.configurations.compileClasspath.get())
      // Don't use intellij internal APIs, redirect them to embedded Kotlin Compiler APIs
      relocate("com.intellij", "org.jetbrains.kotlin.com.intellij")
    }

    // Replace the standard jar with the one built by 'shadowJar' in both api and runtime variants
    configurations {
      fun NamedDomainObjectProvider<Configuration>.replaceShadowJar() = get().outgoing {
        artifacts.clear()
        artifact(shadowJar.flatMap { it.archiveFile })
      }
      apiElements.replaceShadowJar()
      runtimeElements.replaceShadowJar()
    }

    // Add the jar built by 'shadowJar' to classpath
    tasks.test {
      useJUnitPlatform()
      dependsOn(shadowJar)
      classpath += shadowJar.get().outputs.files
      // Print all runtime classpath of dependencies when test
//      println("All test dependencies: ")
//      println(classpath.files.joinToString("\n").prependIndent("  "))
    }
  }
}

publication.data {
  val baseVersion = "0.1.0"
  version = "$baseVersion-LOCAL"
  // Used to publish non-local versions of artifacts in CI environment
  versionInCI = "$baseVersion-SNAPSHOT"

  displayName = rootProject.name
  groupId = "com.meowool.meta"
  description = "Provides the environment for developing catnip related projects."
  url = "https://github.com/meowool/${rootProject.name}"
  vcs = "$url.git"
  developer {
    id = "rin"
    name = "Rin Orz"
    url = "https://github.com/RinOrz/"
  }
}

val Project.metaGroup: String
  get() = extra["meta.group"].toString()

val Project.derivatives: Array<String>
  get() = extra.properties["derivatives"] as? Array<String> ?: emptyArray()
