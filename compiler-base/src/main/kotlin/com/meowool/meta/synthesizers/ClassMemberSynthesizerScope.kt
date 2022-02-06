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
package com.meowool.meta.synthesizers

import com.meowool.meta.utils.sourceDeclaration
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType

/**
 * @author 凛 (RinOrz)
 */
abstract class ClassMemberSynthesizerScope {
  abstract val parent: ClassDescriptor
  abstract val binding: BindingContext

  val parentDeclaration get() = parent.sourceDeclaration as? KtClassOrObject
  val module get() = parent.module

  // //////////////////////////////////////////////////////////////////////////////
  // //                             Analysis Utils                             ////
  // //////////////////////////////////////////////////////////////////////////////

  inline val KtElement?.resolvedCall: ResolvedCall<out CallableDescriptor>? get() = getResolvedCall(binding)
  inline val KtElement?.resolvedCallee: CallableDescriptor? get() = resolvedCall?.resultingDescriptor
  inline val KtExpression?.type: KotlinType? get() = this?.getType(binding)
}
