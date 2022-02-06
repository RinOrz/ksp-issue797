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
package com.meowool.meta.cloak.processes

import com.meowool.meta.Meta
import com.meowool.meta.cloak.analyzes.InstanceMockActualTypeMap
import com.meowool.meta.cloak.identifiers.InstanceMockIdentifiers.ActualInstance
import com.meowool.meta.cloak.identifiers.InstanceMockIdentifiers.ActualType
import com.meowool.meta.cloak.identifiers.InstanceMockIdentifiers.InstanceMock
import com.meowool.meta.cloak.identifiers.InstanceMockIdentifiers.Mock
import com.meowool.meta.cloak.identifiers.ReflectionTypeIdentifiers.ReflectionTypeFqn
import com.meowool.meta.codegen.CodegenContext
import com.meowool.meta.codegen.DeclarationOrigin
import com.meowool.meta.codes
import com.meowool.meta.utils.ir.addBackingField
import com.meowool.meta.utils.ir.allSupertypes
import com.meowool.meta.utils.ir.findPropertyByName
import com.meowool.meta.utils.ir.irCall
import com.meowool.meta.utils.ir.irReturnExprBody
import com.meowool.meta.utils.ir.irSetProperty
import com.meowool.meta.utils.ir.type
import com.meowool.sweekt.cast
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull

/**
 * Used to generate code for synthetic properties.
 *
 * ```
 * class : InstanceMock<T> {
 *   val actual: T = null
 *
 *   companion object {
 *     val actualType = Type("...")
 *   }
 * }
 * ```
 *
 * @author 凛 (RinOrz)
 */
@Meta val InstanceMockSyntheticPropertiesProcessor = codes.property(
  premise = { isInstanceMockSyntheticProperty },
  processing = {
    property.apply {
      when (name.asString()) {
        // val actual: T = null
        ActualInstance -> addBackingField()

        // val actualType = Type("...")
        ActualType -> {
          val fqName = parents.first { it is IrClass && !it.isCompanion }.kotlinFqName
          val actualType = metaContext.cachedMap(InstanceMockActualTypeMap).get(fqName).cast<String>()
          val creator = referenceFunctions(ReflectionTypeFqn).single {
            it.valueParameters.singleOrNull()?.type?.isString() == true
          }
          // Type("...")
          addBackingField(initializer = buildIr { irExprBody(irCall(creator, irString(actualType))) })
        }
      }
      // Create default accessors that will eventually be inlined by the compiler
      createGetter().also { newGetter ->
        getter remapSymbolTo newGetter
        getter = newGetter
      }
    }
  }
)

/**
 * Used to add synthetic constructor.
 *
 * ```
 * class : InstanceMock<T> {
 *   constructor(p0: T) : this() {
 *     this.actual = actual
 *   }
 * }
 * ```
 *
 * @author 凛 (RinOrz)
 */
@Meta val InstanceMockSyntheticConstructorProcessor = codes.clasѕ(
  premise = { isInheritedInstanceMock && origin != DeclarationOrigin.Synthetic },
  processing = { syntheticConstructorTo(clasѕ) }
)

private fun CodegenContext.syntheticConstructorTo(irClass: IrClass): IrConstructor {
  val actualProperty = irClass.findPropertyByName(ActualInstance)!!
  return irClass.addConstructor(
    valueParameters = buildValueParameterList { add(actualProperty.type) },
    body = { constructor ->
      irBlockBody {
        // : this()
        +irDelegatingConstructorCall(irClass.constructors.single { it.valueParameters.isEmpty() })
        // this.actual = actual
        +irSetProperty(constructor, actualProperty, irGet(constructor.valueParameters.single()))
      }
    }
  )
}

/**
 * Used to add synthetic constructor.
 *
 * ```
 * class A : InstanceMock<T> {
 *   companion object {
 *     inline fun mock(actual: T): A = A(actual)
 *   }
 * }
 * ```
 *
 * @author 凛 (RinOrz)
 */
@Meta val InstanceMockSyntheticFunctionProcessor = codes.function(
  premise = {
    parentClassOrNull?.isCompanion == true &&
      parentClassOrNull?.parentClassOrNull?.isInheritedInstanceMock == true &&
      name.asString() == Mock && valueParameters.singleOrNull()?.name?.asString() == ActualInstance
  },
  processing = {
    val hostClass = function.parentAsClass.parentAsClass
    val syntheticConstructor = hostClass.findDeclaration {
      it.origin == DeclarationOrigin.Synthetic && it.valueParameters.size == 1
    } ?: syntheticConstructorTo(hostClass)

    function.body = function.buildIr {
      irReturnExprBody(
        irCall(syntheticConstructor.symbol, irGet(function.valueParameters.single()))
      )
    }
  }
)

private val IrProperty.isInstanceMockSyntheticProperty: Boolean
  get() = name.asString().let { it == ActualInstance || it == ActualType } && getter != null && getter?.body == null

internal val IrClass.isInheritedInstanceMock: Boolean
  get() = allSupertypes.any { InstanceMock equalTo it }
