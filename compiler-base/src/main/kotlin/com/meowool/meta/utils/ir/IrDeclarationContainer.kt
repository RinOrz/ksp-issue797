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

import com.meowool.sweekt.castOrNull
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrProperty

@PublishedApi
internal val deferredDeclarations = mutableMapOf<IrDeclarationContainer, MutableList<IrDeclaration>>()

@PublishedApi
internal inline fun <R> IrDeclarationContainer.runDeclarations(block: List<IrDeclaration>.() -> R) =
  deferredDeclarations[this] ?: declarations.block()

fun IrDeclarationContainer.addDeferredChild(declaration: IrDeclaration) {
  deferredDeclarations.getOrPut(this) { mutableListOf() }.add(declaration)
}

inline fun IrDeclarationContainer.findProperty(predicate: (IrProperty) -> Boolean): IrProperty? = runDeclarations {
  find { it is IrProperty && predicate(it) }
}.castOrNull<IrProperty>()

fun IrDeclarationContainer.findPropertyByName(name: String): IrProperty? =
  findProperty { it.name.asString() == name }
