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
@file:Suppress("NAME_SHADOWING")

package com.meowool.meta.internal

import com.intellij.psi.PsiElement
import com.meowool.meta.Analyzers
import com.meowool.meta.MetaExtension
import com.meowool.meta.analysis.AnalyzerContext
import com.meowool.meta.analysis.CallAnalysisPremise
import com.meowool.meta.analysis.CallAnalyzer
import com.meowool.meta.analysis.CallAnalyzerContext
import com.meowool.meta.analysis.ClassLikeAnalysisPremise
import com.meowool.meta.analysis.ClassLikeAnalyzerContext
import com.meowool.meta.analysis.ClassOrObjectAnalysisPremise
import com.meowool.meta.analysis.DeclarationAnalyzer
import com.meowool.meta.analysis.DeclarationAnalyzerContext
import com.meowool.meta.analysis.ExpressionAnalysisPremise
import com.meowool.meta.analysis.ExpressionAnalyzer
import com.meowool.meta.analysis.ExpressionAnalyzerContext
import com.meowool.meta.analysis.PropertyAnalysisPremise
import com.meowool.meta.analysis.PropertyAnalyzerContext
import com.meowool.sweekt.cast
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

/**
 * @author 凛 (RinOrz)
 */
internal object AnalyzerFactory : Analyzers {
  override fun <R> classOrObject(
    premise: ClassOrObjectAnalysisPremise,
    analyzing: ClassLikeAnalyzerContext<KtClassOrObject, R>.() -> R
  ): DeclarationAnalyzer<R> = object : DeclarationAnalyzer<R> {
    override var context: MetaExtension.Context = MetaExtension.Context.Default
    override lateinit var componentContainer: StorageComponentContainer

    override fun check(
      declaration: KtDeclaration,
      descriptor: DeclarationDescriptor,
      context: DeclarationCheckerContext
    ) {
      val classLike = declaration as? KtClassOrObject ?: return
      if (descriptor !is ClassDescriptor) return
      if (premise(classLike, descriptor)) ClassLikeAnalyzerContext<KtClassOrObject, R>(
        classLike,
        descriptor,
        context.trace,
        context.languageVersionSettings,
        context.moduleDescriptor,
        this.context,
        context.deprecationResolver,
        componentContainer,
      ).runAnalyze { analyzing() }
    }

    override fun analyze(context: DeclarationAnalyzerContext<out KtDeclaration, out DeclarationDescriptor, R>): R? {
      val context = context.cast<ClassLikeAnalyzerContext<KtClassOrObject, R>>()
      return if (premise(context.declaration, context.descriptor)) context.runAnalyze { analyzing() } else null
    }
  }

  override fun <R> classLike(
    premise: ClassLikeAnalysisPremise,
    analyzing: ClassLikeAnalyzerContext<*, R>.() -> R
  ): DeclarationAnalyzer<R> = object : DeclarationAnalyzer<R> {
    override var context: MetaExtension.Context = MetaExtension.Context.Default
    override lateinit var componentContainer: StorageComponentContainer

    override fun check(
      declaration: KtDeclaration,
      descriptor: DeclarationDescriptor,
      context: DeclarationCheckerContext
    ) {
      val classLike = declaration as? KtClassLikeDeclaration ?: return
      if (descriptor !is ClassDescriptor) return
      if (premise(classLike, descriptor)) ClassLikeAnalyzerContext<KtClassLikeDeclaration, R>(
        classLike,
        descriptor,
        context.trace,
        context.languageVersionSettings,
        context.moduleDescriptor,
        this.context,
        context.deprecationResolver,
        componentContainer,
      ).runAnalyze { analyzing() }
    }

    override fun analyze(context: DeclarationAnalyzerContext<out KtDeclaration, out DeclarationDescriptor, R>): R? {
      require(context is ClassLikeAnalyzerContext<*, R>)
      return if (premise(context.declaration, context.descriptor)) context.runAnalyze { analyzing() } else null
    }
  }

  override fun <R> property(
    premise: PropertyAnalysisPremise,
    @BuilderInference analyzing: PropertyAnalyzerContext<R>.() -> R
  ): DeclarationAnalyzer<R> = object : DeclarationAnalyzer<R> {
    override var context: MetaExtension.Context = MetaExtension.Context.Default
    override lateinit var componentContainer: StorageComponentContainer

    override fun check(
      declaration: KtDeclaration,
      descriptor: DeclarationDescriptor,
      context: DeclarationCheckerContext
    ) {
      val property = declaration as? KtProperty ?: return
      if (descriptor !is PropertyDescriptor) return
      if (premise(property, descriptor)) PropertyAnalyzerContext<R>(
        property,
        descriptor,
        context.trace,
        context.languageVersionSettings,
        context.moduleDescriptor,
        this.context,
        context.deprecationResolver,
        componentContainer,
      ).runAnalyze { analyzing() }
    }

    override fun analyze(context: DeclarationAnalyzerContext<out KtDeclaration, out DeclarationDescriptor, R>): R? {
      require(context is PropertyAnalyzerContext<R>)
      return if (premise(context.declaration, context.descriptor)) context.runAnalyze { analyzing() } else null
    }
  }

  override fun <R> call(
    premise: CallAnalysisPremise,
    @BuilderInference analyzing: CallAnalyzerContext<R>.() -> R
  ): CallAnalyzer<R> = object : CallAnalyzer<R> {
    override var context: MetaExtension.Context = MetaExtension.Context.Default
    override lateinit var componentContainer: StorageComponentContainer

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
      if (premise(resolvedCall)) CallAnalyzerContext<R>(
        resolvedCall,
        context.resolutionContext,
        context.missingSupertypesResolver,
        reportOn,
        context.trace,
        context.moduleDescriptor,
        this.context,
        context.deprecationResolver,
        context.languageVersionSettings,
        componentContainer,
      ).runAnalyze { analyzing() }
    }

    override fun analyze(context: CallAnalyzerContext<R>): R? =
      if (premise(context.call)) context.runAnalyze { analyzing() } else null
  }

  override fun <R> expression(
    premise: ExpressionAnalysisPremise,
    @BuilderInference analyzing: ExpressionAnalyzerContext<R>.() -> R
  ): ExpressionAnalyzer<R> = object : ExpressionAnalyzer<R> {
    override var context: MetaExtension.Context = MetaExtension.Context.Default
    override lateinit var componentContainer: StorageComponentContainer

    override fun analyze(context: ExpressionAnalyzerContext<R>) =
      if (premise(context.expression)) context.runAnalyze { analyzing() } else null
  }

  private inline fun <R, C : AnalyzerContext<R>> C.runAnalyze(block: C.() -> R): R? = try {
    block()
  } catch (e: AnalysisTerminationException) {
    null
  }
}
