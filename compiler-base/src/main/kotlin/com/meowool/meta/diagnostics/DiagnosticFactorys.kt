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

package com.meowool.meta.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory4
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer

fun diagnosticInfo(message: String): DiagnosticFactory0Provider = DiagnosticFactory0Provider(
  DiagnosticFactory0.create(Severity.INFO),
  message,
)

fun <A : Any> diagnosticInfo(
  message: String,
  renderer: DiagnosticParameterRenderer<A>
): DiagnosticFactory1Provider<A> = DiagnosticFactory1Provider(
  DiagnosticFactory1.create(Severity.INFO),
  message,
  renderer
)

fun <A : Any, B : Any> diagnosticInfo(
  message: String,
  rendererA: DiagnosticParameterRenderer<A>,
  rendererB: DiagnosticParameterRenderer<B>
): DiagnosticFactory2Provider<A, B> = DiagnosticFactory2Provider(
  DiagnosticFactory2.create(Severity.INFO),
  message,
  rendererA,
  rendererB
)

fun <A : Any, B : Any, C : Any> diagnosticInfo(
  message: String,
  rendererA: DiagnosticParameterRenderer<A>,
  rendererB: DiagnosticParameterRenderer<B>,
  rendererC: DiagnosticParameterRenderer<C>
): DiagnosticFactory3Provider<A, B, C> = DiagnosticFactory3Provider(
  DiagnosticFactory3.create(Severity.INFO),
  message,
  rendererA,
  rendererB,
  rendererC
)

fun <A : Any, B : Any, C : Any, D : Any> diagnosticInfo(
  message: String,
  rendererA: DiagnosticParameterRenderer<A>,
  rendererB: DiagnosticParameterRenderer<B>,
  rendererC: DiagnosticParameterRenderer<C>,
  rendererD: DiagnosticParameterRenderer<D>
): DiagnosticFactory4Provider<A, B, C, D> = DiagnosticFactory4Provider(
  DiagnosticFactory4.create(Severity.INFO),
  message,
  rendererA,
  rendererB,
  rendererC,
  rendererD
)

fun diagnosticError(message: String): DiagnosticFactory0Provider = DiagnosticFactory0Provider(
  DiagnosticFactory0.create(Severity.ERROR),
  message,
)

fun <A : Any> diagnosticError(
  message: String,
  renderer: DiagnosticParameterRenderer<A>
): DiagnosticFactory1Provider<A> = DiagnosticFactory1Provider(
  DiagnosticFactory1.create(Severity.ERROR),
  message,
  renderer
)

fun <A : Any, B : Any> diagnosticError(
  message: String,
  rendererA: DiagnosticParameterRenderer<A>,
  rendererB: DiagnosticParameterRenderer<B>
): DiagnosticFactory2Provider<A, B> = DiagnosticFactory2Provider(
  DiagnosticFactory2.create(Severity.ERROR),
  message,
  rendererA,
  rendererB
)

fun <A : Any, B : Any, C : Any> diagnosticError(
  message: String,
  rendererA: DiagnosticParameterRenderer<A>,
  rendererB: DiagnosticParameterRenderer<B>,
  rendererC: DiagnosticParameterRenderer<C>
): DiagnosticFactory3Provider<A, B, C> = DiagnosticFactory3Provider(
  DiagnosticFactory3.create(Severity.ERROR),
  message,
  rendererA,
  rendererB,
  rendererC
)

fun <A : Any, B : Any, C : Any, D : Any> diagnosticError(
  message: String,
  rendererA: DiagnosticParameterRenderer<A>,
  rendererB: DiagnosticParameterRenderer<B>,
  rendererC: DiagnosticParameterRenderer<C>,
  rendererD: DiagnosticParameterRenderer<D>
): DiagnosticFactory4Provider<A, B, C, D> = DiagnosticFactory4Provider(
  DiagnosticFactory4.create(Severity.ERROR),
  message,
  rendererA,
  rendererB,
  rendererC,
  rendererD
)

fun diagnosticWarn(message: String): DiagnosticFactory0Provider = DiagnosticFactory0Provider(
  DiagnosticFactory0.create(Severity.WARNING),
  message,
)

fun <A : Any> diagnosticWarn(
  message: String,
  renderer: DiagnosticParameterRenderer<A>
): DiagnosticFactory1Provider<A> = DiagnosticFactory1Provider(
  DiagnosticFactory1.create(Severity.WARNING),
  message,
  renderer
)

