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
package com.meowool.meta.internal

import com.intellij.psi.PsiElement
import com.meowool.meta.Analyzers
import com.meowool.meta.MetaExtension
import com.meowool.meta.analysis.AnalyzerContext
import com.meowool.meta.analysis.CallAnalysisPremise
import com.meowool.meta.analysis.CallAnalyzer
import com.meowool.meta.analysis.CallAnalyzerContext
import com.meowool.meta.analysis.DeclarationAnalyzer
import com.meowool.meta.analysis.PropertyAnalysisPremise
import com.meowool.meta.analysis.PropertyAnalyzerContext
import com.meowool.meta.internal.CommonDiagnosticMessagesProvider.FAIL
import com.meowool.meta.internal.CommonDiagnosticMessagesProvider.INFO
import com.meowool.meta.internal.CommonDiagnosticMessagesProvider.WARN
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

/**
 * @author 凛 (RinOrz)
 */
internal object AnalyzerFactory : Analyzers {
  override fun property(
    premise: PropertyAnalysisPremise,
    analyzing: PropertyAnalyzerContext.() -> Unit
  ) = object : DeclarationAnalyzer {
    override var context: MetaExtension.Context = MetaExtension.Context.Default

    override fun check(
      declaration: KtDeclaration,
      descriptor: DeclarationDescriptor,
      context: DeclarationCheckerContext
    ) {
      val property = declaration as? KtProperty ?: return
      if (descriptor !is PropertyDescriptor) return
      if (premise(property, descriptor)) PropertyAnalyzerContext(
        property,
        descriptor,
        context.trace,
        context.languageVersionSettings,
        context.moduleDescriptor,
        context.deprecationResolver
      ).report(analyzing)
    }
  }

  override fun call(
    premise: CallAnalysisPremise,
    analyzing: CallAnalyzerContext.() -> Unit
  ) = object : CallAnalyzer {
    override var context: MetaExtension.Context = MetaExtension.Context.Default

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
      if (premise(resolvedCall)) CallAnalyzerContext(
        resolvedCall,
        reportOn,
        context.trace,
        context.languageVersionSettings,
        context.moduleDescriptor,
        context.deprecationResolver
      ).report(analyzing)
    }
  }

  private inline fun <C : AnalyzerContext> C.report(block: C.() -> Unit) {
    block()
    reports.forEach {
      val diagnostic = when(it.severity) {
        Severity.INFO -> INFO
        Severity.ERROR -> FAIL
        Severity.WARNING -> WARN
      }
      trace.report(diagnostic.on(it.element, it.message))
    }
  }
}
