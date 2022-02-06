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

import com.meowool.meta.internal.SynthesizerFactory
import com.meowool.meta.synthesizers.ClassDescriptorScope
import com.meowool.meta.synthesizers.CompanionObjectSynthesizer
import com.meowool.meta.synthesizers.ConstructorsSynthesizer
import com.meowool.meta.synthesizers.FunctionsSynthesizer
import com.meowool.meta.synthesizers.PropertiesSynthesizer

/**
 * @author 凛 (RinOrz)
 */
interface Synthesizers {
  fun constructors(
    premise: ClassDescriptorScope.() -> Boolean = { true },
    synthesizing: ConstructorsSynthesizer.Scope.() -> Unit
  ): ConstructorsSynthesizer

  fun properties(synthesizing: PropertiesSynthesizer.Scope.() -> Unit): PropertiesSynthesizer

  fun functions(synthesizing: FunctionsSynthesizer.Scope.() -> Unit): FunctionsSynthesizer

  fun companionObject(synthesizing: CompanionObjectSynthesizer.NameBuilder.() -> Unit): CompanionObjectSynthesizer
}

val synthesizers: Synthesizers = SynthesizerFactory
