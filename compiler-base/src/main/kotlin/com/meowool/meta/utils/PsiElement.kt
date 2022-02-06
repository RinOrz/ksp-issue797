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
package com.meowool.meta.utils

import com.intellij.psi.PsiElement

/**
 * Example 1:
 * ```
 * val value = 0
 * ```
 * Result: `val value = 0`
 *
 * Example 2:
 * ```
 * val value = run {
 *   println(1 + 1)
 * }
 * ```
 * Result: `val value = run { println(1 + 1) }`
 *
 * Example 3:
 * ```
 * val value = run {
 *   val x = 0
 *   println(1 + x)
 * }
 * ```
 * Result: `val value = run { ... println(1 + x) }`
 *
 * @author 凛 (RinOrz)
 */
val PsiElement.singleLineText: String
  get() {
    val lines = text.lines()
    return when {
      lines.size == 1 -> lines.first()
      lines.size == 3 -> lines.joinToString(" ")
      lines.size > 3 -> lines.first() + " ... " + lines[lines.lastIndex - 1].trimIndent() + " " + lines.last().trimIndent()
      else -> text
    }
  }
