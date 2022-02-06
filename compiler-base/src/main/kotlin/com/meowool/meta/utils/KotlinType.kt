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

import com.meowool.sweekt.LazyInit
import com.meowool.sweekt.toJvmTypeDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName
import org.jetbrains.kotlin.resolve.DescriptorUtils.getFqNameSafe
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.checkers.CheckerContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

/**
 * @author 凛 (RinOrz)
 */
val KotlinType.fqName: String get() = getJetTypeFqName(printTypeArguments = false)

/**
 * @author 凛 (RinOrz)
 */
val KotlinType.fqNameWithArgs: String get() = getJetTypeFqName(printTypeArguments = true)

/**
 * @author 凛 (RinOrz)
 */
val KotlinType.fqNameSafe: FqName get() = getFqNameSafe(constructor.declarationDescriptor ?: error(constructor))

/**
 * @author 凛 (RinOrz)
 */
val KotlinType.fqNameUnsafe: FqNameUnsafe get() = getFqName(constructor.declarationDescriptor ?: error(constructor))

/**
 * @author 凛 (RinOrz)
 */
fun KtExpression?.getType(context: CheckerContext): KotlinType? = this?.getType(context.trace.bindingContext)

/**
 * @author 凛 (RinOrz)
 */
fun KtExpression?.getType(trace: BindingTrace): KotlinType? = this?.getType(trace.bindingContext)

/** `kotlin.String` -> `Ljava/lang/String;` */
@LazyInit private val TypeJvmDescriptor: WritableSlice<Pair<KotlinType, Boolean>, String> = Slices.createSimpleSlice()

/**
 * kotlin.Int -> I
 * kotlin.BooleanArray -> [Z
 * kotlin.Array<kotlin.Int> -> [java.lang.Integer
 */
@Suppress("SpellCheckingInspection")
fun KotlinType.toJvmDescriptor(trace: BindingTrace, forcedObject: Boolean = false): String? =
  trace.getOrRecord(TypeJvmDescriptor, this to forcedObject) {
    when {
      // Boolean | Byte | Char | Short | Int | Long | Float | Double
      KotlinBuiltIns.isPrimitiveType(this) -> when (fqNameUnsafe) {
        FqNames._boolean -> if (forcedObject) "Ljava/lang/Boolean;" else "Z"
        FqNames._byte -> if (forcedObject) "Ljava/lang/Byte;" else "B"
        FqNames._char -> if (forcedObject) "Ljava/lang/Character;" else "C"
        FqNames._short -> if (forcedObject) "Ljava/lang/Short;" else "S"
        FqNames._int -> if (forcedObject) "Ljava/lang/Integer;" else "I"
        FqNames._long -> if (forcedObject) "Ljava/lang/Long;" else "J"
        FqNames._float -> if (forcedObject) "Ljava/lang/Float;" else "F"
        FqNames._double -> if (forcedObject) "Ljava/lang/Double;" else "D"
        else -> null
      }
      // BooleanArray | ByteArray | CharArray | ShortArray | IntArray | LongArray | FloatArray | DoubleArray
      KotlinBuiltIns.isPrimitiveArray(this) -> when (fqNameSafe) {
        PrimitiveType.BOOLEAN.arrayTypeFqName -> "[Z"
        PrimitiveType.BYTE.arrayTypeFqName -> "[B"
        PrimitiveType.CHAR.arrayTypeFqName -> "[C"
        PrimitiveType.SHORT.arrayTypeFqName -> "[S"
        PrimitiveType.INT.arrayTypeFqName -> "[I"
        PrimitiveType.LONG.arrayTypeFqName -> "[J"
        PrimitiveType.FLOAT.arrayTypeFqName -> "[F"
        PrimitiveType.DOUBLE.arrayTypeFqName -> "[D"
        else -> return null
      }
      // Array<*> | Array<Object>
      KotlinBuiltIns.isArray(this) -> (arguments.singleOrNull() ?: return null).let {
        if (it.isStarProjection) {
          "[java/lang/Object;"
        } else {
          it.type.toJvmDescriptor(trace, forcedObject = true)?.run { "[$this" }
        }
      }
      /** Reference: [org.jetbrains.kotlin.ir.interpreter.state.Wrapper.getClass] */
      else -> when (fqNameUnsafe) {
        FqNames.any -> "Ljava/lang/Object;"
        FqNames.number -> "Ljava/lang/Number;"
        FqNames.charSequence -> "Ljava/lang/CharSequence;"
        FqNames.string -> "Ljava/lang/String;"
        FqNames._enum -> "Ljava/lang/Enum;"
        VoidFqName -> if (forcedObject) "Ljava/lang/Void;" else "V"
        else -> null
      } ?: when (fqNameSafe) {
        FqNames.comparable -> "Ljava/lang/Comparable;"
        FqNames.throwable -> "Ljava/lang/Throwable;"
        FqNames.iterable -> "Ljava/lang/Iterable;"
        FqNames.collection, FqNames.mutableCollection -> "Ljava/util/Collection;"
        FqNames.list, FqNames.mutableList -> "Ljava/util/List;"
        FqNames.set, FqNames.mutableSet -> "Ljava/util/Set;"
        FqNames.map, FqNames.mutableMap -> "Ljava/util/Map;"
        FqNames.listIterator, FqNames.mutableListIterator -> "Ljava/util/ListIterator;"
        FqNames.iterator, FqNames.mutableIterator -> "Ljava/util/Iterator;"
        FqNames.mapEntry, FqNames.mutableMapEntry -> "Ljava/util/Map\$Entry;"
        else -> null
      }
    } ?: fqName.toJvmTypeDescriptor()
  }

private val VoidFqName = FqNameUnsafe("java.lang.Void")
