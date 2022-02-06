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

import com.meowool.meta.CodeProcessors
import com.meowool.meta.MetaExtension
import com.meowool.meta.codegen.CallCodegenContext
import com.meowool.meta.codegen.CallCodegenPremise
import com.meowool.meta.codegen.ClassCodegenContext
import com.meowool.meta.codegen.ClassCodegenPremise
import com.meowool.meta.codegen.CodeProcessor
import com.meowool.meta.codegen.ConstructorCodegenContext
import com.meowool.meta.codegen.ConstructorCodegenPremise
import com.meowool.meta.codegen.FunctionCodegenContext
import com.meowool.meta.codegen.FunctionCodegenPremise
import com.meowool.meta.codegen.PropertyCodegenContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * @author 凛 (RinOrz)
 */
internal object CodeFactory : CodeProcessors {
  override fun clasѕ(premise: ClassCodegenPremise, processing: ClassCodegenContext.() -> Unit): CodeProcessor =
    object : CodeProcessor() {
      override var context: MetaExtension.Context = MetaExtension.Context.Default

      override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.premise().not()) return super.visitClass(declaration)

        CodegenContextImpl(
          pluginContext,
          moduleFragment,
          context,
          referencedSymbolRemapper,
          declaration
        ).apply {
          processing()
          return when (val result = result) {
            is IrClass -> {
              declaration.symbol remapTo result.symbol
              // TODO Remap all declarations
              super.visitClass(result)
            }
            else -> result
          }
        }
      }

      inner class CodegenContextImpl(
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
        metaContext: MetaExtension.Context,
        referencedSymbolRemapper: MetaReferencedSymbolRemapper,
        clasѕ: IrClass,
      ) : ClassCodegenContext(pluginContext, moduleFragment, metaContext, referencedSymbolRemapper) {
        private var _class: IrClass? = clasѕ
        override var result: IrStatement = clasѕ
          set(value) {
            field = value
            _class = result as? IrClass
          }
        override val clasѕ: IrClass
          get() = _class ?: error("The result has been modified, it is not a constructor")
      }
    }

  override fun constructor(
    premise: ConstructorCodegenPremise,
    processing: ConstructorCodegenContext.() -> Unit
  ): CodeProcessor = object : CodeProcessor() {
    override var context: MetaExtension.Context = MetaExtension.Context.Default

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
      if (declaration.premise().not()) return super.visitConstructor(declaration)

      ConstructorCodegenContextImpl(
        pluginContext,
        moduleFragment,
        context,
        referencedSymbolRemapper,
        declaration
      ).apply {
        processing()
        return when (val result = result) {
          is IrFunction -> {
            declaration.symbol remapTo result.symbol
            when (result) {
              is IrConstructor -> super.visitConstructor(result)
              is IrSimpleFunction -> super.visitSimpleFunction(result)
              else -> super.visitFunction(result)
            }
          }
          else -> result
        }
      }
    }

    inner class ConstructorCodegenContextImpl(
      pluginContext: IrPluginContext,
      moduleFragment: IrModuleFragment,
      metaContext: MetaExtension.Context,
      referencedSymbolRemapper: MetaReferencedSymbolRemapper,
      constructor: IrConstructor,
    ) : ConstructorCodegenContext(pluginContext, moduleFragment, metaContext, referencedSymbolRemapper) {
      private var _constructor: IrConstructor? = constructor
      override var result: IrStatement = constructor
        set(value) {
          field = value
          _constructor = result as? IrConstructor
        }

      override val constructor: IrConstructor
        get() = _constructor ?: error("The result has been modified, it is not a constructor")
    }
  }

  override fun function(premise: FunctionCodegenPremise, processing: FunctionCodegenContext.() -> Unit): CodeProcessor =
    object : CodeProcessor() {
      override var context: MetaExtension.Context = MetaExtension.Context.Default

      override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (declaration.premise().not()) return super.visitSimpleFunction(declaration)

        FunctionCodegenContextImpl(
          pluginContext,
          moduleFragment,
          context,
          referencedSymbolRemapper,
          declaration
        ).apply {
          processing()
          return when (val result = result) {
            is IrFunction -> {
              declaration.symbol remapTo result.symbol
              when (result) {
                is IrConstructor -> super.visitConstructor(result)
                is IrSimpleFunction -> super.visitSimpleFunction(result)
                else -> super.visitFunction(result)
              }
              super.visitFunction(result)
            }
            else -> result
          }
        }
      }

      inner class FunctionCodegenContextImpl(
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
        metaContext: MetaExtension.Context,
        referencedSymbolRemapper: MetaReferencedSymbolRemapper,
        function: IrSimpleFunction,
      ) : FunctionCodegenContext(pluginContext, moduleFragment, metaContext, referencedSymbolRemapper) {
        private var _function: IrSimpleFunction? = function
        override var result: IrStatement = function
          set(value) {
            field = value
            _function = result as? IrSimpleFunction
          }

        override val function: IrSimpleFunction
          get() = _function ?: error("The result has been modified, it is not a function")
      }
    }

  override fun property(
    premise: IrProperty.() -> Boolean,
    processing: PropertyCodegenContext.() -> Unit
  ): CodeProcessor = object : CodeProcessor() {
    override var context: MetaExtension.Context = MetaExtension.Context.Default

    override fun visitProperty(declaration: IrProperty): IrStatement {
      if (declaration.premise().not()) return super.visitProperty(declaration)

      PropertyCodegenContextImpl(
        pluginContext,
        moduleFragment,
        context,
        referencedSymbolRemapper,
        declaration
      ).apply {
        processing()
        return when (val result = result) {
          is IrProperty -> {
            declaration.symbol remapTo result.symbol
            declaration.backingField?.symbol remapTo result.backingField?.symbol
            declaration.getter?.symbol remapTo result.getter?.symbol
            declaration.setter?.symbol remapTo result.setter?.symbol
            super.visitProperty(result)
          }
          else -> result
        }
      }
    }

    inner class PropertyCodegenContextImpl(
      pluginContext: IrPluginContext,
      moduleFragment: IrModuleFragment,
      metaContext: MetaExtension.Context,
      referencedSymbolRemapper: MetaReferencedSymbolRemapper,
      property: IrProperty,
    ) : PropertyCodegenContext(pluginContext, moduleFragment, metaContext, referencedSymbolRemapper) {
      private var _property: IrProperty? = property
      override var result: IrStatement = property
        set(value) {
          field = value
          _property = result as? IrProperty
        }

      override val property: IrProperty
        get() = _property ?: error("The result has been modified, it is not a property")
    }
  }

  override fun call(premise: CallCodegenPremise, processing: CallCodegenContext.() -> Unit): CodeProcessor =
    object : CodeProcessor() {
      override var context: MetaExtension.Context = MetaExtension.Context.Default

      override fun visitCall(expression: IrCall): IrExpression {
        if (expression.premise().not()) return super.visitCall(expression)

        CallCodegenContextImpl(
          pluginContext,
          moduleFragment,
          context,
          referencedSymbolRemapper,
          expression
        ).apply {
          processing()
          return when (val result = result) {
            is IrCall -> super.visitCall(result)
            else -> result
          }
        }
      }

      inner class CallCodegenContextImpl(
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
        metaContext: MetaExtension.Context,
        referencedSymbolRemapper: MetaReferencedSymbolRemapper,
        call: IrCall,
      ) : CallCodegenContext(pluginContext, moduleFragment, metaContext, referencedSymbolRemapper) {
        private var _call: IrCall? = call

        override val call: IrCall
          get() = _call ?: error("The result has been modified, it is not a 'IrCall'")

        override var result: IrExpression = call
          set(value) {
            field = value
            _call = result as? IrCall
          }
      }
    }

  override fun onStart(callback: (context: IrPluginContext, module: IrModuleFragment) -> Unit): CodeProcessor.Callback.Start =
    object : CodeProcessor.Callback.Start() {
      override var context: MetaExtension.Context = MetaExtension.Context.Default
      override fun action(context: IrPluginContext, module: IrModuleFragment) = callback(context, module)
    }

  override fun onEnd(callback: (context: IrPluginContext, module: IrModuleFragment) -> Unit): CodeProcessor.Callback.End =
    object : CodeProcessor.Callback.End() {
      override var context: MetaExtension.Context = MetaExtension.Context.Default
      override fun action(context: IrPluginContext, module: IrModuleFragment) = callback(context, module)
    }
}
