package com.meowool.meta.internal

import com.meowool.meta.annotations.InternalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

@InternalCompilerApi
inline fun transformRemapping(
  module: IrModuleFragment,
  context: IrPluginContext,
  action: (context: IrPluginContext, module: IrModuleFragment, symbolRemapper: MetaSymbolRemapper) -> Unit
) {
  val symbolRemapper = MetaSymbolRemapper()
  module.transformChildrenVoid(DeepCopyIrTreeWithSymbols(symbolRemapper, DeepCopyTypeRemapper(symbolRemapper)))
  action(context, module, symbolRemapper)
  module.patchDeclarationParents()
}