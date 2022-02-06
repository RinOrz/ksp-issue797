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
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

@Diagnostic val INSTANCE_MOCK_CREATING_EXPR_ANALYZE_ERROR = diagnosticError(
  "Cannot manually create an instance of a mock class, " +
    "any mock class that inherit 'InstanceMock' will be automatically created by compiler."
)

@Diagnostic val INSTANCE_MOCK_INCOMPATIBLE_MODIFIER_ERROR = diagnosticError(
  "Class that inherit ''InstanceMock'' cannot be modified with ''{0}'' keyword.",
  Renderers.STRING
)

@Diagnostic val INSTANCE_MOCK_HAVE_CONSTRUCTOR_ERROR = diagnosticError(
  "Class that inherit 'InstanceMock' cannot have any constructors."
)

@Diagnostic val INSTANCE_MOCK_NO_OVERRIDE_TYPE_ERROR = diagnosticError(
  "Class that inherit 'InstanceMock' must override the 'type' property."
)

@Diagnostic val INSTANCE_MOCK_MUTABLE_TYPE_ERROR = diagnosticError(
  "The 'type' property of the mock class must be immutable (val)."
)

@Diagnostic val INSTANCE_MOCK_UNINITIALIZED_TYPE_ERROR = diagnosticError(
  "The 'type' property of the mock class must have initializer (override val type = ...)."
)
