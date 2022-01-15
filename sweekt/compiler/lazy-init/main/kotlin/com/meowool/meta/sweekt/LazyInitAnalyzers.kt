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

import com.meowool.meta.analyzers
import com.meowool.meta.annotations.Meta
import com.meowool.meta.utils.extensionExpression
import com.meowool.meta.utils.hasAnnotation
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

/**
 * Analyzes properties annotated with @LazyInit.
 *
 * @author 凛 (RinOrz)
 */
@Meta val lazyInitPropertyAnalyzer = analyzers.property(
  premise = { it.hasAnnotation(LAZY_INIT) },
  analyzing = {
    property.requireNoGetter { getter ->
      "Property marked with @LazyInit cannot declare getter (`${getter.text}`) at the same time."
    }
    property.requireInitializer {
      "Property marked with @LazyInit must have initializer (e.g. `val ${declaration.name} = ...`) for the value of lazy initialization."
    }
    descriptor.requireNoAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME) {
      "Property marked with @LazyInit cannot be marked with @JvmField at the same time."
    }
  }
)

@Meta val lazyInitCallAnalyzer = analyzers.call {
  when (callee.fqNameSafe) {
    // case: any.resetLazyValue()
    LAZY_INIT_RESET_VALUE -> {
      val extension = call.extensionExpression
      requireNotNull(extension) {
        "To call 'resetLazyValue', the receiver must be a property marked with @LazyInit."
      }
      extension.resolvedCallee?.requireAnnotation(LAZY_INIT, highlighted = extension) {
        "Cannot call 'resetLazyValue' on a property that is not marked with @LazyInit."
      }
    }
    // case: resetLazyValues(...)
    LAZY_INIT_RESET_VALUES -> {
      val vararg = call.valueArgumentsByIndex?.get(0)
      require(vararg is VarargValueArgument) {
        "Cannot call '${SweektNames.Root}.resetLazyValues' without vararg argument."
      }
      vararg.arguments.forEach {
        val expr = it.getArgumentExpression()
        expr.resolvedCallee?.requireAnnotation(LAZY_INIT, highlighted = expr ?: it.asElement()) {
          "Requires a property marked with @LazyInit as a argument of the 'resetLazyValues' function."
        }
      }
    }
  }
}
