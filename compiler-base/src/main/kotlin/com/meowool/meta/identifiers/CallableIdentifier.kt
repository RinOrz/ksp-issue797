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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

/**
 * @author 凛 (RinOrz)
 */
abstract class CallableIdentifier(
  private val name: String,
  private val possiblyParameters: Array<ParameterListIdentifier>,
  private val returnType: TypeIdentifier,
  private val parentFqName: FqName,
  private val possiblyExtensions: Array<TypeIdentifier>,
) : Identifier {
  override fun equalTo(other: Any?): Boolean {
    if (this === other) return true

    when (other) {
      is CallableDescriptor -> {
        val other = other.original
        if (name != other.name.asString()) return false
        if (!returnType.equalTo(other.returnType)) return false
        if (parentFqName.asString() != other.containingDeclaration.fqNameSafe.asString()) return false

        if (possiblyParameters.isEmpty() && other.valueParameters.isNotEmpty()) return false
        if (possiblyParameters.isNotEmpty()) {
          if (possiblyParameters.all { it.isNotEmpty() } && other.valueParameters.isEmpty()) return false
          if (!possiblyParameters.anyEqualTo(other.valueParameters)) return false
        }

        if (possiblyExtensions.isEmpty() && other.extensionReceiverParameter != null) return false
        if (possiblyExtensions.isNotEmpty()) {
          if (other.extensionReceiverParameter == null) return false
          if (!possiblyExtensions.anyEqualTo(other.extensionReceiverParameter?.type)) return false
        }
      }
      else -> return false
    }
    return true
  }
}
