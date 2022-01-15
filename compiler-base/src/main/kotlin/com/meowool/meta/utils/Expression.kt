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
@file:Suppress("SpellCheckingInspection", "NOTHING_TO_INLINE")

package com.meowool.catnip.utils

import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.CheckerContext

/**
 * @author 凛 (RinOrz)
 */
inline fun KtExpression.deparenthesize(): KtExpression = KtPsiUtil.safeDeparenthesize(this)

/**
 * @author 凛 (RinOrz)
 */
fun KtExpression.toConstant(context: CheckerContext): Any? = toConstant(context.trace)

/**
 * @author 凛 (RinOrz)
 */
fun KtExpression.toConstant(trace: BindingTrace): Any? =
  BindingUtils.getCompileTimeValue(trace.bindingContext, deparenthesize())

/**
 * @author 凛 (RinOrz)
 */
fun KtExpression.toStringConstant(context: CheckerContext): String? = toConstant(context) as? String

/**
 * @author 凛 (RinOrz)
 */
fun KtExpression.toStringConstant(trace: BindingTrace): String? = toConstant(trace) as? String
