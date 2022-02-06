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
package com.meowool.meta.testing

import com.meowool.meta.codes
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.util.IrMessageLogger.Severity
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

val IrPluginContext.irMessageLogger get() = createDiagnosticReporter("com.meowool.meta.testing.MetaTester")

val KotlinLikeBeforeDumper = codes.onStart { context, module ->
  context.irMessageLogger.report(Severity.INFO, module.dumpKotlinLike(), null)
}

val KotlinLikeAfterDumper = codes.onEnd { context, module ->
  context.irMessageLogger.report(Severity.INFO, module.dumpKotlinLike(), null)
}

val IrBeforeDumper = codes.onStart { context, module ->
  context.irMessageLogger.report(Severity.INFO, module.dump(), null)
}

val IrAfterDumper = codes.onEnd { context, module ->
  context.irMessageLogger.report(Severity.INFO, module.dump(), null)
}
