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

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isObject

/**
 * Returns true if this expression accesses 'this' class.
 *
 * @author 凛 (RinOrz)
 */
val IrExpression.isAccessThisClass: Boolean
  get() {
    // Todo: More refined detect
    if (this !is IrGetValue) return false
    val target = this.symbol.owner as? IrValueParameter ?: return false
    val expectedClass = this.type.classOrNull?.owner
    if (expectedClass == null || expectedClass.isObject) return false
    return target.origin == IrDeclarationOrigin.INSTANCE_RECEIVER && target.name.asString() == "<this>"
  }
