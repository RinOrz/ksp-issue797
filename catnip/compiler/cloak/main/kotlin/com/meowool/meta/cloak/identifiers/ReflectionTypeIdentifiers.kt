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
package com.meowool.meta.cloak.identifiers

import com.meowool.meta.Identifiers
import com.meowool.meta.cloak.CloakNames
import com.meowool.sweekt.LazyInit
import org.jetbrains.kotlin.builtins.StandardNames.FqNames

/**
 * API identifiers mapping for 'com/meowool/cloak/Type.kt'.
 *
 * @author 凛 (RinOrz)
 */
object ReflectionTypeIdentifiers : Identifiers() {
  @LazyInit val ReflectionTypeFqn = CloakNames.fqNameFor("Type")

  @LazyInit val ReflectionType = type(ReflectionTypeFqn, arguments = listOf(null))

  /** Type(value: ?): Type */
  @LazyInit val ArgAPIs = function(
    name = "Type",
    possiblyParameters = arrayOf(
      parameter("value", type = builtInTypes.String).toList(),
      parameter("value", type = builtInTypes.Class).toList(),
      parameter("value", type = builtInTypes.KClass, hasDefaultValue = true).toList(),
    ),
    returnType = ReflectionType,
    parentFqName = CloakNames.rootFqName
  )

  /** ?.type: Type */
  @LazyInit val ReceiveAPIs = property(
    name = "type",
    type = ReflectionType,
    possiblyExtensions = arrayOf(
      FqNames.string.id,
      builtInTypes.Class,
      builtInTypes.KClass
    ),
    parentFqName = CloakNames.rootFqName
  )
}
