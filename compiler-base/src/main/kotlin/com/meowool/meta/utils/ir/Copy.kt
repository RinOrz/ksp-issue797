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

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrRawFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.getConstructorTypeArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrEnumConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrLocalDelegatedPropertyReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrPropertyReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrRawFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.expressions.putConstructorTypeArgument
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/**
 * @author 凛 (RinOrz)
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrSimpleFunction.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  origin: IrDeclarationOrigin = this.origin,
  symbol: IrSimpleFunctionSymbol = IrSimpleFunctionSymbolImpl(this.descriptor),
  name: Name = this.name,
  visibility: DescriptorVisibility = this.visibility,
  modality: Modality = this.modality,
  returnType: IrType = this.returnType,
  isInline: Boolean = this.isInline,
  isExternal: Boolean = this.isExternal,
  isTailrec: Boolean = this.isTailrec,
  isSuspend: Boolean = this.isSuspend,
  isOperator: Boolean = this.isOperator,
  isInfix: Boolean = this.isInfix,
  isExpect: Boolean = this.isExpect,
  correspondingPropertySymbol: IrPropertySymbol? = this.correspondingPropertySymbol,
  isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
  containerSource: DeserializedContainerSource? = this.containerSource,
  parent: IrDeclarationParent = this.parent,
  annotations: List<IrConstructorCall> = this.annotations,
  metadata: MetadataSource? = this.metadata,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  overriddenSymbols: List<IrSimpleFunctionSymbol> = this.overriddenSymbols,
  dispatchReceiverParameter: IrValueParameter? = this.dispatchReceiverParameter,
  extensionReceiverParameter: IrValueParameter? = this.extensionReceiverParameter,
  valueParameters: List<IrValueParameter> = this.valueParameters,
  typeParameters: List<IrTypeParameter> = this.typeParameters,
  body: IrBody? = this.body,
): IrSimpleFunction = factory.createFunction(
  startOffset,
  endOffset,
  origin,
  symbol,
  name,
  visibility,
  modality,
  returnType,
  isInline,
  isExternal,
  isTailrec,
  isSuspend,
  isOperator,
  isInfix,
  isExpect,
  isFakeOverride,
  containerSource
).also { new ->
  new.parent = parent
  new.annotations = annotations
  new.metadata = metadata
  new.attributeOwnerId = attributeOwnerId
  new.overriddenSymbols = overriddenSymbols
  new.correspondingPropertySymbol = correspondingPropertySymbol
  new.returnType = returnType
  new.dispatchReceiverParameter = dispatchReceiverParameter
  new.extensionReceiverParameter = extensionReceiverParameter
  new.valueParameters = valueParameters
  new.typeParameters = typeParameters
  new.body = body
}

/**
 * @author 凛 (RinOrz)
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrProperty.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  origin: IrDeclarationOrigin = this.origin,
  symbol: IrPropertySymbol = IrPropertySymbolImpl(this.descriptor),
  name: Name = this.name,
  visibility: DescriptorVisibility = this.visibility,
  modality: Modality = this.modality,
  isVar: Boolean = this.isVar,
  isConst: Boolean = this.isConst,
  isLateinit: Boolean = this.isLateinit,
  isDelegated: Boolean = this.isDelegated,
  isExternal: Boolean = this.isExternal,
  isExpect: Boolean = this.isExpect,
  isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
  containerSource: DeserializedContainerSource? = this.containerSource,
  parent: IrDeclarationParent = this.parent,
  annotations: List<IrConstructorCall> = this.annotations,
  metadata: MetadataSource? = this.metadata,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  overriddenSymbols: List<IrPropertySymbol> = this.overriddenSymbols,
): IrProperty = factory.createProperty(
  startOffset,
  endOffset,
  origin,
  symbol,
  name,
  visibility,
  modality,
  isVar,
  isConst,
  isLateinit,
  isDelegated,
  isExternal,
  isExpect,
  isFakeOverride,
  containerSource
).also { new ->
  new.parent = parent
  new.annotations = annotations
  new.metadata = metadata
  new.attributeOwnerId = attributeOwnerId
  new.overriddenSymbols = overriddenSymbols
  new.getter = getter?.also { it.correspondingPropertySymbol = symbol }
  new.setter = setter?.also { it.correspondingPropertySymbol = symbol }

  backingField?.also { old ->
    // fix wrong 'final' modifier
    new.backingField = if (isVar && old.isFinal) {
      old.copy(isFinal = false)
    } else {
      old
    }.also { it.correspondingPropertySymbol = symbol }
  }
}

/**
 * @author 凛 (RinOrz)
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrField.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  origin: IrDeclarationOrigin = this.origin,
  symbol: IrFieldSymbol = IrFieldSymbolImpl(this.descriptor),
  name: Name = this.name,
  type: IrType = this.type,
  visibility: DescriptorVisibility = this.visibility,
  isFinal: Boolean = this.isFinal,
  isExternal: Boolean = this.isExternal,
  isStatic: Boolean = this.isStatic,
  parent: IrDeclarationParent = this.parent,
  annotations: List<IrConstructorCall> = this.annotations,
  metadata: MetadataSource? = this.metadata,
  correspondingPropertySymbol: IrPropertySymbol? = this.correspondingPropertySymbol,
  initializer: IrExpressionBody? = this.initializer,
): IrField = factory.createField(
  startOffset,
  endOffset,
  origin,
  symbol,
  name,
  type,
  visibility,
  isFinal,
  isExternal,
  isStatic
).also { new ->
  new.parent = parent
  new.annotations = annotations
  new.metadata = metadata
  new.correspondingPropertySymbol = correspondingPropertySymbol
  new.initializer = initializer
}

/**
 * @author 凛 (RinOrz)
 */
