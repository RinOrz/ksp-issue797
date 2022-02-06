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
package com.meowool.meta.internal

import com.meowool.meta.MetaExtension
import com.meowool.meta.Synthesizers
import com.meowool.meta.synthesizers.ClassDescriptorScope
import com.meowool.meta.synthesizers.CompanionObjectSynthesizer
import com.meowool.meta.synthesizers.ConstructorsSynthesizer
import com.meowool.meta.synthesizers.FunctionsSynthesizer
import com.meowool.meta.synthesizers.PropertiesSynthesizer
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * @author 凛 (RinOrz)
 */
internal object SynthesizerFactory : Synthesizers {
  override fun constructors(
    premise: ClassDescriptorScope.() -> Boolean,
    synthesizing: ConstructorsSynthesizer.Scope.() -> Unit
  ): ConstructorsSynthesizer =
    object : ConstructorsSynthesizer {
      override var context: MetaExtension.Context = MetaExtension.Context.Default

      override fun generate(
        parent: ClassDescriptor,
        binding: BindingContext,
        result: MutableCollection<ClassConstructorDescriptor>
      ) {
        if (premise(ClassDescriptorScope(parent))) {
          result += ScopeImpl(parent, binding).apply(synthesizing).descriptors
        }
      }

      inner class ScopeImpl(parent: ClassDescriptor, binding: BindingContext) :
        ConstructorsSynthesizer.Scope(parent, binding) {
        val descriptors = mutableListOf<ClassConstructorDescriptor>()

        override fun synthesize(descriptor: ClassConstructorDescriptor) {
          descriptors += descriptor
        }
      }
    }

  override fun properties(synthesizing: PropertiesSynthesizer.Scope.() -> Unit): PropertiesSynthesizer =
    object : PropertiesSynthesizer {
      override var context: MetaExtension.Context = MetaExtension.Context.Default
      private val scope = ScopeImpl().apply(synthesizing)

      override fun getNames(parent: ClassDescriptor): List<Name> = buildList {
        parent.filterData().forEach { add(it.name) }
      }

      override fun generate(
        name: Name,
        parent: ClassDescriptor,
        binding: BindingContext,
        fromSupertypes: MutableList<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
      ) {
        val data = parent.filterData().find { it.name == name } ?: return
        data.descriptor(
          PropertiesSynthesizer.DescriptorScope(name, parent, binding, fromSupertypes)
        )?.let(result::add)
      }

      private fun ClassDescriptor.filterData() = scope.data.filter {
        it.predicate(ClassDescriptorScope(this))
      }

      inner class ScopeImpl : PropertiesSynthesizer.Scope {
        inner class Data(
          val name: Name,
          val predicate: ClassDescriptorScope.() -> Boolean,
          val descriptor: PropertiesSynthesizer.DescriptorScope.() -> PropertyDescriptor?
        )

        val data = mutableListOf<Data>()

        override fun synthesize(
          name: String,
          predicate: ClassDescriptorScope.() -> Boolean,
          descriptor: PropertiesSynthesizer.DescriptorScope.() -> PropertyDescriptor?
        ) {
          data += Data(Name.identifier(name), predicate, descriptor)
        }
      }
    }

  override fun functions(synthesizing: FunctionsSynthesizer.Scope.() -> Unit): FunctionsSynthesizer =
    object : FunctionsSynthesizer {
      override var context: MetaExtension.Context = MetaExtension.Context.Default
      private val scope = ScopeImpl().apply(synthesizing)

      override fun getNames(parent: ClassDescriptor): List<Name> = buildList {
        parent.filterData().forEach { add(it.name) }
      }

      override fun generate(
        name: Name,
        parent: ClassDescriptor,
        binding: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
      ) {
        val data = parent.filterData().find { it.name == name } ?: return
        data.descriptor(
          FunctionsSynthesizer.DescriptorScope(name, parent, binding, fromSupertypes)
        )?.let(result::add)
      }

      private fun ClassDescriptor.filterData() = scope.data.filter {
        it.predicate(ClassDescriptorScope(this))
      }

      inner class ScopeImpl : FunctionsSynthesizer.Scope {
        inner class Data(
          val name: Name,
          val predicate: ClassDescriptorScope.() -> Boolean,
          val descriptor: FunctionsSynthesizer.DescriptorScope.() -> SimpleFunctionDescriptor?
        )

        val data = mutableListOf<Data>()

        override fun synthesize(
          name: String,
          predicate: ClassDescriptorScope.() -> Boolean,
          descriptor: FunctionsSynthesizer.DescriptorScope.() -> SimpleFunctionDescriptor?
        ) {
          data += Data(Name.identifier(name), predicate, descriptor)
        }
      }
    }

  override fun companionObject(synthesizing: CompanionObjectSynthesizer.NameBuilder.() -> Unit): CompanionObjectSynthesizer =
    object : CompanionObjectSynthesizer {
      override var context: MetaExtension.Context = MetaExtension.Context.Default

      override fun getName(parent: ClassDescriptor): Name? =
        CompanionObjectSynthesizer.NameBuilder(parent).apply(synthesizing).result
    }
}
