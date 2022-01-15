package com.meowool.meta.sweekt

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
class MetaComponentRegister : ComponentRegistrar {
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

        lazyInitPropertyProcessor.transform()
        lazyInitCallProcessor.transform()
      }
    }
  }

  class StorageComponentContainerContributor : KotlinStorageComponentContainerContributor {
    override fun registerModuleComponents(
      container: StorageComponentContainer,
      platform: TargetPlatform,
      moduleDescriptor: ModuleDescriptor
    ) {
      container.useInstance(com.meowool.meta.sweekt.lazyInitCallAnalyzer)
      container.useInstance(com.meowool.meta.sweekt.lazyInitPropertyAnalyzer)
    }
  }
}