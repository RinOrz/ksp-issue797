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
@file:Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate")

package com.meowool.meta.analysis

import com.intellij.psi.PsiElement
import com.meowool.meta.MetaExtension
import com.meowool.meta.diagnostics.BaseDiagnosticBuilder
import com.meowool.meta.internal.AnalysisTerminationException
import com.meowool.meta.utils.getType
import com.meowool.meta.utils.psi
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.tryGetService
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticWithParameters1Renderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.STRING
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.allConstructors
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @author 凛 (RinOrz)
 */
abstract class AnalyzerContext<R> {
  abstract val analyzed: PsiElement
  abstract val trace: BindingTrace
  abstract val languageVersionSettings: LanguageVersionSettings
  abstract val moduleDescriptor: ModuleDescriptor
  abstract val metaContext: MetaExtension.Context
  abstract val deprecationResolver: DeprecationResolver
  abstract val componentContainer: StorageComponentContainer

  // TestOnly
  fun log(message: String) = log { message }

  // TestOnly
  inline fun log(message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (metaContext.loggable) trace.report(SIMPLE_INFO.on(analyzed, message()))
  }

  fun Diagnostic.report(termination: Boolean = false) {
    trace.report(this)
    if (termination) throw AnalysisTerminationException()
  }

  fun BaseDiagnosticBuilder.report(termination: Boolean = false) {
    build(analyzed).report(termination)
  }

  fun BaseDiagnosticBuilder.reportIf(condition: Boolean, termination: Boolean = false) {
    contract { returns() implies (!condition || !termination) }
    if (condition) report(termination)
  }

  inline fun reportIf(condition: Boolean, termination: Boolean = false, diagnostic: () -> BaseDiagnosticBuilder) {
    contract {
      returns() implies (!condition || !termination)
      callsInPlace(diagnostic, InvocationKind.AT_MOST_ONCE)
    }
    if (condition) diagnostic().report(termination)
  }

  inline fun reportIfNot(condition: Boolean, termination: Boolean = false, diagnostic: () -> BaseDiagnosticBuilder) {
    contract {
      returns() implies (condition && !termination)
      callsInPlace(diagnostic, InvocationKind.AT_MOST_ONCE)
    }
    if (condition.not()) diagnostic().report(termination)
  }

  inline fun <T : Any> reportIfNull(
    instance: T?,
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    contract {
      returns() implies (instance != null)
      callsInPlace(diagnostic, InvocationKind.AT_MOST_ONCE)
    }
    reportIf(instance == null, termination, diagnostic)
  }

  // //////////////////////////////////////////////////////////////////////////////
  // //                              Report Utils                              ////
  // //////////////////////////////////////////////////////////////////////////////

  inline fun Annotated.reportIfMark(
    fqName: FqName,
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    contract { callsInPlace(diagnostic, InvocationKind.AT_MOST_ONCE) }
    val annotation = annotations.findAnnotation(fqName)
    if (annotation != null) diagnostic().build(annotation.psi ?: analyzed).report(termination)
  }

  inline fun Annotated.reportIfNotMark(
    fqName: FqName,
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    if (annotations.hasAnnotation(fqName)) diagnostic().build(this as? PsiElement ?: analyzed).report(termination)
  }

  inline fun KtModifierListOwner.reportIfModified(
    modifier: KtModifierKeywordToken,
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    val modifierPsi = modifierList?.getModifier(modifier)
    if (modifierPsi != null) diagnostic().build(modifierPsi).report(termination)
  }

  inline fun KtModifierListOwner.reportIfNotModified(
    modifier: KtModifierKeywordToken,
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    if (hasModifier(modifier)) diagnostic().build(this).report(termination)
  }

  inline fun KtClassOrObject.reportIfHasConstructor(
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    val constructors = allConstructors
    if (constructors.isNotEmpty()) constructors.forEach { diagnostic().build(it).report(termination) }
  }

  inline fun KtClassOrObject.reportIfNoConstructor(
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    if (allConstructors.isEmpty()) diagnostic().build(this).report(termination)
  }

