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

import com.intellij.psi.PsiElement
import com.meowool.sweekt.castOrNull
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

/**
 * @author 凛 (RinOrz)
 */
val DeclarationDescriptor?.sourceDeclaration: KtDeclaration?
  get() = this?.let { DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtDeclaration }

/**
 * @author 凛 (RinOrz)
 */
val KtElement?.source: SourceElement? get() = this?.psiOrParent?.let(::KotlinSourceElement)

/**
 * @author 凛 (RinOrz)
 */
val PsiElement?.kotlinSource: SourceElement? get() = this.castOrNull<KtElement>()?.source