fun <A : Any, B : Any> diagnosticWarn(
  message: String,
  rendererA: DiagnosticParameterRenderer<A>,
  rendererB: DiagnosticParameterRenderer<B>
): DiagnosticFactory2Provider<A, B> = DiagnosticFactory2Provider(
  DiagnosticFactory2.create(Severity.WARNING),
  message,
  rendererA,
  rendererB
)

fun <A : Any, B : Any, C : Any> diagnosticWarn(
  message: String,
  rendererA: DiagnosticParameterRenderer<A>,
  rendererB: DiagnosticParameterRenderer<B>,
  rendererC: DiagnosticParameterRenderer<C>
): DiagnosticFactory3Provider<A, B, C> = DiagnosticFactory3Provider(
  DiagnosticFactory3.create(Severity.WARNING),
  message,
  rendererA,
  rendererB,
  rendererC
)

fun <A : Any, B : Any, C : Any, D : Any> diagnosticWarn(
  message: String,
  rendererA: DiagnosticParameterRenderer<A>,
  rendererB: DiagnosticParameterRenderer<B>,
  rendererC: DiagnosticParameterRenderer<C>,
  rendererD: DiagnosticParameterRenderer<D>
): DiagnosticFactory4Provider<A, B, C, D> = DiagnosticFactory4Provider(
  DiagnosticFactory4.create(Severity.WARNING),
  message,
  rendererA,
  rendererB,
  rendererC,
  rendererD
)

// ------------------------------ Providers ------------------------------

abstract class BaseDiagnosticFactoryProvider {

  abstract val factory: DiagnosticFactory<*>
  abstract val message: String

  abstract infix fun renderTo(map: DiagnosticFactoryToRendererMap)
}

class DiagnosticFactory0Provider(
  override val factory: DiagnosticFactory0<PsiElement>,
  override val message: String,
) : BaseDiagnosticFactoryProvider(), BaseDiagnosticBuilder {
  override fun renderTo(map: DiagnosticFactoryToRendererMap) {
    map.put(factory, message)
  }

  fun on(highlighted: PsiElement?) = DiagnosticBuilder().on(highlighted)

  override fun build(defaultHighlighted: PsiElement) = factory.on(defaultHighlighted)

  inner class DiagnosticBuilder : BaseDiagnosticBuilder {
    private var highlighted: PsiElement? = null

    fun on(highlighted: PsiElement?): DiagnosticBuilder = apply {
      this.highlighted = highlighted
    }

    override fun build(defaultHighlighted: PsiElement): Diagnostic = factory.on(highlighted ?: defaultHighlighted)
  }
}

class DiagnosticFactory1Provider<A : Any>(
  override val factory: DiagnosticFactory1<PsiElement, A>,
  override val message: String,
  private val renderer: DiagnosticParameterRenderer<A>,
) : BaseDiagnosticFactoryProvider() {
  override fun renderTo(map: DiagnosticFactoryToRendererMap) {
    map.put(factory, message, renderer)
  }

  fun on(highlighted: PsiElement?): DiagnosticBuilder = DiagnosticBuilder().on(highlighted)

  fun with(argument: A): DiagnosticBuilder = DiagnosticBuilder().with(argument)

  inner class DiagnosticBuilder : BaseDiagnosticBuilder {
    private var highlighted: PsiElement? = null
    private lateinit var argument: A

    fun on(highlighted: PsiElement?): DiagnosticBuilder = apply {
      this.highlighted = highlighted
    }

    fun with(argument: A): DiagnosticBuilder = apply {
      this.argument = argument
    }

    override fun build(defaultHighlighted: PsiElement): Diagnostic = factory.on(
      highlighted ?: defaultHighlighted,
      argument
    )
  }
}

