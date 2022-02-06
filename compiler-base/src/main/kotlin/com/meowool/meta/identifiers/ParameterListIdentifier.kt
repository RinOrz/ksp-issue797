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

/**
 * @author 凛 (RinOrz)
 */
data class ParameterListIdentifier internal constructor(val params: List<ParameterIdentifier>) :
  Identifier, List<ParameterIdentifier> by params {

  override fun equalTo(other: Any?): Boolean {
    if (this === other) return true
    if (this.hashCode() == other.hashCode()) return true
    if (other is List<*>) {
      if (this.isEmpty() && other.isEmpty()) return true
      if (this.size != other.size) return false

      this.forEachIndexed { index, parameterIdentifier ->
        if (!parameterIdentifier.equalTo(other[index])) return false
      }
    }
    return true
  }
}
