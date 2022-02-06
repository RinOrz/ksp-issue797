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
package com.meowool.meta.sweekt

import com.meowool.meta.Meta
import com.meowool.meta.analyzers
import com.meowool.meta.utils.extensionExpression
import com.meowool.meta.utils.hasAnnotation
import org.jetbrains.kotlin.lexer.KtTokens.CONST_KEYWORD
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

/**
 * Analyzes properties annotated with @LazyInit.
 *
 * @author 凛 (RinOrz)
 */
@Meta val LazyInitPropertyAnalyzer = analyzers.property(
  premise = { it.hasAnnotation(LAZY_INIT) },
  analyzing = {
    property.reportIfUninitialized { LAZY_INIT_INITIALIZER_ERROR.with(property.valOrVarKeyword, descriptor) }
    property.reportIfHasGetter { LAZY_INIT_GETTER_ERROR }
    property.reportIfModified(CONST_KEYWORD) { LAZY_INIT_MODIFIED_CONST_ERROR }
    descriptor.backingField?.reportIfMark(JVM_FIELD_ANNOTATION_FQ_NAME) { LAZY_INIT_MARKED_JVM_FIELD_ERROR }
  }
)

/**
 * Analyzes call targets annotated with @LazyInit.
 *
 * @author 凛 (RinOrz)
 */
@Meta val LazyInitCallAnalyzer = analyzers.call {
  when (callee.fqNameSafe) {
    // case: any.resetLazyValue()
    LAZY_INIT_RESET_VALUE -> {
      val extension = call.extensionExpression
      reportIfNull(extension) { LAZY_INIT_RESET_VALUE_NO_RECEIVER_ERROR }
      extension.resolvedCallee?.reportIfNotMark(LAZY_INIT) {
        LAZY_INIT_RESET_VALUE_ILLEGAL_RECEIVER_ERROR.on(extension)
      }
    }
    // case: resetLazyValues(...)
    LAZY_INIT_RESET_VALUES -> {
      val vararg = call.valueArgumentsByIndex?.getOrNull(0)
      reportIfNot(vararg is VarargValueArgument, termination = true) { LAZY_INIT_RESET_VALUES_WITHOUT_VARARG_ERROR }
      vararg.arguments.forEach {
        val expr = it.getArgumentExpression()
        expr.resolvedCallee?.reportIfNotMark(LAZY_INIT) {
          LAZY_INIT_RESET_VALUES_ILLEGAL_ARG_ERROR.on(expr ?: it.asElement())
        }
      }
    }
    else -> {}
  }
}
