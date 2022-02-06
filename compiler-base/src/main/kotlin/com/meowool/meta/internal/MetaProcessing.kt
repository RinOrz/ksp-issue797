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
package com.meowool.meta.internal

import com.meowool.meta.utils.ir.copy
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.util.isPropertyAccessor
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

@InternalCompilerApi
inline fun IrModuleFragment.processMeta(
  context: IrPluginContext,
  action: (context: IrPluginContext, module: IrModuleFragment, referencedSymbolRemapper: MetaReferencedSymbolRemapper) -> Unit
) {
  val symbolRemapper = MetaReferencedSymbolRemapper()
  action(context, this, symbolRemapper)

  // Deep copy to process remapped symbols
  transformChildrenVoid(symbolRemapper)
  patchDeclarationParents()

  // Fix bug phase
  transformChildrenVoid(object : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
      var call = expression
      // Fix incorrect property get call
      if (expression.origin == IrStatementOrigin.GET_PROPERTY && !expression.symbol.owner.isPropertyAccessor) {
        call = expression.copy(origin = null)
      }
      return super.visitCall(call)
    }
  })
}
