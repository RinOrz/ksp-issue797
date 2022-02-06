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
@file:Suppress("PropertyName")

package com.meowool.meta

import com.meowool.meta.identifiers.FunctionIdentifier
import com.meowool.meta.identifiers.ParameterIdentifier
import com.meowool.meta.identifiers.ParameterListIdentifier
import com.meowool.meta.identifiers.PropertyIdentifier
import com.meowool.meta.identifiers.TypeIdentifier
import org.jetbrains.kotlin.builtins.StandardNames.FqNames.string
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import kotlin.reflect.KClass

/**
 * @author 凛 (RinOrz)
 */
open class Identifiers {
  val builtInTypes = BuiltInTypes()

  fun type(
    fqName: FqName,
    isNullable: Boolean = false,
    arguments: List<TypeIdentifier?> = emptyList(),
  ) = TypeIdentifier(fqName, isNullable, arguments)

  fun type(
    fqName: FqNameUnsafe,
    isNullable: Boolean = false,
    arguments: List<TypeIdentifier?> = emptyList(),
  ) = TypeIdentifier(fqName.toSafe(), isNullable, arguments)

  fun genericType(isNullable: Boolean = false) = TypeIdentifier(null, isNullable, emptyList())

  fun nullableType(fqName: FqName, arguments: List<TypeIdentifier?> = emptyList()) =
    TypeIdentifier(fqName, true, arguments)

  fun nullableType(fqName: FqNameUnsafe, arguments: List<TypeIdentifier?> = emptyList()) =
    TypeIdentifier(fqName.toSafe(), true, arguments)

  fun nullableGenericType() = TypeIdentifier(null, true, emptyList())

  inline fun <reified T> type(arguments: List<TypeIdentifier?> = T::class.java.typeParameters.map { null }) =
    type(FqName(T::class.java.name), arguments = arguments)

  inline fun <reified T> nullableType(arguments: List<TypeIdentifier?> = T::class.java.typeParameters.map { null }) =
    nullableType(FqName(T::class.java.name), arguments = arguments)

  fun property(
    name: String,
    type: TypeIdentifier,
    parentFqName: FqName,
    extension: TypeIdentifier? = null,
    possiblyExtensions: Array<TypeIdentifier> = emptyArray(),
  ) = PropertyIdentifier(
    name,
    type,
    parentFqName,
    if (extension != null) arrayOf(extension, *possiblyExtensions) else possiblyExtensions
  )

  fun function(
    name: String,
    returnType: TypeIdentifier,
    parentFqName: FqName,
    parameters: ParameterListIdentifier? = null,
    possiblyParameters: Array<ParameterListIdentifier> = emptyArray(),
    extension: TypeIdentifier? = null,
    possiblyExtensions: Array<TypeIdentifier> = emptyArray(),
  ) = FunctionIdentifier(
    name,
    if (parameters != null) arrayOf(parameters, *possiblyParameters) else possiblyParameters,
    returnType,
    parentFqName,
    if (extension != null) arrayOf(extension, *possiblyExtensions) else possiblyExtensions
  )

  fun parameters(vararg parameters: ParameterIdentifier) = ParameterListIdentifier(parameters.toList())

  fun parameter(
    name: String,
    type: TypeIdentifier,
    isVararg: Boolean = false,
    hasDefaultValue: Boolean = false,
  ) = ParameterIdentifier(name, type, isVararg, hasDefaultValue)

  fun ParameterIdentifier.toList() = parameters(this)

  val FqName.id get() = type(this)
  val FqNameUnsafe.id get() = type(this)

  inner class BuiltInTypes {
    val Class = type<Class<*>>()
    val KClass = type<KClass<*>>()
    val String = string.id
  }
}

val identifiers: Identifiers = Identifiers()
