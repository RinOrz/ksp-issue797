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
package com.meowool.meta.utils.ir

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.parentAsClass

fun IrBuilderWithScope.thisExpr(function: IrFunction) = function.thisReceiver?.let(::irGet)

fun IrBuilderWithScope.irReturnExprBody(value: IrExpression) =
  irExprBody(irReturn(value))

fun IrBuilderWithScope.irReturnBlockExprBody(value: IrExpression) =
  irBlockBody { +irReturn(value) }

fun IrBuilderWithScope.irGetField(receiver: IrExpression?, property: IrProperty): IrGetField =
  irGetField(receiver, property.backingField!!)

fun IrBuilderWithScope.irGetField(scope: IrFunction, property: IrProperty): IrGetField =
  irGetField(thisExpr(scope), property)

fun IrBuilderWithScope.irSetField(
  scope: IrFunction,
  property: IrProperty,
  value: IrExpression
): IrExpression = irSetField(thisExpr(scope), property.backingField!!, value)

fun IrBuilderWithScope.irSetField(
  receiver: IrExpression?,
  property: IrProperty,
  value: IrExpression
): IrExpression = irSetField(receiver, property.backingField!!, value)

fun IrBuilderWithScope.irGetProperty(receiver: IrExpression?, property: IrProperty): IrExpression {
  return if (property.getter != null)
    irGet(property.getter!!.returnType, receiver, property.getter!!.symbol)
  else
    irGetField(receiver, property.backingField!!)
}

fun IrBuilderWithScope.irSetProperty(
  receiver: IrExpression?,
  property: IrProperty,
  value: IrExpression
): IrExpression {
  return if (property.setter != null)
    irSet(property.setter!!.returnType, receiver, property.setter!!.symbol, value)
  else
    irSetField(receiver, property.backingField!!, value)
}

fun IrBuilderWithScope.irGetProperty(scope: IrFunction, property: IrProperty): IrExpression =
  irGetProperty(thisExpr(scope), property)

fun IrBuilderWithScope.irSetProperty(
  scope: IrFunction,
  property: IrProperty,
  value: IrExpression
): IrExpression = irSetProperty(thisExpr(scope), property, value)

fun IrBuilderWithScope.irCall(
  callee: IrFunction,
  vararg valueArguments: IrExpression,
  dispatchReceiver: IrExpression? = null,
  extensionReceiver: IrExpression? = null,
  origin: IrStatementOrigin? = null,
  superQualifierSymbol: IrClassSymbol? = null
): IrCall {
  require(valueArguments.size == callee.valueParameters.size)
  return irCall(callee, origin, superQualifierSymbol).also {
    it.dispatchReceiver = dispatchReceiver
    it.extensionReceiver = extensionReceiver
    valueArguments.forEachIndexed { index, irExpression ->
      it.putValueArgument(index, irExpression)
    }
  }
}

fun IrBuilderWithScope.irCall(
  callee: IrSimpleFunctionSymbol,
  vararg valueArguments: IrExpression,
  dispatchReceiver: IrExpression? = null,
  extensionReceiver: IrExpression? = null,
  origin: IrStatementOrigin? = null,
  superQualifierSymbol: IrClassSymbol? = null
): IrCall {
  require(valueArguments.size == callee.owner.valueParameters.size)
  return irCall(callee.owner, origin, superQualifierSymbol).also {
    it.dispatchReceiver = dispatchReceiver
    it.extensionReceiver = extensionReceiver
    valueArguments.forEachIndexed { index, irExpression ->
      it.putValueArgument(index, irExpression)
    }
  }
}

fun IrBuilderWithScope.irCall(
  callee: IrConstructorSymbol,
  vararg valueArguments: IrExpression,
  dispatchReceiver: IrExpression? = null,
  extensionReceiver: IrExpression? = null,
  origin: IrStatementOrigin? = null,
  type: IrType = callee.owner.returnType,
  constructedClass: IrClass = callee.owner.parentAsClass
): IrConstructorCall {
  require(valueArguments.size == callee.owner.valueParameters.size)
  return IrConstructorCallImpl(
    startOffset, endOffset, type, callee,
    valueArgumentsCount = callee.owner.valueParameters.size,
    typeArgumentsCount = callee.owner.typeParameters.size + constructedClass.typeParameters.size,
    constructorTypeArgumentsCount = callee.owner.typeParameters.size,
    origin = origin
  ).also {
    it.dispatchReceiver = dispatchReceiver
    it.extensionReceiver = extensionReceiver
    valueArguments.forEachIndexed { index, irExpression ->
      it.putValueArgument(index, irExpression)
    }
  }
}

fun IrBuilderWithScope.irWhen(
  type: IrType = context.irBuiltIns.unitType,
  block: BranchBuilder.() -> Unit
): IrWhen {
  val whenExpr = IrWhenImpl(startOffset, endOffset, type)
  val builder = BranchBuilder(whenExpr, context, scope, startOffset, endOffset)
  builder.block()
  return whenExpr
}

class BranchBuilder(
  private val irWhen: IrWhen,
  context: IrGeneratorContext,
  scope: Scope,
  startOffset: Int,
  endOffset: Int
) : IrBuilderWithScope(context, scope, startOffset, endOffset) {
  operator fun IrBranch.unaryPlus() {
    irWhen.branches.add(this)
  }
}
