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
package com.meowool.meta.utils.descriptors

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

class ValueParameterDescriptorBuilder(
  private val name: Name,
  private val outType: KotlinType,
  private val annotations: Annotations,
  private val declaresDefaultValue: Boolean,
  private val isCrossinline: Boolean,
  private val isNoinline: Boolean,
  private val varargElementType: KotlinType?,
  private val source: SourceElement
) {
  internal fun build(containingDeclaration: CallableDescriptor, index: Int) = ValueParameterDescriptorImpl(
    containingDeclaration,
    null,
    index,
    this.annotations,
    this.name,
    this.outType,
    this.declaresDefaultValue,
    this.isCrossinline,
    this.isNoinline,
    this.varargElementType,
    this.source,
  )
}

fun ModuleDescriptor.buildValueParameterDescriptor(
  name: Name,
  outType: KotlinType,
  annotations: Annotations = Annotations.EMPTY,
  declaresDefaultValue: Boolean = false,
  isCrossinline: Boolean = false,
  isNoinline: Boolean = false,
  isVararg: Boolean = false,
  source: SourceElement? = null,
) = ValueParameterDescriptorBuilder(
  name,
  outType,
  annotations,
  declaresDefaultValue,
  isCrossinline,
  isNoinline,
  if (isVararg) getVarargParameterType(outType) else null,
  source ?: SourceElement.NO_SOURCE
)

private fun ModuleDescriptor.getVarargParameterType(elementType: KotlinType): KotlinType =
  builtIns.getPrimitiveArrayKotlinTypeByPrimitiveKotlinType(elementType)
    ?: builtIns.getArrayType(Variance.OUT_VARIANCE, elementType)
