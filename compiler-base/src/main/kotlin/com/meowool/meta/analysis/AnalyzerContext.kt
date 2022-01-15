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
package com.meowool.meta.analysis

import com.intellij.psi.PsiElement
import com.meowool.meta.internal.AnalysisReport
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @author 凛 (RinOrz)
 */
abstract class AnalyzerContext {
  abstract val analyzed: PsiElement
  abstract val trace: BindingTrace
  abstract val languageVersionSettings: LanguageVersionSettings
  abstract val moduleDescriptor: ModuleDescriptor
  abstract val deprecationResolver: DeprecationResolver

  internal val reports: MutableList<AnalysisReport> = mutableListOf()

  fun warn(message: String, highlighted: PsiElement = analyzed) {
    reports += AnalysisReport(highlighted, message, Severity.WARNING)
  }

  fun fail(message: String, highlighted: PsiElement = analyzed) {
    reports += AnalysisReport(highlighted, message, Severity.ERROR)
  }

  inline fun require(condition: Boolean, highlighted: PsiElement = analyzed, message: () -> String) {
    contract {
      returns() implies condition
      callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    }
    if (!condition) fail(message(), highlighted)
  }

  inline fun <T : Any> requireNotNull(value: T?, highlighted: PsiElement = analyzed, message: () -> String) {
    contract {
      returns() implies (value != null)
      callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    }
    if (value == null) fail(message(), highlighted)
  }

  inline fun <T : Any> requireNull(value: T?, highlighted: PsiElement = analyzed, message: () -> String) {
    contract {
      returns() implies (value == null)
      callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    }
    if (value != null) fail(message(), highlighted)
  }

  // //////////////////////////////////////////////////////////////////////////////
  // //                             Analysis Utils                             ////
  // //////////////////////////////////////////////////////////////////////////////

  inline val KtElement?.resolvedCall get(): ResolvedCall<out CallableDescriptor>? = getResolvedCall(trace.bindingContext)
  inline val KtElement?.resolvedCallee get(): CallableDescriptor? = resolvedCall?.resultingDescriptor

  inline fun DeclarationDescriptor.requireAnnotation(
    fqName: FqName,
    highlighted: PsiElement = analyzed,
    message: () -> String
  ) = require(annotations.hasAnnotation(fqName), highlighted, message)

  inline fun DeclarationDescriptor.requireNoAnnotation(
    fqName: FqName,
    highlighted: PsiElement = analyzed,
    message: () -> String
  ) = require(!annotations.hasAnnotation(fqName), highlighted, message)

  inline fun KtProperty.requireInitializer(highlighted: PsiElement = analyzed, message: () -> String) =
    require(initializer != null, highlighted, message)

  inline fun KtProperty.requireNoInitializer(
    highlighted: PsiElement = analyzed,
    message: (KtExpression) -> String
  ) {
    val initializer = initializer
    require(initializer == null, highlighted) { message(initializer!!) }
  }

  inline fun KtProperty.requireGetter(highlighted: PsiElement = analyzed, message: () -> String) =
    require(getter != null, highlighted, message)

  inline fun KtProperty.requireNoGetter(
    highlighted: PsiElement = analyzed,
    message: (KtPropertyAccessor) -> String
  ) {
    val getter = getter
    require(getter == null, highlighted) { message(getter!!) }
  }
}
