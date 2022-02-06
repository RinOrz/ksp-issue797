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
import com.meowool.meta.codegen.CodegenContext
import com.meowool.meta.codes
import com.meowool.meta.internal.InternalCompilerApi
import com.meowool.meta.utils.ir.callee
import com.meowool.meta.utils.ir.copy
import com.meowool.meta.utils.ir.correspondingProperty
import com.meowool.meta.utils.ir.findProperty
import com.meowool.meta.utils.ir.getVarargValueArgument
import com.meowool.meta.utils.ir.irGetField
import com.meowool.meta.utils.ir.irGetProperty
import com.meowool.meta.utils.ir.irReturnExprBody
import com.meowool.meta.utils.ir.irSetField
import com.meowool.meta.utils.ir.irSetProperty
import com.meowool.meta.utils.ir.irWhen
import com.meowool.meta.utils.ir.isVal
import com.meowool.meta.utils.ir.parentContainer
import com.meowool.meta.utils.ir.type
import com.meowool.meta.utils.sameTo
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irComposite
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName

/**
 * Processes properties annotated with @LazyInit.
 * ---
 *
 * - Step 1: Set the property to mutable.
 * - Step 2: Generate a bridge field "isLazyInited$xxx" for property.
 * - Step 3: Move the property's initializer expression into the getter.
 *
 * For example:
 * ```
 * @LazyInit
 * val a = "Hello World"
 * ```
 *
 * After Processing:
 * ```
 * var isLazyInited$a = false
 * var a: String
 *   get() = when {
 *     isLazyInited$a -> field
 *     else -> {
 *       field = "Hello World"
 *       isLazyInited$a = true
 *     }
 *   }
 * ```
 *
 * @author 凛 (RinOrz)
 */
@Meta val LazyInitPropertyProcessor = codes.property(
  premise = { hasAnnotation(LAZY_INIT) && !isFakeOverride && backingField != null },
  processing = {
    // Step 1
    if (property.isVal) result = property.copy(isVar = true)

    // Step 2
    val lazyInitBridge = getLazyInitBridgeOf(property)

    // Step 3
    property.getOrAddGetter().syntheticDefaultAccessor().also { getter ->
      getter.body = getter.buildIr {
        irReturnExprBody(
          irWhen(property.type) {
            +irBranch(
              irEquals(irGetProperty(getter, lazyInitBridge), irTrue()),
              irGetField(getter, property)
            )
            +irElseBranch(
              irBlock {
                val value = irTemporary(property.moveInitializerExprTo(getter)).apply {
                  parent = getter
                }
                +irSetField(getter, property, irGet(value))
                +irSetProperty(getter, lazyInitBridge, irTrue())
                +irGet(value)
              }
            )
          }
        )
      }
    }
  }
)

/**
 * Processes calls to 'any.resetLazyValue()' and 'resetLazyValues(...)'.
 * ---
 *
 * - Step 1. Extract the property arguments to reset.
 * - Step 2. Replace calls set their bridge to `false`.
 *
 * For example:
 * ```
 * foo.resetLazyValue()
 * resetLazyValues(bar, baz)
 * ```
 * After Processing:
 * ```
 * isLazyInited$foo = false
 * isLazyInited$bar = false
 * isLazyInited$baz = false
 * ```
 *
 * @author 凛 (RinOrz)
 */
@Meta val LazyInitCallProcessor = codes.call {

  fun IrBuilderWithScope.replaceCallToSetBridge(reference: IrElement?): IrExpression {
    require(reference is IrCall)
    // get called property declaration
    val referenced = reference.callee.correspondingProperty ?: error("Callee is not a property")
    // get or create the bridge corresponding to the property marked with @LazyInit
    val lazyInitBridge = getLazyInitBridgeOf(referenced)
    // set the bridge to false
    return irSetField(reference.dispatchReceiver, lazyInitBridge, irFalse())
  }

  when (callee.kotlinFqName) {
    // case: any.resetLazyValue()
    LAZY_INIT_RESET_VALUE -> {
      result = call.buildIr { replaceCallToSetBridge(reference = call.extensionReceiver) }
    }
    // case: resetLazyValues(...)
    LAZY_INIT_RESET_VALUES -> {
      val vararg = call.getVarargValueArgument(0)
      result = call.buildIr {
        irComposite {
          vararg.elements.forEach {
            +replaceCallToSetBridge(reference = it)
          }
        }
      }
    }
  }
}

private fun CodegenContext.getLazyInitBridgeOf(property: IrProperty): IrProperty {
  val bridgeName = "isLazyInited\$" + property.name.asString()

  return property.parentContainer.findProperty {
    it.name sameTo bridgeName && it.type == builtIns.booleanType
  } ?: property.parentContainer.addProperty(
    name = bridgeName,
    original = property,
    type = builtIns.booleanType,
    initializer = { irBoolean(false) }
  ) {
    isVar = true
    visibility = DescriptorVisibilities.PUBLIC
  }
}

@InternalCompilerApi const val LAZY_INIT_BRIDGE_PREFIX = "isLazyInited$"
