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
@file:Suppress("SpellCheckingInspection")

package com.meowool.meta.cloak.identifiers

import com.meowool.meta.Identifiers
import com.meowool.meta.cloak.CloakNames
import com.meowool.meta.cloak.identifiers.ReflectionTypeIdentifiers.ReflectionType
import com.meowool.sweekt.LazyInit
import com.meowool.sweekt.iteration.toArray
import org.jetbrains.kotlin.builtins.PrimitiveType

/**
 * API identifiers mapping for 'com/meowool/cloak/ExplicitTypeInstance.kt'.
 *
 * @author 凛 (RinOrz)
 */
object ExplicitTypeInstanceIdentifiers : Identifiers() {
  @LazyInit private val PrimitiveTypeIds = PrimitiveType.values().map { it.typeFqName.id }.toArray()

  @LazyInit val ExplicitTypeInstance = type(CloakNames.fqNameFor("ExplicitTypeInstance"), arguments = listOf(null))

  /** T?.typed(actualType: Type<T> = T::class.type): ExplicitTypeInstance<T> */
  @LazyInit val typed = function(
    extension = nullableGenericType(),
    name = "typed",
    parameters = parameter("actualType", type = ReflectionType, hasDefaultValue = true).toList(),
    returnType = ExplicitTypeInstance,
    parentFqName = CloakNames.rootFqName
  )

  /** val ?.primitiveTyped: ExplicitTypeInstance<?> */
  @LazyInit val primitiveTypeds = property(
    name = "primitiveTyped",
    type = ExplicitTypeInstance,
    parentFqName = CloakNames.rootFqName,
    possiblyExtensions = PrimitiveTypeIds
  )

  /** val ?.objectTyped: ExplicitTypeInstance<?> */
  @LazyInit val objectTypeds = property(
    name = "objectTyped",
    type = ExplicitTypeInstance,
    parentFqName = CloakNames.rootFqName,
    possiblyExtensions = PrimitiveTypeIds
  )
}
