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
@file:Suppress("EqualsOrHashCode", "NAME_SHADOWING")

package com.meowool.meta.identifiers

import com.meowool.sweekt.isNotNull
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.resolve.calls.components.isVararg

/**
 * @author 凛 (RinOrz)
 */
data class ParameterIdentifier internal constructor(
  val name: String,
  val type: TypeIdentifier,
  val isVararg: Boolean,
  val hasDefaultValue: Boolean
) : Identifier {
  override fun equalTo(other: Any?): Boolean {
    if (this === other) return true
    if (this.hashCode() == other.hashCode()) return true

    when (other) {
      is ValueParameterDescriptor -> {
        val other = other.original
        if (name != other.name.asString()) return false
        if (isVararg != other.isVararg) return false
        if (!type.equalTo(other.type)) return false
        if (hasDefaultValue != other.declaresDefaultValue()) return false
      }
      is IrValueParameter -> {
        if (name != other.name.asString()) return false
        if (isVararg != other.isVararg) return false
        if (!type.equalTo(other.type)) return false
        if (hasDefaultValue != other.defaultValue.isNotNull()) return false
      }
      else -> return false
    }
    return true
  }
}
