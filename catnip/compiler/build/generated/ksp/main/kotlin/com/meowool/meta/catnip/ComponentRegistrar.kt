        package com.meowool.meta.catnip

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
        class ComponentRegistrar : org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar {
          override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
            org.jetbrains.kotlin.extensions.StorageComponentContainerContributor.registerExtension(project, StorageComponentContainerContributor())
            org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension.registerExtension(project, IrGenerationExtension())
          }

          class StorageComponentContainerContributor : org.jetbrains.kotlin.extensions.StorageComponentContainerContributor {
            override fun registerModuleComponents(
              container: StorageComponentContainer,
              platform: TargetPlatform,
              moduleDescriptor: ModuleDescriptor
            ) {
              fun Analyzer.use() = also {
                it.componentContainer = container
                container.useInstance(it)
              }

              com.meowool.meta.cloak.analyzes.InstanceMockInstantiationAnalyzer.use()
com.meowool.meta.cloak.analyzes.InstanceMockDeclarationAnalyzer.use()
            }
          }

          class IrGenerationExtension : org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension {
            override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
              
              moduleFragment.processMeta(pluginContext) { context, module, referencedSymbolRemapper ->

                fun CodeProcessor.transform() = also {
                  it.pluginContext = context
                  it.moduleFragment = module
                  it.referencedSymbolRemapper = referencedSymbolRemapper
                  moduleFragment.transformChildrenVoid(it)
                }

                com.meowool.meta.cloak.processes.InstanceMockSyntheticPropertiesProcessor.transform()
com.meowool.meta.cloak.processes.InstanceMockSyntheticConstructorProcessor.transform()
com.meowool.meta.cloak.processes.InstanceMockSyntheticFunctionProcessor.transform()
              }
              
            }
          }
        }