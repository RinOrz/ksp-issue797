/*
 * Copyright (c) 2022. The Meowool Organization Open Source Project
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
package com.meowool.meta

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.meowool.sweekt.iteration.toArray
import org.intellij.lang.annotations.Language

const val MetaPackageName = "com.meowool.meta"

val KSDeclaration.qualifiedNameString get() = qualifiedName!!.asString()

infix fun KSTypeReference.isQualified(qualifiedName: String): Boolean =
  resolve().declaration.qualifiedNameString == qualifiedName

fun Sequence<KSPropertyDeclaration>.filterType(qualifiedName: String) =
  filter { it.type.isQualified(qualifiedName) }

/**
 * Create a [Dependencies] to associate with an output.
 *
 * @receiver Sources for this output to depend on.
 * @param aggregating Whether the output should be invalidated on a new source file or a change in any of the existing
 *   files. Namely, whenever there are new information.
 */
fun Sequence<KSDeclaration>.asDependencies(aggregating: Boolean = true): Dependencies =
  Dependencies(aggregating, *mapNotNull { it.containingFile }.toArray())

fun CodeGenerator.createKotlinFile(
  dependencies: Dependencies,
  packageName: String,
  fileName: String,
  @Language("kotlin") sourceCode: String,
) = createNewFile(
  dependencies,
  packageName,
  fileName,
).bufferedWriter().use { it.write(sourceCode.trimIndent()) }

fun CodeGenerator.createSpiFile(
  dependencies: Dependencies,
  providerInterface: String,
  implementationClass: String,
) = createSpiFile(dependencies, providerInterface, implementationClasses = arrayOf(implementationClass))

fun CodeGenerator.createSpiFile(
  dependencies: Dependencies,
  providerInterface: String,
  vararg implementationClasses: String,
) = createNewFile(
  dependencies,
  packageName = "",
  fileName = "META-INF/services/$providerInterface",
  extensionName = ""
).bufferedWriter().use {
  it.write(implementationClasses.joinToString("\n"))
}
