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

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.meowool.sweekt.castOrNull
import com.meowool.sweekt.iteration.isNotEmpty
import com.meowool.sweekt.iteration.toArray
import org.intellij.lang.annotations.Language

/**
 * @author 凛 (RinOrz)
 */
@AutoService(SymbolProcessorProvider::class)
class MetaPluginProcessor : SymbolProcessorProvider, SymbolProcessor {
  private lateinit var codeGenerator: CodeGenerator

  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = apply {
    codeGenerator = environment.codeGenerator
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val metaExtensions = resolver.getSymbolsWithAnnotation("$PackageName.annotations.Meta")
      .asSequence()
      .map { it as KSPropertyDeclaration }

    val callAnalyzers = metaExtensions.filter {
      it.type.isQualified("$KotlinResolvePackage.calls.checkers", "CallChecker")
    }
    val propertyAnalyzers = metaExtensions.filter {
      it.type.isQualified("$KotlinResolvePackage.checkers", "DeclarationChecker")
    }
    val codeProcessors = metaExtensions.filter {
      it.type.isQualified("$PackageName.codegen", "CodeProcessor")
    }

    val associatedFiles = (callAnalyzers + propertyAnalyzers + codeProcessors).mapNotNull { it.containingFile }
    if (associatedFiles.isNotEmpty()) {
      val dependencies = Dependencies(true, *associatedFiles.toArray())

      generateSourceFile(
        """
          package $PackageName

          import com.intellij.mock.MockProject
          import com.meowool.meta.annotations.InternalCompilerApi
          import com.meowool.meta.codegen.CodeProcessor
          import com.meowool.meta.internal.transformRemapping
          import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
          import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
          import org.jetbrains.kotlin.config.CompilerConfiguration
          import org.jetbrains.kotlin.container.StorageComponentContainer
          import org.jetbrains.kotlin.container.useInstance
          import org.jetbrains.kotlin.descriptors.ModuleDescriptor
          import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
          import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
          import org.jetbrains.kotlin.platform.TargetPlatform
          import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension as KotlinIrGenerationExtension
          import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor as KotlinStorageComponentContainerContributor

          /**
           * Automatically generated, do not edit.
           *
           * @author 凛 (RinOrz)
           */
          @OptIn(InternalCompilerApi::class)
          class $ComponentRegisterName : ComponentRegistrar {
            override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
              KotlinStorageComponentContainerContributor.registerExtension(project, StorageComponentContainerContributor())
              KotlinIrGenerationExtension.registerExtension(project, IrGenerationExtension())
            }

            class IrGenerationExtension : KotlinIrGenerationExtension {
              override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
                transformRemapping(moduleFragment, pluginContext) { context, module, symbolRemapper ->
                  
                  fun CodeProcessor.transform() = also {
                    it.pluginContext = context
                    it.moduleFragment = module
                    it.symbolRemapper = symbolRemapper
                    moduleFragment.transformChildrenVoid(it)
                  }
        
                  ${codeProcessors.joinToString("\n") { it.qualifiedNameString + ".transform()" }}
                }
              }
            }

            class StorageComponentContainerContributor : KotlinStorageComponentContainerContributor {
              override fun registerModuleComponents(
                container: StorageComponentContainer,
                platform: TargetPlatform,
                moduleDescriptor: ModuleDescriptor
              ) {
                ${(callAnalyzers + propertyAnalyzers).joinToString("\n") { "container.useInstance(${it.qualifiedNameString})" }}
              }
            }
          }
        """.trimIndent(),
        dependencies
      )
      generateServiceLoaderFile(
        providerInterface = "org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar",
        implementationClass = "$PackageName.$ComponentRegisterName",
        dependencies
      )
      generateServiceLoaderFile(
        providerInterface = "org.jetbrains.kotlin.extensions.StorageComponentContainerContributor",
        implementationClass = "$PackageName.$ComponentRegisterName\$StorageComponentContainerContributor",
        dependencies
      )
    }

    return emptyList()
  }

  private fun generateSourceFile(@Language("kotlin") code: String, dependencies: Dependencies) {
    codeGenerator.createNewFile(
      dependencies,
      PackageName,
      ComponentRegisterName,
    ).bufferedWriter().use { it.write(code) }
  }

  private fun generateServiceLoaderFile(
    providerInterface: String,
    implementationClass: String,
    dependencies: Dependencies
  ) {
    codeGenerator.createNewFile(
      dependencies,
      packageName = "",
      fileName = "META-INF/services/$providerInterface",
      extensionName = ""
    ).bufferedWriter().use { it.write(implementationClass) }
  }

  companion object {
    const val PackageName = "com.meowool.meta"
    const val ComponentRegisterName = "MetaComponentRegister"
    const val KotlinResolvePackage = "org.jetbrains.kotlin.resolve"

    val KSDeclaration.qualifiedNameString get() = qualifiedName!!.asString()

    /**
     * Returns true if the type is exactly the same as the given type [packageName] + '.' + [shortName].
     */
    fun KSTypeReference.isQualified(packageName: String, shortName: String): Boolean {
      if (toString() != shortName) return false

      element.castOrNull<KSClassifierReference>()?.apply {
        if (referencedName() != shortName) return false
      }

      return resolve().declaration.qualifiedNameString == "$packageName.$shortName"
    }
  }
}
