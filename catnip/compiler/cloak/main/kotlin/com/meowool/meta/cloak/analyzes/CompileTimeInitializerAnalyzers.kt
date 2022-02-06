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
@file:Suppress("RemoveExplicitTypeArguments")

package com.meowool.meta.cloak.analyzes

import com.meowool.meta.analysis.AnalyzerContext
import com.meowool.meta.analyzers
import com.meowool.meta.cloak.diagnostics.NOT_A_COMPILE_TIME_INITIALIZER_ERROR
import com.meowool.meta.utils.getOrRecordNotNull
import com.meowool.meta.utils.isCompileTimeDeterminedProperty
import com.meowool.meta.utils.sourceDeclaration
import com.meowool.sweekt.LazyInit
import com.meowool.sweekt.castOrNull
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.util.slicedMap.Slices.createSimpleSlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

/**
 * Used to analyze the compile-time initializer expressions of referenced expressions.
 *
 * For example, the analysis result of the reference to `bar` in the following code is `100 + 10`:
 * ```
 * val foo = 100 + 10
 * val bar = foo
 * println(bar) // analyze here
 * ```
 *
 * @author 凛 (RinOrz)
 */
internal val CompileTimeInitializerAnalyzer = analyzers.call<KtExpression?> {
  var initializer = getCompileTimeInitializer(callee.sourceDeclaration, rawCall.callElement)

  while (initializer != null) {
    initializer = getCompileTimeInitializer(initializer.resolvedCallee?.sourceDeclaration, initializer)
  }

  initializer
}

private fun AnalyzerContext<*>.getCompileTimeInitializer(
  declaration: KtDeclaration?,
  callElement: KtElement
): KtExpression? {
  reportIfNot(declaration.isCompileTimeDeterminedProperty) {
    NOT_A_COMPILE_TIME_INITIALIZER_ERROR.with(declaration ?: callElement)
  }

  if (declaration == null) return null

  return trace.getOrRecordNotNull(CompileTimeInitializerExpr, declaration) {
    declaration.castOrNull<KtProperty>()?.initializer
  }
}

@LazyInit private val CompileTimeInitializerExpr: WritableSlice<KtDeclaration, KtExpression> = createSimpleSlice()
