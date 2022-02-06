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
package com.meowool.meta.cloak.synthetics

import com.meowool.meta.Meta
import com.meowool.meta.cloak.analyzes.isInheritedInstanceMock
import com.meowool.meta.cloak.identifiers.InstanceMockIdentifiers.ActualInstance
import com.meowool.meta.cloak.identifiers.InstanceMockIdentifiers.ActualType
import com.meowool.meta.cloak.identifiers.InstanceMockIdentifiers.InstanceMock
import com.meowool.meta.cloak.identifiers.InstanceMockIdentifiers.Mock
import com.meowool.meta.synthesizers
import com.meowool.meta.utils.descriptors.buildValueParameterDescriptor
import com.meowool.meta.utils.descriptors.createFunctionDescriptor
import com.meowool.meta.utils.descriptors.createPropertyDescriptor
import com.meowool.meta.utils.descriptors.toVariableDescriptor
import com.meowool.meta.utils.findClassDeclarationAcrossModule
import com.meowool.meta.utils.kotlinSource
import com.meowool.meta.utils.sourceDeclaration
import com.meowool.sweekt.castOrNull
import com.meowool.sweekt.ifNull
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * Synthesizes companion object for the mock class that inherits 'InstanceMock'.
 *
 * @author 凛 (RinOrz)
 */
@Meta val InstanceMockCompanionObjectSynthesizer = synthesizers.companionObject {
  if (parent.isInheritedInstanceMock) synthesize()
}

/**
 * Synthesizes property for the companion object of the mock class that inherits 'InstanceMock'.
 *
 * @author 凛 (RinOrz)
 */
val InstanceMockPropertySynthesizer = synthesizers.properties {
  synthesize(
    name = ActualInstance,
    predicate = { parent.isInheritedInstanceMock },
    descriptor = {
      fun findInstanceMockInterfaceComment(text: String) = module.findInstanceMockInterface()?.children
        ?.filterIsInstance<KDoc>()?.first { text in it.text }?.allChildren
        ?.first { it.text == text }?.kotlinSource

      parent.createPropertyDescriptor(
        name,
        // Type<?>
        type = parent.findTypeOfActualTypeProperty(binding) ?: return@synthesize null,
        isFinal = false,
        source = findInstanceMockInterfaceComment("val $name: T")
      )
    }
  )

  synthesize(
    name = ActualType,
    predicate = { parent.isCompanionObjectInInheritedInstanceMock },
    descriptor = {
      val actualTypeProperty = parent.findActualTypeProperty(binding)
      val actualTypePropertyType = actualTypeProperty?.returnType ?: return@synthesize null

      parent.createPropertyDescriptor(
        name,
        type = actualTypePropertyType,
        source = actualTypeProperty.source
      )
    }
  )
}

/**
 * Synthesizes `inline fun mock(actual: T)`.
 *
 * @author 凛 (RinOrz)
 */
@Meta val InstanceMockStaticFunctionSynthesizer = synthesizers.functions {
  synthesize(
    name = Mock,
    predicate = { parent.isCompanionObjectInInheritedInstanceMock },
    descriptor = {
      val hostClass = parent.containingDeclaration as? ClassDescriptor ?: return@synthesize null
      parent.createFunctionDescriptor(
        name,
        module.buildValueParameterDescriptor(
          Name.identifier(ActualInstance),
          hostClass.findTypeOfActualTypeProperty(binding) ?: return@synthesize null
        ),
        returnType = hostClass.defaultType,
        source = hostClass.source
      ).apply { isInline = true }
    }
  )
}

private fun ModuleDescriptor.findInstanceMockInterface() = findClassDeclarationAcrossModule(InstanceMock.fqName)

private fun ClassDescriptor.findActualTypeProperty(binding: BindingContext) =
  sourceDeclaration.castOrNull<KtClassOrObject>().ifNull { module.findInstanceMockInterface() }
    ?.findPropertyByName(ActualType)
    ?.toVariableDescriptor(binding)

// Actual<Type>
private fun ClassDescriptor.findTypeOfActualTypeProperty(binding: BindingContext) =
  findActualTypeProperty(binding)?.returnType?.arguments?.singleOrNull()?.type

private val ClassDescriptor.isCompanionObjectInInheritedInstanceMock
  get() = isCompanionObject && containingDeclaration.isInheritedInstanceMock
