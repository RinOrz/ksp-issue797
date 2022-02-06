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

import com.meowool.meta.utils.ir.deferredDeclarations
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Patch child declarations:
 *  add all added [deferredDeclarations] to the container after processing.
 *
 * @author 凛 (RinOrz)
 */
@InternalCompilerApi
abstract class PatchDeclarationContainerTransformer : IrElementTransformerVoid() {
  override fun visitFile(declaration: IrFile): IrFile =
    super.visitFile(declaration).patch()

  override fun visitPackageFragment(declaration: IrPackageFragment): IrPackageFragment =
    super.visitPackageFragment(declaration).patch()

  override fun visitClass(declaration: IrClass): IrStatement =
    super.visitClass(declaration).patch()

  private fun <E : IrElement> E.patch() = also { container ->
    if (container is IrDeclarationContainer) deferredDeclarations[container]?.let { deferred ->
      container.declarations += deferred
      deferredDeclarations.remove(container)
    }
  }
}
