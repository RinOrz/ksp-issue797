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
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

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
): IrField = let { old ->
  factory.createField(startOffset, endOffset, origin, symbol, name, type, visibility, isFinal, isExternal, isStatic)
    .apply {
      parent = old.parent
      annotations = old.annotations
      metadata = old.metadata
      correspondingPropertySymbol = old.correspondingPropertySymbol
      initializer = old.initializer
    }
}
