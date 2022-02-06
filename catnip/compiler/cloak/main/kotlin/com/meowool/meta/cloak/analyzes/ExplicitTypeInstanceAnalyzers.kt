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
package com.meowool.meta.cloak.analyzes

import com.meowool.meta.analysis.CallAnalyzerContext
import com.meowool.meta.analyzers
import com.meowool.meta.cloak.diagnostics.EXPLICIT_TYPE_INSTANCE_ANALYZE_ERROR
import com.meowool.meta.cloak.identifiers.ExplicitTypeInstanceIdentifiers.ExplicitTypeInstance
import com.meowool.meta.cloak.identifiers.ExplicitTypeInstanceIdentifiers.objectTypeds
import com.meowool.meta.cloak.identifiers.ExplicitTypeInstanceIdentifiers.primitiveTypeds
import com.meowool.meta.cloak.identifiers.ExplicitTypeInstanceIdentifiers.typed
import com.meowool.meta.utils.argSingleOrNull
import com.meowool.meta.utils.getOrRecordNotNull
import com.meowool.meta.utils.toJvmDescriptor
import com.meowool.sweekt.LazyInit
import com.meowool.sweekt.onNull
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

/**
 * Used to analyze calls that explicitly specify the instance type.
 *
 * @author 凛 (RinOrz)
 */
val ExplicitTypeInstanceCallAnalyzer = analyzers.call(
  premise = { ExplicitTypeInstance equalTo resultingDescriptor.returnType },
  analyzing = CallAnalyzerContext<*>::analyzeExplicitTypeInstanceUsage
)

private fun CallAnalyzerContext<*>.analyzeExplicitTypeInstanceUsage(): String? =
  trace.getOrRecordNotNull(CallToExplicitType, rawCall) {
    when {
      typed equalTo callee -> when (val arg = call.valueArguments.argSingleOrNull()) {
        // case: `"Hello, world!".typed(CharSequence::class.type)`
        is ExpressionValueArgument -> arg.valueArgument?.getArgumentExpression()?.let {
          ReflectionTypeCallAnalyzer.analyze(it.resolvedCall, it)
        }
        // case: `"Hello, world!".typed<CharSequence>()`
        is DefaultValueArgument -> call.typeArguments.values.singleOrNull()?.toJvmDescriptor(trace)
        else -> null
      }

      // case: true.primitiveTyped
      primitiveTypeds equalTo callee -> callee.extensionReceiverParameter?.type?.toJvmDescriptor(trace)

      // case: true.objectTyped
      objectTypeds equalTo callee -> callee.extensionReceiverParameter?.type?.toJvmDescriptor(
        trace,
        forcedObject = true
      )

      // case:
      //   val int = 0.objectTyped
      //   call(int)
      else -> CompileTimeInitializerAnalyzer.analyze(call, analyzed)
        ?.resolvedCall?.let(::rebuild)?.analyzeExplicitTypeInstanceUsage()
    }
  }.onNull { EXPLICIT_TYPE_INSTANCE_ANALYZE_ERROR.report() }?.also(::log)

@LazyInit private val CallToExplicitType: WritableSlice<Call, String> = Slices.createSimpleSlice()
