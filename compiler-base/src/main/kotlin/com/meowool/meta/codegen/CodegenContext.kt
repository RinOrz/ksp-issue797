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
@file:Suppress("MemberVisibilityCanBePrivate", "NOTHING_TO_INLINE")

package com.meowool.meta.codegen

import com.meowool.meta.MetaExtension
import com.meowool.meta.internal.MetaReferencedSymbolRemapper
import com.meowool.meta.utils.ir.addDeferredChild
import com.meowool.meta.utils.ir.irGetField
import com.meowool.meta.utils.ir.irReturnExprBody
import com.meowool.meta.utils.ir.irSetField
import com.meowool.meta.utils.ir.isAccessThisInstanceReceiver
import com.meowool.meta.utils.ir.isStatic
import com.meowool.meta.utils.ir.thisReceiver
import com.meowool.meta.utils.ir.type
import com.meowool.sweekt.ifNull
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.IrPropertyBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * @author 凛 (RinOrz)
 */
abstract class CodegenContext(
  val pluginContext: IrPluginContext,
  val moduleFragment: IrModuleFragment,
  val metaContext: MetaExtension.Context,
  val referencedSymbolRemapper: MetaReferencedSymbolRemapper,
) : IrGeneratorContext {
  override val irBuiltIns: IrBuiltIns get() = pluginContext.irBuiltIns

  val languageVersionSettings: LanguageVersionSettings get() = pluginContext.languageVersionSettings
  val symbols: BuiltinSymbolsBase get() = pluginContext.symbols
  val platform: TargetPlatform? get() = pluginContext.platform

  fun referenceClass(fqName: FqName): IrClassSymbol? = pluginContext.referenceClass(fqName)
  fun referenceTypeAlias(fqName: FqName): IrTypeAliasSymbol? = pluginContext.referenceTypeAlias(fqName)

  fun referenceConstructorsSymbols(classFqn: FqName): Collection<IrConstructorSymbol> =
    pluginContext.referenceConstructors(classFqn)

  fun referenceConstructors(classFqn: FqName): Sequence<IrConstructor> =
    referenceConstructorsSymbols(classFqn).asSequence().map { it.owner }

  fun referenceFunctionsSymbols(fqName: FqName): Collection<IrSimpleFunctionSymbol> =
    pluginContext.referenceFunctions(fqName)

  fun referenceFunctions(fqName: FqName): Sequence<IrSimpleFunction> =
    referenceFunctionsSymbols(fqName).asSequence().map { it.owner }

  fun referencePropertiesSymbols(fqName: FqName): Collection<IrPropertySymbol> =
    pluginContext.referenceProperties(fqName)

  fun referenceProperties(fqName: FqName): Sequence<IrProperty> =
    referencePropertiesSymbols(fqName).asSequence().map { it.owner }

  fun referenceTopLevel(
    signature: IdSignature,
    kind: IrDeserializer.TopLevelSymbolKind,
    moduleDescriptor: ModuleDescriptor
  ): IrSymbol? = pluginContext.referenceTopLevel(signature, kind, moduleDescriptor)

  // ////////////////////////////////////////////////////////////////////////
  // //                            Remapping                             ////
  // ////////////////////////////////////////////////////////////////////////

  infix fun IrClassSymbol?.remapTo(new: IrClassSymbol?) {
    if (this == new) return
    if (this == null || new == null) return
    referencedSymbolRemapper.oldToNewClasses[this] = new
  }

  infix fun IrClass?.remapSymbolTo(new: IrClass?) = this?.symbol.remapTo(new?.symbol)

  infix fun IrSimpleFunctionSymbol?.remapTo(new: IrFunctionSymbol?) {
    if (this == new) return
    if (this == null || new == null) return
    referencedSymbolRemapper.oldToNewSimpleFunctions[this] = new
  }

  infix fun IrSimpleFunction?.remapSymbolTo(new: IrFunction?) = this?.symbol.remapTo(new?.symbol)

  infix fun IrConstructorSymbol?.remapTo(new: IrFunctionSymbol?) {
    if (this == new) return
    if (this == null || new == null) return
    referencedSymbolRemapper.oldToNewConstructors[this] = new
  }

  infix fun IrConstructor?.remapSymbolTo(new: IrFunction?) = this?.symbol.remapTo(new?.symbol)

  infix fun IrPropertySymbol?.remapTo(new: IrPropertySymbol?) {
    if (this == new) return
    if (this == null || new == null) return
    referencedSymbolRemapper.oldToNewProperties[this] = new
  }

  infix fun IrProperty?.remapSymbolTo(new: IrProperty?) = this?.symbol.remapTo(new?.symbol)

  infix fun IrFieldSymbol?.remapTo(new: IrFieldSymbol?) {
    if (this == new) return
    if (this == null || new == null) return
    referencedSymbolRemapper.oldToNewFields[this] = new
  }

  infix fun IrField?.remapSymbolTo(new: IrField?) = this?.symbol.remapTo(new?.symbol)

  // ////////////////////////////////////////////////////////////////////////
  // //                             Ir Utils                             ////
  // ////////////////////////////////////////////////////////////////////////

  inline val builtIns: IrBuiltIns get() = pluginContext.irBuiltIns
  inline val factory: IrFactory get() = pluginContext.irFactory

  fun <E : IrElement> E.process(processor: CodeProcessor) = transform(processor.init(), null)

  fun <E : IrElement> E.processChildren(processor: CodeProcessor) = apply { transformChildrenVoid(processor.init()) }

  fun buildValueParameterList(builder: ValueParameterListBuilder.() -> Unit): List<IrValueParameter> =
    ValueParameterListBuilder().apply(builder)

  inner class ValueParameterListBuilder internal constructor() : MutableList<IrValueParameter> by mutableListOf() {
    inline operator fun IrValueParameter.unaryPlus() = add(this)

    fun add(
      type: IrType,
      name: Name = Name.identifier("p$size"),
      defaultValue: (IrBuilderWithScope.() -> IrExpressionBody)? = null,
      index: Int = size,
      startOffset: Int = SYNTHETIC_OFFSET,
      endOffset: Int = SYNTHETIC_OFFSET,
      origin: IrDeclarationOrigin = DeclarationOrigin.Synthetic,
      symbol: IrValueParameterSymbol = IrValueParameterSymbolImpl(),
      varargElementType: IrType? = null,
      isCrossinline: Boolean = false,
      isNoinline: Boolean = false,
      isHidden: Boolean = false,
      isAssignable: Boolean = false,
    ) {
      IrValueParameterImpl(
        startOffset,
        endOffset,
        origin,
        symbol,
        name,
        index,
        type,
        varargElementType,
        isCrossinline,
        isNoinline,
        isHidden,
        isAssignable
      ).also {
        it.defaultValue = defaultValue?.let { block ->
          it.buildIr { block() }
        }
        add(it)
      }
    }
  }

  fun IrDeclarationContainer.addConstructor(
    original: IrConstructor? = null,
    valueParameters: List<IrValueParameter> = original?.valueParameters.orEmpty(),
    origin: IrDeclarationOrigin = DeclarationOrigin.Synthetic,
    dispatchReceiverParameter: IrValueParameter? = null,
    isPrimary: Boolean = original?.isPrimary == true,
    body: (IrBuilderWithScope.(IrConstructor) -> IrBlockBody)? = null,
    builder: IrFunctionBuilder.() -> Unit = {}
  ): IrConstructor = factory.buildConstructor {
    if (original != null) updateFrom(original)
    if (this@addConstructor is IrClass) returnType = defaultType
    builder()
    this.isPrimary = isPrimary
    this.origin = origin
  }.also { constructor ->
    constructor.parent = this
    constructor.dispatchReceiverParameter = dispatchReceiverParameter ?: constructor.thisReceiver
    constructor.valueParameters = valueParameters
    if (body != null) constructor.body = constructor.buildIr { body(constructor) }
    this.addDeferredChild(constructor)
  }

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
    this.addDeferredChild(property)
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

  inline fun IrProperty.getOrAddGetter(builder: IrFunctionBuilder.() -> Unit = {}): IrSimpleFunction = getter.ifNull {
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
  ): R = DeclarationIrBuilder(pluginContext, this.symbol, startOffset, endOffset).run(block)

  inline fun <R> IrDeclarationReference.buildIr(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    block: DeclarationIrBuilder.() -> R
  ): R = DeclarationIrBuilder(pluginContext, this.symbol, startOffset, endOffset).run(block)

  fun IrProperty.copyInitializerExprTo(function: IrFunction): IrExpression? = backingField?.initializer?.expression
    ?.transform(
      object : IrElementTransformerVoid() {
        override fun visitGetValue(expression: IrGetValue): IrExpression = when {
          expression.isAccessThisInstanceReceiver -> IrGetValueImpl(
            startOffset,
            endOffset,
            function.thisReceiver!!.symbol
          )
          else -> super.visitGetValue(expression)
        }
      },
      null
    )

  fun IrProperty.moveInitializerExprTo(function: IrFunction): IrExpression? =
    copyInitializerExprTo(function)?.apply { backingField?.initializer = null }

  fun IrSimpleFunction.syntheticDefaultAccessor() = apply {
    if (origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
      origin = DeclarationOrigin.SyntheticDefaultPropertyAccessor
    }
  }

  @PublishedApi internal fun IrFunctionBuilder.initDefaultGetter(property: IrProperty) {
    name = Name.special("<get-${property.name}>")
    origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
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
    origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
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

  private fun CodeProcessor.init() = also { new ->
    new.pluginContext = this@CodegenContext.pluginContext
    new.moduleFragment = this@CodegenContext.moduleFragment
    new.referencedSymbolRemapper = this@CodegenContext.referencedSymbolRemapper
  }
}