fun IrGetField.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  symbol: IrFieldSymbol = this.symbol,
  type: IrType = this.type,
  origin: IrStatementOrigin? = this.origin,
  superQualifierSymbol: IrClassSymbol? = this.superQualifierSymbol,
  receiver: IrExpression? = this.receiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId
) = IrGetFieldImpl(
  startOffset,
  endOffset,
  symbol,
  type,
  origin,
  superQualifierSymbol
).also { new ->
  new.receiver = receiver
  new.attributeOwnerId = attributeOwnerId
}

/**
 * @author 凛 (RinOrz)
 */
fun IrSetField.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  symbol: IrFieldSymbol = this.symbol,
  type: IrType = this.type,
  origin: IrStatementOrigin? = this.origin,
  superQualifierSymbol: IrClassSymbol? = this.superQualifierSymbol,
  receiver: IrExpression? = this.receiver,
  value: IrExpression = this.value,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId
) = IrSetFieldImpl(
  startOffset,
  endOffset,
  symbol,
  type,
  origin,
  superQualifierSymbol
).also { new ->
  new.receiver = receiver
  new.attributeOwnerId = attributeOwnerId
  new.value = value
}

/**
 * @author 凛 (RinOrz)
 */
fun IrCall.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  symbol: IrSimpleFunctionSymbol = this.symbol,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  origin: IrStatementOrigin? = this.origin,
  superQualifierSymbol: IrClassSymbol? = this.superQualifierSymbol,
) = IrCallImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  valueArguments.size,
  origin,
  superQualifierSymbol
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrCall.copyToConstructorCall(
  symbol: IrConstructorSymbol,
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  // FIXME: Maybe this is a problem
  constructorTypeArguments: Array<IrType?> = emptyArray(),
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  origin: IrStatementOrigin? = this.origin,
) = IrConstructorCallImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  constructorTypeArguments.size,
  valueArguments.size,
  origin,
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  constructorTypeArguments.forEachIndexed { index, type -> new.putConstructorTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrCall.copyToEnumConstructorCall(
  symbol: IrConstructorSymbol,
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
) = IrEnumConstructorCallImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  valueArguments.size,
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrCall.copyToDelegatingConstructorCall(
  symbol: IrConstructorSymbol,
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
) = IrDelegatingConstructorCallImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  valueArguments.size,
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrPropertyReference.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  symbol: IrPropertySymbol = this.symbol,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  field: IrFieldSymbol? = this.field,
  getter: IrSimpleFunctionSymbol? = this.getter,
  setter: IrSimpleFunctionSymbol? = this.setter,
  origin: IrStatementOrigin? = this.origin,
) = IrPropertyReferenceImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  field,
  getter,
  setter,
  origin
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrFunctionReference.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  symbol: IrFunctionSymbol = this.symbol,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  reflectionTarget: IrFunctionSymbol? = symbol,
  origin: IrStatementOrigin? = this.origin,
) = IrFunctionReferenceImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  valueArguments.size,
  reflectionTarget,
  origin,
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrRawFunctionReference.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  symbol: IrFunctionSymbol = this.symbol,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
) = IrRawFunctionReferenceImpl(
  startOffset,
  endOffset,
  type,
  symbol,
).also { new ->
  new.attributeOwnerId = attributeOwnerId
}

