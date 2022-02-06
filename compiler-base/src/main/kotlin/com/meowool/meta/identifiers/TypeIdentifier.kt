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
@file:Suppress("EqualsOrHashCode")

package com.meowool.meta.identifiers

import com.meowool.meta.utils.fqName
import com.meowool.sweekt.iteration.isNotEmpty
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable

/**
 * @author 凛 (RinOrz)
 */
class TypeIdentifier internal constructor(
  val fqName: FqName?,
  private val isNullable: Boolean,
  /** `null` indicates star projection. */
  private val arguments: List<TypeIdentifier?>,
) : Identifier {

  override fun equalTo(other: Any?): Boolean {
    if (this === other) return true
    if (this.hashCode() == other.hashCode()) return true

    when (other) {
      is KotlinType -> {
        if (isNullable != other.isMarkedNullable) return false
        if (fqName != null) if (fqName.asString() != other.fqName) return false
        if (arguments.size != other.arguments.size) return false
        arguments.forEachIndexed { index, arg ->
          if (arg != null && !arg.equalTo(other.arguments[index].type)) return false
        }
      }
      is IrType -> {
        if (isNullable != other.isMarkedNullable()) return false
        if (fqName != null) if (fqName != other.classFqName) return false
        if (arguments.isNotEmpty()) {
          if (other !is IrSimpleType) return false
          if (arguments.size != other.arguments.size) return false
          arguments.forEachIndexed { index, arg ->
            if (arg != null && !arg.equalTo(other.arguments[index].typeOrNull)) return false
          }
        }
      }
      else -> return false
    }
    return true
  }
}
