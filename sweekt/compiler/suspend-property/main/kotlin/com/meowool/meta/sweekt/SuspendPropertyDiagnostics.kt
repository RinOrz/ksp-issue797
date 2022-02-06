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

val SUSPEND_PROPERTY_WITHOUT_ACCESSORS_ERROR = diagnosticError(
  "Property marked with @Suspend without accessors is meaningless."
)

val SUSPEND_GETTER_INITIALIZER_ERROR = diagnosticError(
  "Getter of property marked with @Suspend can only initialize a specific block (e.g. `get() = suspendGetter { ... }`)."
)

val SUSPEND_SETTER_INITIALIZER_ERROR = diagnosticError(
  "Setter of property marked with @Suspend can only initialize a specific block (e.g. `set(value) = suspendSetter { ... }`)."
)

val SUSPEND_PROPERTY_MARKED_JVM_FIELD_ERROR = diagnosticError(
  "Property marked with @Suspend cannot be marked with @JvmField at the same time."
)

val OVERRIDDEN_SUSPEND_PROPERTY_NOT_MARKED_ERROR = diagnosticError(
  "To override a property marked with @Suspend, this property also needs to be marked with @Suspend."
)