class DiagnosticFactory2Provider<A : Any, B : Any>(
  override val factory: DiagnosticFactory2<PsiElement, A, B>,
  override val message: String,
  private val rendererA: DiagnosticParameterRenderer<A>,
  private val rendererB: DiagnosticParameterRenderer<B>,
) : BaseDiagnosticFactoryProvider() {
  override fun renderTo(map: DiagnosticFactoryToRendererMap) {
    map.put(factory, message, rendererA, rendererB)
  }

  fun on(highlighted: PsiElement?): DiagnosticBuilder = DiagnosticBuilder().on(highlighted)

  fun with(argumentA: A, argumentB: B): DiagnosticBuilder = DiagnosticBuilder().with(argumentA, argumentB)

  inner class DiagnosticBuilder : BaseDiagnosticBuilder {
    private var highlighted: PsiElement? = null
    private lateinit var argumentA: A
    private lateinit var argumentB: B

    fun on(highlighted: PsiElement?): DiagnosticBuilder = apply {
      this.highlighted = highlighted
    }

    fun with(argumentA: A, argumentB: B): DiagnosticBuilder = apply {
      this.argumentA = argumentA
      this.argumentB = argumentB
    }

    override fun build(defaultHighlighted: PsiElement): Diagnostic = factory.on(
      highlighted ?: defaultHighlighted,
      argumentA,
      argumentB
    )
  }
}

class DiagnosticFactory3Provider<A : Any, B : Any, C : Any>(
  override val factory: DiagnosticFactory3<PsiElement, A, B, C>,
  override val message: String,
  private val rendererA: DiagnosticParameterRenderer<A>,
  private val rendererB: DiagnosticParameterRenderer<B>,
  private val rendererC: DiagnosticParameterRenderer<C>,
) : BaseDiagnosticFactoryProvider() {
  override fun renderTo(map: DiagnosticFactoryToRendererMap) {
    map.put(factory, message, rendererA, rendererB, rendererC)
  }

  fun on(highlighted: PsiElement?): DiagnosticBuilder = DiagnosticBuilder().on(highlighted)

  fun with(argumentA: A, argumentB: B, argumentC: C): DiagnosticBuilder = DiagnosticBuilder().with(argumentA, argumentB, argumentC)

  inner class DiagnosticBuilder : BaseDiagnosticBuilder {
    private var highlighted: PsiElement? = null
    private lateinit var argumentA: A
    private lateinit var argumentB: B
    private lateinit var argumentC: C

    fun on(highlighted: PsiElement?): DiagnosticBuilder = apply {
      this.highlighted = highlighted
    }

    fun with(argumentA: A, argumentB: B, argumentC: C): DiagnosticBuilder = apply {
      this.argumentA = argumentA
      this.argumentB = argumentB
      this.argumentC = argumentC
    }

    override fun build(defaultHighlighted: PsiElement): Diagnostic = factory.on(
      highlighted ?: defaultHighlighted,
      argumentA,
      argumentB,
      argumentC
    )
  }
}

class DiagnosticFactory4Provider<A : Any, B : Any, C : Any, D : Any>(
  override val factory: DiagnosticFactory4<PsiElement, A, B, C, D>,
  override val message: String,
  private val rendererA: DiagnosticParameterRenderer<A>,
  private val rendererB: DiagnosticParameterRenderer<B>,
  private val rendererC: DiagnosticParameterRenderer<C>,
  private val rendererD: DiagnosticParameterRenderer<D>,
) : BaseDiagnosticFactoryProvider() {
  override fun renderTo(map: DiagnosticFactoryToRendererMap) {
    map.put(factory, message, rendererA, rendererB, rendererC, rendererD)
  }

  fun on(highlighted: PsiElement?): DiagnosticBuilder = DiagnosticBuilder().on(highlighted)

  fun with(argumentA: A, argumentB: B, argumentC: C, argumentD: D): DiagnosticBuilder = DiagnosticBuilder().with(argumentA, argumentB, argumentC, argumentD)

  inner class DiagnosticBuilder : BaseDiagnosticBuilder {
    private var highlighted: PsiElement? = null
    private lateinit var argumentA: A
    private lateinit var argumentB: B
    private lateinit var argumentC: C
    private lateinit var argumentD: D

    fun on(highlighted: PsiElement?): DiagnosticBuilder = apply {
      this.highlighted = highlighted
    }

    fun with(argumentA: A, argumentB: B, argumentC: C, argumentD: D): DiagnosticBuilder = apply {
      this.argumentA = argumentA
      this.argumentB = argumentB
      this.argumentC = argumentC
      this.argumentD = argumentD
    }

    override fun build(defaultHighlighted: PsiElement): Diagnostic = factory.on(
      highlighted ?: defaultHighlighted,
      argumentA,
      argumentB,
      argumentC,
      argumentD
    )
  }
}
