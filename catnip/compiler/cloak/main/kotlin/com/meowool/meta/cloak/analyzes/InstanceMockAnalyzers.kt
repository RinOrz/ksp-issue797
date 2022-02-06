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
package com.meowool.meta.cloak.analyzes

import com.meowool.meta.Meta
import com.meowool.meta.analyzers
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_CREATING_EXPR_ANALYZE_ERROR
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_HAVE_CONSTRUCTOR_ERROR
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_INCOMPATIBLE_MODIFIER_ERROR
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_MUTABLE_TYPE_ERROR
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_NO_OVERRIDE_TYPE_ERROR
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_UNINITIALIZED_TYPE_ERROR
import com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_ANALYZE_ERROR
import com.meowool.meta.cloak.identifiers.InstanceMockIdentifiers.ActualType
import com.meowool.meta.cloak.identifiers.InstanceMockIdentifiers.InstanceMock
import com.meowool.meta.utils.getOrRecordNotNull
import com.meowool.sweekt.LazyInit
import com.meowool.sweekt.castOrNull
import com.meowool.sweekt.onNotNull
import com.meowool.sweekt.onNull
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isSuperOrDelegatingConstructorCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.util.slicedMap.Slices.createSimpleSlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

/**
 * Used to analyze classes that inherit `InstanceMock`.
 *
 * - Mock class cannot be abstract.
 * - Mock class cannot declare any constructors.
 * - Sub mock class must override the type property.
 *
 * @author 凛 (RinOrz)
 */
@Meta val InstanceMockDeclarationAnalyzer = analyzers.classOrObject(
  premise = { it.isInheritedInstanceMock },
  analyzing = {
    trace.getOrRecordNotNull(InstanceMockActualTypeCache, classLike) {
      classLike.reportIfModified(KtTokens.ABSTRACT_KEYWORD) { INSTANCE_MOCK_INCOMPATIBLE_MODIFIER_ERROR.with("abstract") }
      classLike.reportIfHasConstructor { INSTANCE_MOCK_HAVE_CONSTRUCTOR_ERROR }
      classLike.castOrNull<KtClass>()?.apply {
        reportIf(isInterface()) {
          INSTANCE_MOCK_INCOMPATIBLE_MODIFIER_ERROR.with("interface").on(getClassOrInterfaceKeyword())
        }
      }
      val type = classLike.findPropertyByName(ActualType).castOrNull<KtProperty>()
      reportIfNull(type, termination = true) { INSTANCE_MOCK_NO_OVERRIDE_TYPE_ERROR.on(classLike.nameIdentifier) }
      reportIf(type.isVar) { INSTANCE_MOCK_MUTABLE_TYPE_ERROR.on(type.valOrVarKeyword) }

      val initializer = type.reportIfUninitialized(termination = true) { INSTANCE_MOCK_UNINITIALIZED_TYPE_ERROR }.let {
        type.initializer!!
      }
      // Analyzing the actual type of mock class
      ReflectionTypeCallAnalyzer.analyze(initializer.resolvedCall, initializer).onNull {
        REFLECTION_TYPE_ANALYZE_ERROR.report()
      }.onNotNull {
        metaContext.cachedMap(InstanceMockActualTypeMap).put(
          key = classLike.fqName ?: descriptor.fqNameSafe.asString(),
          value = it
        )
      }
    }
  }
)

/**
 * Used to analyze calls that create instance that inherit `InstanceMock`.
 *
 * [Reference](https://github.com/JetBrains/kotlin/blob/1.6.20/compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/checkers/AbstractClassInstantiationChecker.kt)
 *
 * @author 凛 (RinOrz)
 */
@Meta val InstanceMockInstantiationAnalyzer = analyzers.call(
  premise = { resultingDescriptor is ConstructorDescriptor && !isSuperOrDelegatingConstructorCall(call) },
  analyzing = {
    if (callee.castOrNull<ConstructorDescriptor>()?.constructedClass.isInheritedInstanceMock) {
      INSTANCE_MOCK_CREATING_EXPR_ANALYZE_ERROR.report()
    }
  }
)

@LazyInit private val InstanceMockActualTypeCache: WritableSlice<KtClassLikeDeclaration, String> = createSimpleSlice()

internal const val InstanceMockActualTypeMap = "InstanceMocks.actualType"

internal val DeclarationDescriptor?.isInheritedInstanceMock: Boolean
  get() = this is ClassDescriptor && getAllSuperClassifiers().any { InstanceMock equalTo it.defaultType }
