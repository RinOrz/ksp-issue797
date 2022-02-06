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

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl

/**
 * @author 凛 (RinOrz)
 */
fun ClassDescriptor.createConstructorDescriptor(
  vararg valueParameters: ValueParameterDescriptorBuilder,
  typeParameters: List<TypeParameterDescriptor> = emptyList(),
  isPrimary: Boolean = false,
  isSynthetic: Boolean = true,
  annotations: Annotations = Annotations.EMPTY,
  visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
  source: SourceElement? = null,
): ClassConstructorDescriptor {
  val descriptor = when {
    isSynthetic -> ClassConstructorDescriptorImpl.createSynthesized(
      this,
      annotations,
      isPrimary,
      source ?: SourceElement.NO_SOURCE,
    )
    else -> ClassConstructorDescriptorImpl.create(
      this,
      annotations,
      isPrimary,
      source ?: SourceElement.NO_SOURCE,
    )
  }
  descriptor.initialize(
    valueParameters.mapIndexed { index, builder -> builder.build(descriptor, index) }.toList(),
    visibility,
    typeParameters,
  )
  descriptor.returnType = this.defaultType
  return descriptor
}
