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
package com.meowool.meta

import com.meowool.meta.analysis.CallAnalysisPremise
import com.meowool.meta.analysis.CallAnalyzer
import com.meowool.meta.analysis.CallAnalyzerContext
import com.meowool.meta.analysis.DeclarationAnalyzer
import com.meowool.meta.analysis.EmptyPremise
import com.meowool.meta.analysis.EmptyPremiseWithParam
import com.meowool.meta.analysis.PropertyAnalysisPremise
import com.meowool.meta.analysis.PropertyAnalyzerContext
import com.meowool.meta.internal.AnalyzerFactory
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker

/**
 * @author 凛 (RinOrz)
 */
interface Analyzers {
  fun property(
    premise: PropertyAnalysisPremise = EmptyPremiseWithParam,
    analyzing: PropertyAnalyzerContext.() -> Unit
  ): DeclarationAnalyzer

  fun call(
    premise: CallAnalysisPremise = EmptyPremise,
    analyzing: CallAnalyzerContext.() -> Unit
  ): CallAnalyzer

  operator fun invoke(block: Analyzers.() -> Unit) = block()
}

val analyzers: Analyzers = AnalyzerFactory
