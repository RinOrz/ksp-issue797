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
package com.meowool.meta.utils

import com.meowool.sweekt.castOrNull
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtDeclarationWithInitializer
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression

/**
 * @author 凛 (RinOrz)
 */
val KtDeclaration?.singleBody: KtExpression?
  get() = castOrNull<KtDeclarationWithInitializer>()?.initializer ?: castOrNull<KtDeclarationWithBody>()?.run {
    bodyBlockExpression?.statements?.singleOrNull()?.castOrNull<KtReturnExpression>()?.returnedExpression
      ?: bodyExpression
  }

/**
 * @author 凛 (RinOrz)
 */
fun KtDeclaration?.filterBody(predicate: (KtExpression) -> Boolean): List<KtExpression> = buildList {
  this@filterBody.castOrNull<KtDeclarationWithInitializer>()?.initializer?.let(::add)
  this@filterBody.castOrNull<KtDeclarationWithBody>()?.apply {
    bodyBlockExpression?.statements?.let(::addAll)
    bodyExpression?.let(::add)
  }
}.filter(predicate)
