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
import org.jetbrains.kotlin.lexer.KtTokens.FINAL_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OPEN_KEYWORD
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVariableDeclaration

/**
 * @author 凛 (RinOrz)
 */
val KtDeclaration?.isCompileTimeDeterminedProperty: Boolean
  get() {
    if (this !is KtProperty) return false
    // The function members of interface are 'open' by default
    if (parent.parent.castOrNull<KtClass>()?.isInterface() == true) {
      if (hasModifier(FINAL_KEYWORD).not()) return false
    }
    return hasModifier(OPEN_KEYWORD).not() && castOrNull<KtVariableDeclaration>()?.isVar == false
  }
