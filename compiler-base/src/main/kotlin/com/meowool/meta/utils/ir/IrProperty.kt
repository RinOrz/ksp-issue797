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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/**
 * @author 凛 (RinOrz)
 */
val IrProperty.isMutable: Boolean get() = isVar && backingField?.isFinal == false

/**
 * @author 凛 (RinOrz)
 */
val IrProperty.isImmutable: Boolean get() = !isVar || backingField?.isFinal == true

/**
 * @author 凛 (RinOrz)
 */
val IrProperty.type: IrType get() = backingField?.type ?: getter?.returnType ?: error("Property $name type not found!")

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
  containerSource: DeserializedContainerSource? = this.containerSource
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

  backingField?.also { field ->
    // fix wrong 'final' modifier
    backingField = if (isVar && field.isFinal) {
      field.copy(isFinal = false)
    } else {
      field
    }.also { it.correspondingPropertySymbol = symbol }
  }
}
