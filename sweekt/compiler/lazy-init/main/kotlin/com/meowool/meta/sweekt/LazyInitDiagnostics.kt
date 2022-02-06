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
@file:Suppress("NOTHING_TO_INLINE")

package com.meowool.meta.sweekt

import com.meowool.meta.diagnostics.diagnosticError
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

val LAZY_INIT_GETTER_ERROR = diagnosticError("Property marked with @LazyInit cannot declare getter at the same time.")

val LAZY_INIT_INITIALIZER_ERROR = diagnosticError(
  "Property marked with @LazyInit must have initializer (e.g. `{0} {1} = ...`) " +
    "for the value of lazy initialization.",
  Renderers.ELEMENT_TEXT,
  Renderers.NAME,
)

val LAZY_INIT_MARKED_JVM_FIELD_ERROR = diagnosticError(
  "Property marked with @LazyInit cannot be marked with @JvmField at the same time."
)

val LAZY_INIT_MODIFIED_CONST_ERROR = diagnosticError(
  "Property marked with @LazyInit cannot be used 'const' modifier at the same time."
)
val LAZY_INIT_RESET_VALUE_NO_RECEIVER_ERROR = diagnosticError(
  "To call 'resetLazyValue', the receiver must be call a property marked with @LazyInit."
)

val LAZY_INIT_RESET_VALUE_ILLEGAL_RECEIVER_ERROR = diagnosticError(
  "Cannot call 'resetLazyValue' on a instance that is not a property marked with @LazyInit."
)

val LAZY_INIT_RESET_VALUES_WITHOUT_VARARG_ERROR = diagnosticError(
  "Cannot call '${SweektNames.Root}.resetLazyValues' without 'vararg' argument."
)

val LAZY_INIT_RESET_VALUES_ILLEGAL_ARG_ERROR = diagnosticError(
  "Requires a property marked with @LazyInit as a argument of the 'resetLazyValues' function."
)
