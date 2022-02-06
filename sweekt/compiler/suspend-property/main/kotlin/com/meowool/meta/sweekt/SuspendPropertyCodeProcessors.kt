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
package com.meowool.meta.sweekt

import com.meowool.meta.Meta
import com.meowool.meta.codes
import com.meowool.meta.utils.ir.addDeferredChild
import com.meowool.meta.utils.ir.copy
import com.meowool.meta.utils.ir.irReturnExprBody
import com.meowool.meta.utils.ir.parentContainer
import com.meowool.sweekt.castOrNull
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.statements

/**
 * Processes properties annotated with @Suspend.
 * ---
 *
 * - Step 1: Move all accessors to new suspend functions.
 * - Step 2: Replace suspended block with inlined block.
 * - Step 3: Remap all references of original accessors to new suspend functions.
 *
 * For example:
 * ```
 * @Suspend
 * var time: Long
 *   get() = suspendGetter {
 *     withContext(Dispatchers.IO) {
 *       fetchData().time
 *     }
 *   }
 *   set(value) = suspendSetter {
 *     withContext(Dispatchers.Main) {
 *       updateTimeView(value)
 *     }
 *   }
 * ```
 *
 * After Processing:
 * ```
 * suspend fun getTime(): Long {
 *   return suspendGetterInlined {
 *     withContext(Dispatchers.IO) {
 *       fetchData().time
 *     }
 *   }
 * }
 *
 * suspend fun setTime(value: Long) {
 *   suspendSetterInlined {
 *     withContext(Dispatchers.Main) {
 *       updateTimeView(value)
 *     }
 *   }
 * }
 * ```
 *
 * @author 凛 (RinOrz)
 */
@Meta val SuspendPropertyProcessor = codes.property(
  premise = { hasAnnotation(SUSPEND) && !isFakeOverride },
  processing = {
    fun IrSimpleFunction.moveToNewSuspendFunction() = copy(
      isSuspend = true,
      isFakeOverride = false,
      correspondingPropertySymbol = null,
      origin = IrDeclarationOrigin.DEFINED
    ).apply {
      // The expression `get() = suspendCall {}` is actually `get() { return suspendCall {} }`,
      // So we need to process the actual return expression
      body = body?.statements?.singleOrNull()?.castOrNull<IrReturn>()
        ?.value?.process(SuspendCallInlineProcessor)?.castOrNull<IrExpression>()
        ?.let { buildIr { irReturnExprBody(it) } }

      parentContainer.addDeferredChild(this)
    }

    property.apply {
      getter remapSymbolTo getter?.moveToNewSuspendFunction()
      setter remapSymbolTo setter?.moveToNewSuspendFunction()

      getter = null
      setter = null
    }
  }
)

/**
 * Processes calls to 'suspendGetter' or 'suspendSetter' blocks.
 * Replace the targets they call with inline function.
 *
 * @author 凛 (RinOrz)
 */
private val SuspendCallInlineProcessor = codes.call {
  when (callee.kotlinFqName) {
    SUSPEND_GETTER -> INLINED_SUSPEND_GETTER
    SUSPEND_SETTER -> INLINED_SUSPEND_SETTER
    else -> null
  }?.also { inlinedSymbol ->
    result = call.copy(symbol = referenceFunctions(inlinedSymbol).single())
  }
}
