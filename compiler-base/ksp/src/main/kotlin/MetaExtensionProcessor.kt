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
import com.meowool.sweekt.iteration.takeIfNotEmpty

/**
 * @author 凛 (RinOrz)
 */
object MetaExtensionProcessor : AnnotationProcessor<KSPropertyDeclaration>("$MetaPackageName.Meta") {
  private const val KotlinComponentRegistrar = "org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar"
  private const val KotlinStorageComponentContainerContributor = "org.jetbrains.kotlin.extensions.StorageComponentContainerContributor"
  private const val KotlinIrGenerationExtension = "org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension"

  override fun Sequence<KSPropertyDeclaration>.process(
    resolver: Resolver,
    generator: CodeGenerator,
    packageName: String
  ) {
    val callAnalyzers = filterType("$MetaPackageName.analysis.CallAnalyzer")
    val propertyAnalyzers = filterType("$MetaPackageName.analysis.DeclarationAnalyzer")
    val codeProcessors = filterType("$MetaPackageName.codegen.CodeProcessor")
    val codeProcessorStartCallbacks = filterType("$MetaPackageName.codegen.CodeProcessor.Callback.Start")
    val codeProcessorEndCallbacks = filterType("$MetaPackageName.codegen.CodeProcessor.Callback.End")

    val storageComponents = callAnalyzers.plus(propertyAnalyzers)
    val registerComponentContainerContributorStatement = storageComponents.takeIfNotEmpty()?.let {
      "$KotlinStorageComponentContainerContributor.registerExtension(project, StorageComponentContainerContributor())"
    }.orEmpty()

    val irExtensions = codeProcessors.plus(codeProcessorStartCallbacks).plus(codeProcessorEndCallbacks)
    val registerIrExtensionStatement = irExtensions.takeIfNotEmpty()?.let {
      "$KotlinIrGenerationExtension.registerExtension(project, IrGenerationExtension())"
    }.orEmpty()

    val dependencies = storageComponents.plus(irExtensions).takeIfNotEmpty()?.asDependencies() ?: return

    @Suppress("unused")
    generator.createKotlinFile(
      dependencies,
      packageName,
      "ComponentRegistrar",
      sourceCode = """
        package $packageName

        import com.intellij.mock.MockProject
        import com.meowool.meta.codegen.CodeProcessor
        import com.meowool.meta.analysis.Analyzer
        import com.meowool.meta.internal.processMeta
        import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
        import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
        import org.jetbrains.kotlin.config.CompilerConfiguration
        import org.jetbrains.kotlin.container.StorageComponentContainer
        import org.jetbrains.kotlin.container.useInstance
        import org.jetbrains.kotlin.descriptors.ModuleDescriptor
        import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
        import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
        import org.jetbrains.kotlin.platform.TargetPlatform

        /**
         * Automatically generated, do not edit.
         *
         * @author 凛 (RinOrz)
         */
        @OptIn(com.meowool.meta.internal.InternalCompilerApi::class)
        class ComponentRegistrar : $KotlinComponentRegistrar {
          override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
            $registerComponentContainerContributorStatement
            $registerIrExtensionStatement
          }

          class StorageComponentContainerContributor : $KotlinStorageComponentContainerContributor {
            override fun registerModuleComponents(
              container: StorageComponentContainer,
              platform: TargetPlatform,
              moduleDescriptor: ModuleDescriptor
            ) {
              fun Analyzer.use() = also {
                it.componentContainer = container
                container.useInstance(it)
              }

              ${storageComponents.joinToString("\n") { "${it.qualifiedNameString}.use()" }}
            }
          }

          class IrGenerationExtension : $KotlinIrGenerationExtension {
            override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
              ${codeProcessorStartCallbacks.joinToString("\n") { "$it.action(pluginContext, moduleFragment)" }}
              moduleFragment.processMeta(pluginContext) { context, module, referencedSymbolRemapper ->

                fun CodeProcessor.transform() = also {
                  it.pluginContext = context
                  it.moduleFragment = module
                  it.referencedSymbolRemapper = referencedSymbolRemapper
                  moduleFragment.transformChildrenVoid(it)
                }

                ${codeProcessors.joinToString("\n") { it.qualifiedNameString + ".transform()" }}
              }
              ${codeProcessorEndCallbacks.joinToString("\n") { "$it.action(pluginContext, moduleFragment)" }}
            }
          }
        }
      """
    )

    generator.createSpiFile(
      dependencies,
      providerInterface = KotlinComponentRegistrar,
      implementationClass = "$packageName.ComponentRegistrar",
    )
    generator.createSpiFile(
      dependencies,
      providerInterface = KotlinStorageComponentContainerContributor,
      implementationClass = "$packageName.ComponentRegistrar\$StorageComponentContainerContributor",
    )
  }
}
