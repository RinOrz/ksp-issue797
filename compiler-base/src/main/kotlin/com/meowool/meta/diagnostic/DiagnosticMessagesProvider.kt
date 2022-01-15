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

package com.meowool.meta.diagnostic

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer

/**
 * Used to provide diagnostic data.
 *
 * @author 凛 (RinOrz)
 */
abstract class DiagnosticMessagesProvider {
  val renderers = DiagnosticFactoryToRendererMap(this.javaClass.name)

  fun initFactory(messages: DiagnosticMessagesExtension) =
    Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(this.javaClass, messages)

  fun renderInfo(message: String) =
    DiagnosticFactory0.create<PsiElement>(Severity.INFO).also { renderers.put(it, message) }

  fun renderWarn(message: String) =
    DiagnosticFactory0.create<PsiElement>(Severity.WARNING).also { renderers.put(it, message) }

  fun renderError(message: String) =
    DiagnosticFactory0.create<PsiElement>(Severity.ERROR).also { renderers.put(it, message) }

  fun <A> renderInfo(message: String, renderer: DiagnosticParameterRenderer<A>) =
    DiagnosticFactory1.create<PsiElement, A>(Severity.INFO).also { renderers.put(it, message, renderer) }

  fun <A> renderWarn(message: String, renderer: DiagnosticParameterRenderer<A>) =
    DiagnosticFactory1.create<PsiElement, A>(Severity.WARNING).also { renderers.put(it, message, renderer) }

  fun <A> renderError(message: String, renderer: DiagnosticParameterRenderer<A>) =
    DiagnosticFactory1.create<PsiElement, A>(Severity.ERROR).also { renderers.put(it, message, renderer) }

  fun <A, B> renderInfo(
    message: String,
    rendererA: DiagnosticParameterRenderer<A>,
    rendererB: DiagnosticParameterRenderer<B>
  ) = DiagnosticFactory2.create<PsiElement, A, B>(Severity.INFO).also {
    renderers.put(it, message, rendererA, rendererB)
  }

  fun <A, B> renderWarn(
    message: String,
    rendererA: DiagnosticParameterRenderer<A>,
    rendererB: DiagnosticParameterRenderer<B>
  ) = DiagnosticFactory2.create<PsiElement, A, B>(Severity.WARNING).also {
    renderers.put(it, message, rendererA, rendererB)
  }

  fun <A, B> renderError(
    message: String,
    rendererA: DiagnosticParameterRenderer<A>,
    rendererB: DiagnosticParameterRenderer<B>
  ) = DiagnosticFactory2.create<PsiElement, A, B>(Severity.ERROR).also {
    renderers.put(it, message, rendererA, rendererB)
  }
}
