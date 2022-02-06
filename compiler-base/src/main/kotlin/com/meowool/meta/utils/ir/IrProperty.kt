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
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

/**
 * @author 凛 (RinOrz)
 */
inline val IrProperty.isVal: Boolean get() = isVar.not()

/**
 * @author 凛 (RinOrz)
 */
val IrProperty.type: IrType get() = backingField?.type ?: getter?.returnType ?: error("Property $name type not found!")

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrProperty.addBackingField(
  startOffset: Int = this.startOffset,
  endOffset: Int = this.endOffset,
  origin: IrDeclarationOrigin = this.origin,
  symbol: IrFieldSymbol = IrFieldSymbolImpl(this.descriptor),
  name: Name = this.name,
  type: IrType = this.type,
  visibility: DescriptorVisibility = this.visibility,
  isFinal: Boolean = false,
  isExternal: Boolean = this.isExternal,
  isStatic: Boolean = this.isStatic,
  parent: IrDeclarationParent = this.parent,
  annotations: List<IrConstructorCall> = this.annotations,
  metadata: MetadataSource? = this.metadata,
  correspondingPropertySymbol: IrPropertySymbol? = this.symbol,
  initializer: IrExpressionBody? = null,
) {
  backingField = factory.createField(
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
}
