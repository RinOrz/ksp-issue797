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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.meowool.meta.analysis

import com.intellij.psi.PsiElement
import com.meowool.meta.MetaExtension
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.MissingSupertypesResolver
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.scopes.LexicalScope

/**
 * @author 凛 (RinOrz)
 */
class CallAnalyzerContext<R>(
  val call: ResolvedCall<*>,
  val resolution: ResolutionContext<*>?,
  val missingSupertypesResolver: MissingSupertypesResolver?,
  override val analyzed: PsiElement,
  override val trace: BindingTrace,
  override val moduleDescriptor: ModuleDescriptor,
  override val metaContext: MetaExtension.Context,
  override val deprecationResolver: DeprecationResolver,
  override val languageVersionSettings: LanguageVersionSettings,
  override val componentContainer: StorageComponentContainer,
) : AnalyzerContext<R>() {
  val callee: CallableDescriptor get() = call.resultingDescriptor

  val rawCall: Call get() = call.call

  val scope: LexicalScope?
    get() = resolution?.scope

  val dataFlowInfo: DataFlowInfo?
    get() = resolution?.dataFlowInfo

  val isAnnotationContext: Boolean?
    get() = resolution?.isAnnotationContext

  val dataFlowValueFactory: DataFlowValueFactory?
    get() = resolution?.dataFlowValueFactory

  fun rebuild(call: ResolvedCall<*>) = CallAnalyzerContext<R>(
    call,
    resolution,
    missingSupertypesResolver,
    analyzed,
    trace,
    moduleDescriptor,
    metaContext,
    deprecationResolver,
    languageVersionSettings,
    componentContainer
  )
}
