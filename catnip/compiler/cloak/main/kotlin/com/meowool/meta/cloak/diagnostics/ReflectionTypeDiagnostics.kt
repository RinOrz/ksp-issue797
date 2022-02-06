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

package com.meowool.meta.cloak.diagnostics

import com.meowool.meta.diagnostics.Diagnostic
import com.meowool.meta.diagnostics.diagnosticError

@Diagnostic val REFLECTION_TYPE_ANALYZE_ERROR = diagnosticError(
  "Unable to analyze this 'Type' instance. Currently, only expressions that can be determined at compile time " +
    "are supported to create the 'Type' instance, such as `Type(Int::class)`, `Type<Int>()`, `Int::class.type`."
)

@Diagnostic val REFLECTION_TYPE_IMPLICIT_RECEIVER_ERROR = diagnosticError(
  "Need explicitly specify the extension receiver, such as " +
    "`A::class.type` or `B::class.java.type` or `\"kotlin.time.Duration\".type`"
)

@Diagnostic val REFLECTION_TYPE_ILLEGAL_CLASS_SUFFIX_ERROR = diagnosticError(
  "Illegal suffix, only `kotlin.jvm.java` or `kotlin.jvm.javaObjectType` or `kotlin.jvm.javaPrimitiveType` " +
    "are allowed as suffix of class reference, such as `Any::class.java`"
)
