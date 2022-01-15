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
import com.meowool.meta.codegen.CodeProcessor
import com.meowool.meta.codegen.PropertyCodegenContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol

/**
 * @author 凛 (RinOrz)
 */
internal object CodeFactory : CodeProcessors {
  override fun property(
    premise: IrProperty.() -> Boolean,
    processing: PropertyCodegenContext.() -> Unit
  ): CodeProcessor = object : CodeProcessor() {
    override var context: MetaExtension.Context = MetaExtension.Context.Default

    override fun visitProperty(declaration: IrProperty): IrStatement = when {
      declaration.premise() -> PropertyCodegenContextImpl(pluginContext, declaration).apply(processing).result.also {
        if (it is IrProperty) {
          symbolRemapper.properties.remap(declaration.symbol, it.symbol)
          symbolRemapper.fields.remap(declaration.backingField?.symbol, it.backingField?.symbol)
        }
      }
      else -> super.visitProperty(declaration)
    }
  }

  override fun call(premise: CallCodegenPremise, processing: CallCodegenContext.() -> Unit): CodeProcessor {
    TODO("Not yet implemented")
  }

  inline fun <D : DeclarationDescriptor, B : IrSymbolOwner, reified S : IrBindableSymbol<D, B>>
    MutableMap<S, S>.remap(old: S?, new: S?) {
    if (old != null && new != null) {
      this[old] = new
    }
  }

  class PropertyCodegenContextImpl(base: IrPluginContext, property: IrProperty) :
    PropertyCodegenContext(base) {
    override var result: IrStatement = property

    override val property: IrProperty
      get() = result as? IrProperty ?: error("The result has been modified, it is not a property")
  }
}