  inline fun KtProperty.reportIfInitialized(
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    val initializer = initializer
    if (initializer != null) diagnostic().build(initializer).report(termination)
  }

  inline fun KtProperty.reportIfUninitialized(
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    if (initializer == null) diagnostic().build(this).report(termination)
  }

  inline fun KtProperty.reportIfHasGetter(
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    val getter = getter
    if (getter != null) diagnostic().build(getter).report(termination)
  }

  inline fun KtProperty.reportIfNoGetter(
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    if (getter == null) diagnostic().build(this).report(termination)
  }

  inline fun KtProperty.reportIfHasSetter(
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    val setter = setter
    if (setter != null) diagnostic().build(setter).report(termination)
  }

  inline fun KtProperty.reportIfNoSetter(
    termination: Boolean = false,
    diagnostic: () -> BaseDiagnosticBuilder
  ) {
    if (setter == null) diagnostic().build(this).report(termination)
  }

  // //////////////////////////////////////////////////////////////////////////////
  // //                          Analysis Interactions                         ////
  // //////////////////////////////////////////////////////////////////////////////

  private val analyzeResultSlice: WritableSlice<AnalyzeKey, Any> = Slices.createSimpleSlice()

  private data class AnalyzeKey(val name: String, val value: Any)

  fun <R> DeclarationAnalyzer<R>.analyze(declaration: KtDeclaration?, descriptor: DeclarationDescriptor?): R? {
    if (declaration == null || descriptor == null) return null
    componentContainer = this@AnalyzerContext.componentContainer
    return analyze(
      when {
        declaration is KtProperty && descriptor is PropertyDescriptor -> PropertyAnalyzerContext(
          declaration,
          descriptor,
          trace,
          languageVersionSettings,
          moduleDescriptor,
          metaContext,
          deprecationResolver,
          componentContainer,
        )
        else -> DeclarationAnalyzerContext(
          declaration,
          descriptor,
          trace,
          languageVersionSettings,
          moduleDescriptor,
          metaContext,
          deprecationResolver,
          componentContainer,
        )
      }
    )
  }

  fun <R> CallAnalyzer<R>.analyze(call: ResolvedCall<*>?, analyzed: PsiElement): R? {
    if (call == null) return null
    componentContainer = this@AnalyzerContext.componentContainer
    return analyze(
      CallAnalyzerContext(
        call,
        componentContainer.getOrNull(),
        componentContainer.getOrNull(),
        analyzed,
        trace,
        moduleDescriptor,
        metaContext,
        deprecationResolver,
        languageVersionSettings,
        componentContainer,
      )
    )
  }

  fun <R> ExpressionAnalyzer<R>.analyze(expression: KtExpression?): R? {
    if (expression == null) return null
    componentContainer = this@AnalyzerContext.componentContainer
    return analyze(
      ExpressionAnalyzerContext(
        expression,
        trace,
        languageVersionSettings,
        moduleDescriptor,
        metaContext,
        deprecationResolver,
        componentContainer
      )
    )
  }

  private inline fun <reified T : Any> ComponentProvider.getOrNull(): T? {
    return tryGetService(T::class.java)
  }

  // //////////////////////////////////////////////////////////////////////////////
  // //                             Analysis Utils                             ////
  // //////////////////////////////////////////////////////////////////////////////

  inline val KtElement?.resolvedCall: ResolvedCall<out CallableDescriptor>? get() = getResolvedCall(trace.bindingContext)
  inline val KtElement?.resolvedCallee: CallableDescriptor? get() = resolvedCall?.resultingDescriptor
  inline val KtExpression?.type: KotlinType? get() = getType(trace)
  inline val KtClassLiteralExpression?.classLiteralType: KotlinType? get() = type?.arguments?.singleOrNull()?.type
}

@PublishedApi
internal val SIMPLE_INFO = DiagnosticFactory1.create<PsiElement, String>(Severity.INFO).apply {
  initializeName("SIMPLE_INFO")
  initDefaultRenderer(DiagnosticWithParameters1Renderer("{0}", STRING))
}
