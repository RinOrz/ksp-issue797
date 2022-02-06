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
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * @author 凛 (RinOrz)
 */
object DiagnosticProcessor : AnnotationProcessor<KSPropertyDeclaration>("$MetaPackageName.diagnostics.Diagnostic") {

  override fun Sequence<KSPropertyDeclaration>.process(
    resolver: Resolver,
    generator: CodeGenerator,
    packageName: String,
  ) {
    val dependencies = this.asDependencies()

    val initStatements = this.map {
      """
        ${it.qualifiedNameString}.apply {
          factory.initializeName("${it.simpleName.asString()}")
          factory.initDefaultRenderer(rendererMap.get(factory))
        }
      """.trimIndent()
    }.joinToString("\n")

    val renderStatements = this.map { it.qualifiedNameString + " renderTo rendererMap" }.joinToString("\n")

    generator.createKotlinFile(
      dependencies,
      packageName,
      "DiagnosticsMessagesExtension",
      """
        package $packageName

        import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
        import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

        /**
         * Automatically generated, do not edit.
         *
         * @author 凛 (RinOrz)
         */
        @OptIn(com.meowool.meta.internal.InternalCompilerApi::class)
        class DiagnosticsMessagesExtension : DefaultErrorMessages.Extension {
          override fun getMap(): DiagnosticFactoryToRendererMap = rendererMap

          init {
            $renderStatements
          }

          companion object {
            private val rendererMap = DiagnosticFactoryToRendererMap("$packageName")

            init {
              $initStatements
            }
          }
        }
      """
    )
    generator.createSpiFile(
      dependencies,
      providerInterface = "org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages\$Extension",
      implementationClass = "$packageName.DiagnosticsMessagesExtension"
    )
  }
}