/**
 * @author 凛 (RinOrz)
 */
fun IrReturn.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  value: IrExpression = this.value,
  returnTargetSymbol: IrReturnTargetSymbol = this.returnTargetSymbol,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
) = IrReturnImpl(
  startOffset,
  endOffset,
  type,
  returnTargetSymbol,
  value
).also { new ->
  new.attributeOwnerId = attributeOwnerId
}

/**
 * @author 凛 (RinOrz)
 */
fun IrLocalDelegatedPropertyReference.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  symbol: IrLocalDelegatedPropertySymbol = this.symbol,
  delegate: IrVariableSymbol = this.delegate,
  getter: IrSimpleFunctionSymbol = this.getter,
  setter: IrSimpleFunctionSymbol? = this.setter,
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  origin: IrStatementOrigin? = this.origin,
) = IrLocalDelegatedPropertyReferenceImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  delegate,
  getter,
  setter,
  origin
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
}

/**
 * @author 凛 (RinOrz)
 */
fun IrConstructorCall.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  symbol: IrConstructorSymbol = this.symbol,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  constructorTypeArguments: Array<IrType?> = Array(this.constructorTypeArgumentsCount) { this.getConstructorTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  origin: IrStatementOrigin? = this.origin,
) = IrConstructorCallImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  constructorTypeArguments.size,
  valueArguments.size,
  origin,
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  constructorTypeArguments.forEachIndexed { index, type -> new.putConstructorTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrConstructorCall.copyToCall(
  symbol: IrSimpleFunctionSymbol,
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  origin: IrStatementOrigin? = this.origin,
  superQualifierSymbol: IrClassSymbol? = null,
) = IrCallImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  valueArguments.size,
  origin,
  superQualifierSymbol
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrDelegatingConstructorCall.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  symbol: IrConstructorSymbol = this.symbol,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
) = IrDelegatingConstructorCallImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  valueArguments.size,
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrDelegatingConstructorCall.copyToCall(
  symbol: IrSimpleFunctionSymbol,
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  origin: IrStatementOrigin? = this.origin,
  superQualifierSymbol: IrClassSymbol? = null,
) = IrCallImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  valueArguments.size,
  origin,
  superQualifierSymbol
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrEnumConstructorCall.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  symbol: IrConstructorSymbol = this.symbol,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
) = IrEnumConstructorCallImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  valueArguments.size,
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrEnumConstructorCall.copyToCall(
  symbol: IrSimpleFunctionSymbol,
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  typeArguments: Array<IrType?> = Array(this.typeArgumentsCount) { this.getTypeArgument(it) },
  valueArguments: Array<IrExpression?> = Array(this.valueArgumentsCount) { this.getValueArgument(it) },
  dispatchReceiver: IrExpression? = this.dispatchReceiver,
  extensionReceiver: IrExpression? = this.extensionReceiver,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
  origin: IrStatementOrigin? = this.origin,
  superQualifierSymbol: IrClassSymbol? = null,
) = IrCallImpl(
  startOffset,
  endOffset,
  type,
  symbol,
  typeArguments.size,
  valueArguments.size,
  origin,
  superQualifierSymbol
).also { new ->
  new.attributeOwnerId = attributeOwnerId
  new.dispatchReceiver = dispatchReceiver
  new.extensionReceiver = extensionReceiver
  typeArguments.forEachIndexed { index, type -> new.putTypeArgument(index, type) }
  valueArguments.forEachIndexed { index, value -> new.putValueArgument(index, value) }
}

/**
 * @author 凛 (RinOrz)
 */
fun IrGetObjectValue.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  symbol: IrClassSymbol = this.symbol,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
) = IrGetObjectValueImpl(
  startOffset,
  endOffset,
  type,
  symbol,
).also { new ->
  new.attributeOwnerId = attributeOwnerId
}

/**
 * @author 凛 (RinOrz)
 */
fun IrInstanceInitializerCall.copy(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  type: IrType = this.type,
  classSymbol: IrClassSymbol = this.classSymbol,
  attributeOwnerId: IrAttributeContainer = this.attributeOwnerId,
) = IrInstanceInitializerCallImpl(
  startOffset,
  endOffset,
  classSymbol,
  type,
).also { new ->
  new.attributeOwnerId = attributeOwnerId
}
