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

import com.meowool.sweekt.castOrNull
import com.meowool.sweekt.runOrNull
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

/**
 * @author 凛 (RinOrz)
 */
fun DeclarationDescriptor.createFunctionDescriptor(
  name: Name,
  vararg valueParameters: ValueParameterDescriptorBuilder,
  returnType: KotlinType,
  visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
  isFinal: Boolean = true,
  annotations: Annotations = Annotations.EMPTY,
  typeParameters: List<TypeParameterDescriptor> = emptyList(),
  extensionReceiverParameter: ReceiverParameterDescriptor? = null,
  dispatchReceiverParameter: ReceiverParameterDescriptor? = runOrNull { castOrNull<ClassDescriptor>()?.thisAsReceiverParameter },
  modality: Modality = if (isFinal) Modality.FINAL else Modality.OPEN,
  kind: CallableMemberDescriptor.Kind = CallableMemberDescriptor.Kind.SYNTHESIZED,
  source: SourceElement? = null,
) = SimpleFunctionDescriptorImpl.create(
  this,
  annotations,
  name,
  kind,
  source ?: SourceElement.NO_SOURCE,
).also {
  it.initialize(
    extensionReceiverParameter,
    dispatchReceiverParameter,
    typeParameters,
    valueParameters.mapIndexed { index, builder -> builder.build(it, index) }.toList(),
    returnType,
    modality,
    visibility
  )
}
