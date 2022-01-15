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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.meowool.meta.codegen

import com.meowool.meta.utils.ir.irGetField
import com.meowool.meta.utils.ir.irReturnExprBody
import com.meowool.meta.utils.ir.irSetField
import com.meowool.meta.utils.ir.isAccessThisClass
import com.meowool.meta.utils.ir.isStatic
import com.meowool.meta.utils.ir.thisReceiver
import com.meowool.meta.utils.ir.type
import com.meowool.sweekt.ifNull
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.IrPropertyBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * @author 凛 (RinOrz)
 */
abstract class CodegenContext(val base: IrPluginContext) : IrGeneratorContext {
  override val irBuiltIns: IrBuiltIns get() = base.irBuiltIns

  val languageVersionSettings: LanguageVersionSettings get() = base.languageVersionSettings
  val symbols: BuiltinSymbolsBase get() = base.symbols
  val platform: TargetPlatform? get() = base.platform

  fun referenceClass(fqName: FqName): IrClassSymbol? = base.referenceClass(fqName)
  fun referenceTypeAlias(fqName: FqName): IrTypeAliasSymbol? = base.referenceTypeAlias(fqName)
  fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol> = base.referenceConstructors(classFqn)
  fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol> = base.referenceFunctions(fqName)
  fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol> = base.referenceProperties(fqName)

  fun referenceTopLevel(
    signature: IdSignature,
    kind: IrDeserializer.TopLevelSymbolKind,
    moduleDescriptor: ModuleDescriptor
  ): IrSymbol? = base.referenceTopLevel(signature, kind, moduleDescriptor)

  // ////////////////////////////////////////////////////////////////////////
  // //                             Ir Utils                             ////
  // ////////////////////////////////////////////////////////////////////////

  inline val builtIns: IrBuiltIns get() = base.irBuiltIns
  inline val factory: IrFactory get() = base.irFactory

  fun IrDeclarationContainer.addProperty(
    name: String,
    original: IrProperty? = null,
    origin: IrDeclarationOrigin = DeclarationOrigin.Synthetic,
    type: IrType? = original?.type,
    isStatic: Boolean = original?.isStatic == true,
    initializer: (IrBuilderWithScope.() -> IrExpression)? = null,
    builder: IrPropertyBuilder.() -> Unit = {}
  ): IrProperty = factory.buildProperty {
    if (original != null) updateFrom(original)
    builder()
    this.name = Name.identifier(name)
    this.origin = origin
  }.also { property ->
    property.parent = this
    property.backingField = factory.buildField {
      this.name = property.name
      this.type = type ?: builtIns.anyType
      this.isStatic = isStatic
    }.also { field ->
      field.parent = this
      field.correspondingPropertySymbol = property.symbol
      if (initializer != null) field.initializer = field.buildIr(field.startOffset, field.endOffset) {
        irExprBody(initializer())
      }
    }
    addChild(property)
  }

  inline fun IrProperty.createGetter(
    builder: IrFunctionBuilder.() -> Unit = {}
  ): IrSimpleFunction = irFactory.buildFun {
    initDefaultGetter(this@createGetter)
    builder()
  }.initDefaultGetter(this)

  inline fun IrProperty.addGetterIfAbsent(
    builder: IrFunctionBuilder.() -> Unit = {},
    block: IrSimpleFunction.() -> Unit = {}
  ) {
    if (getter == null) getter = createGetter(builder).apply(block)
  }

  fun IrProperty.getOrAddGetter(builder: IrFunctionBuilder.() -> Unit = {}): IrSimpleFunction = getter.ifNull {
    createGetter(builder).also { getter = it }
  }

  inline fun IrProperty.createSetter(
    builder: IrFunctionBuilder.() -> Unit = {}
  ): IrSimpleFunction = irFactory.buildFun {
    initDefaultSetter(this@createSetter)
    builder()
  }.initDefaultSetter(this)

  inline fun IrProperty.addSetterIfAbsent(
    builder: IrFunctionBuilder.() -> Unit = {},
    block: IrSimpleFunction.() -> Unit = {}
  ) {
    if (setter == null) setter = createSetter(builder).apply(block)
  }

  fun IrProperty.getOrAddSetter(builder: IrFunctionBuilder.() -> Unit = {}): IrSimpleFunction = setter.ifNull {
    createSetter(builder).also { setter = it }
  }

  inline fun <R> IrSymbolOwner.buildIr(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    block: DeclarationIrBuilder.() -> R
  ): R = DeclarationIrBuilder(base, this.symbol, startOffset, endOffset).run(block)

  inline fun <R> IrDeclarationReference.buildIr(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    block: DeclarationIrBuilder.() -> R
  ): R = DeclarationIrBuilder(base, this.symbol, startOffset, endOffset).run(block)

  fun IrProperty.copyInitializerExprTo(function: IrFunction): IrExpression? = backingField?.initializer?.expression
    ?.transform(
      object : IrElementTransformerVoid() {
        override fun visitGetValue(expression: IrGetValue): IrExpression = when {
          expression.isAccessThisClass -> IrGetValueImpl(startOffset, endOffset, function.thisReceiver!!.symbol)
          else -> super.visitGetValue(expression)
        }
      },
      null
    )

  fun IrProperty.moveInitializerExprTo(function: IrFunction): IrExpression? =
    copyInitializerExprTo(function)?.apply { backingField?.initializer = null }

  @PublishedApi internal fun IrFunctionBuilder.initDefaultGetter(property: IrProperty) {
    name = Name.special("<get-${property.name}>")
    origin = DeclarationOrigin.SyntheticDefaultPropertyAccessor
    returnType = property.type
    property.getter?.let(::updateFrom)
  }

  @PublishedApi internal fun IrSimpleFunction.initDefaultGetter(property: IrProperty) = apply {
    parent = property.parent
    correspondingPropertySymbol = property.symbol
    dispatchReceiverParameter = thisReceiver
    if (property.backingField != null) {
      body = buildIr(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
        irReturnExprBody(irGetField(dispatchReceiverParameter?.let(::irGet), property))
      }
    }
  }

  @PublishedApi internal fun IrFunctionBuilder.initDefaultSetter(property: IrProperty) {
    name = Name.special("<set-${property.name}>")
    origin = DeclarationOrigin.SyntheticDefaultPropertyAccessor
    returnType = builtIns.unitType
    property.setter?.let(::updateFrom)
  }

  @PublishedApi internal fun IrSimpleFunction.initDefaultSetter(property: IrProperty) = apply {
    parent = property.parent
    correspondingPropertySymbol = property.symbol
    dispatchReceiverParameter = thisReceiver
    val valueParam = addValueParameter(Name.special("<set-?>"), property.type)
    if (property.backingField != null) body = buildIr(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
      irExprBody(irSetField(dispatchReceiverParameter?.let(::irGet), property, irGet(valueParam)))
    }
  }
}
