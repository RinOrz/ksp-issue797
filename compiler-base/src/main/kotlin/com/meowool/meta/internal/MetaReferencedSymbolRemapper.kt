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
import com.meowool.meta.utils.ir.copyToCall
import com.meowool.meta.utils.ir.copyToConstructorCall
import com.meowool.meta.utils.ir.copyToEnumConstructorCall
import com.meowool.sweekt.castOrNull
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrRawFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * [DeepCopyIrTreeWithSymbols]
 *
 * @author 凛 (RinOrz)
 */
class MetaReferencedSymbolRemapper : IrElementTransformerVoid() {
  val oldToNewClasses = hashMapOf<IrClassSymbol, IrClassSymbol>()
  val oldToNewFields = hashMapOf<IrFieldSymbol, IrFieldSymbol>()
  val oldToNewProperties = hashMapOf<IrPropertySymbol, IrPropertySymbol>()
  val oldToNewSimpleFunctions = hashMapOf<IrSimpleFunctionSymbol, IrFunctionSymbol>()
  val oldToNewConstructors = hashMapOf<IrConstructorSymbol, IrFunctionSymbol>()

  override fun visitProperty(declaration: IrProperty): IrStatement {
    declaration.overriddenSymbols = declaration.overriddenSymbols.map { oldToNewProperties[it] ?: it }
    return super.visitProperty(declaration)
  }

  override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
    declaration.overriddenSymbols = declaration.overriddenSymbols.map { oldToNewSimpleFunctions[it].castOrNull() ?: it }
    return super.visitSimpleFunction(declaration)
  }

  override fun visitGetField(expression: IrGetField): IrExpression = super.visitGetField(
    expression.copy(
      symbol = oldToNewFields[expression.symbol] ?: expression.symbol,
      superQualifierSymbol = expression.superQualifierSymbol?.let(oldToNewClasses::get)
        ?: expression.superQualifierSymbol,
    )
  )

  override fun visitSetField(expression: IrSetField): IrExpression = super.visitSetField(
    expression.copy(
      symbol = oldToNewFields[expression.symbol] ?: expression.symbol,
      superQualifierSymbol = expression.superQualifierSymbol?.let(oldToNewClasses::get)
        ?: expression.superQualifierSymbol,
    )
  )

  override fun visitPropertyReference(expression: IrPropertyReference): IrExpression = super.visitPropertyReference(
    expression.copy(
      symbol = oldToNewProperties[expression.symbol] ?: expression.symbol,
      field = expression.field?.let { oldToNewFields[it] ?: it },
      getter = expression.getter?.let { oldToNewSimpleFunctions[it].castOrNull() ?: it },
      setter = expression.setter?.let { oldToNewSimpleFunctions[it].castOrNull() ?: it },
    )
  )

  override fun visitFunctionReference(expression: IrFunctionReference): IrExpression = super.visitFunctionReference(
    expression.copy(
      symbol = getReferencedFunction(expression.symbol),
      reflectionTarget = expression.reflectionTarget?.let(::getReferencedFunction),
    )
  )

  override fun visitRawFunctionReference(expression: IrRawFunctionReference): IrExpression =
    super.visitRawFunctionReference(
      expression.copy(
        symbol = getReferencedFunction(expression.symbol),
      )
    )

  override fun visitReturn(expression: IrReturn): IrExpression = super.visitReturn(
    expression.copy(
      returnTargetSymbol = when (val returnTarget = expression.returnTargetSymbol) {
        is IrFunctionSymbol -> getReferencedFunction(returnTarget)
        else -> returnTarget
      },
    )
  )

  override fun visitCall(expression: IrCall): IrExpression =
    when (val newSymbol = oldToNewSimpleFunctions[expression.symbol] ?: expression.symbol) {
      is IrSimpleFunctionSymbol -> super.visitCall(
        expression.copy(
          symbol = newSymbol,
          superQualifierSymbol = expression.superQualifierSymbol?.let(oldToNewClasses::get)
            ?: expression.superQualifierSymbol,
        )
      )
      is IrConstructorSymbol -> when (newSymbol.owner.parentAsClass.kind) {
        ClassKind.ENUM_CLASS, ClassKind.ENUM_ENTRY -> super.visitEnumConstructorCall(
          expression.copyToEnumConstructorCall(
            symbol = newSymbol
          )
        )
        // TODO: copyToDelegatingConstructorCall
        else -> super.visitConstructorCall(expression.copyToConstructorCall(symbol = newSymbol))
      }
      else -> error("Unknown symbol type: $newSymbol")
    }

  override fun visitConstructorCall(expression: IrConstructorCall): IrExpression =
    when (val newSymbol = oldToNewConstructors[expression.symbol] ?: expression.symbol) {
      is IrConstructorSymbol -> super.visitConstructorCall(expression.copy(symbol = newSymbol))
      is IrSimpleFunctionSymbol -> super.visitCall(expression.copyToCall(symbol = newSymbol))
      else -> error("Unknown symbol type: $newSymbol")
    }

  override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression =
    when (val newSymbol = oldToNewConstructors[expression.symbol] ?: expression.symbol) {
      is IrConstructorSymbol -> super.visitEnumConstructorCall(expression.copy(symbol = newSymbol))
      is IrSimpleFunctionSymbol -> super.visitCall(expression.copyToCall(symbol = newSymbol))
      else -> error("Unknown symbol type: $newSymbol")
    }

  override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression =
    when (val newSymbol = oldToNewConstructors[expression.symbol] ?: expression.symbol) {
      is IrConstructorSymbol -> super.visitDelegatingConstructorCall(expression.copy(symbol = newSymbol))
      is IrSimpleFunctionSymbol -> super.visitCall(expression.copyToCall(symbol = newSymbol))
      else -> error("Unknown symbol type: $newSymbol")
    }

  override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression =
    super.visitLocalDelegatedPropertyReference(
      expression.copy(
        getter = oldToNewSimpleFunctions[expression.getter].castOrNull() ?: expression.getter,
        setter = oldToNewSimpleFunctions[expression.setter].castOrNull() ?: expression.setter,
      )
    )

  override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression =
    super.visitInstanceInitializerCall(
      expression.copy(classSymbol = oldToNewClasses[expression.classSymbol] ?: expression.classSymbol)
    )

  override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression = super.visitGetObjectValue(
    expression.copy(
      symbol = oldToNewClasses[expression.symbol] ?: expression.symbol,
    )
  )

  private fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol =
    oldToNewSimpleFunctions[symbol] ?: symbol
}
