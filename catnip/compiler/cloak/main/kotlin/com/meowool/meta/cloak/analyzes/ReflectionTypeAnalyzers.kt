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
@file:Suppress("NAME_SHADOWING")

package com.meowool.meta.cloak.analyzes

import com.meowool.meta.analysis.AnalyzerContext
import com.meowool.meta.analysis.CallAnalyzerContext
import com.meowool.meta.analyzers
import com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_ANALYZE_ERROR
import com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_ILLEGAL_CLASS_SUFFIX_ERROR
import com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_IMPLICIT_RECEIVER_ERROR
import com.meowool.meta.cloak.identifiers.ReflectionTypeIdentifiers.ArgAPIs
import com.meowool.meta.cloak.identifiers.ReflectionTypeIdentifiers.ReceiveAPIs
import com.meowool.meta.cloak.identifiers.ReflectionTypeIdentifiers.ReflectionType
import com.meowool.meta.utils.deparenthesize
import com.meowool.meta.utils.extensionExpression
import com.meowool.meta.utils.getOrRecordNotNull
import com.meowool.meta.utils.toJvmDescriptor
import com.meowool.meta.utils.toStringConstant
import com.meowool.sweekt.LazyInit
import com.meowool.sweekt.castOrNull
import com.meowool.sweekt.onNull
import com.meowool.sweekt.toJvmTypeDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.util.slicedMap.Slices.createSimpleSlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

/**
 * Used to analyze calls of `Type(**)` or `**.type`.
 * TODO 检查如果创建的是已经内置的 type, IDE 则应该提示优化
 *
 * @author 凛 (RinOrz)
 */
val ReflectionTypeCallAnalyzer = analyzers.call(
  premise = { ReflectionType equalTo resultingDescriptor.returnType },
  analyzing = CallAnalyzerContext<*>::analyzeReflectionTypeUsage
)

private fun CallAnalyzerContext<*>.analyzeReflectionTypeUsage(): String? =
  trace.getOrRecordNotNull(CallToReflectionType, rawCall) {
    when {
      ArgAPIs equalTo callee -> when (val argument = call.valueArguments.values.singleOrNull()) {
        // case: Type("com.ham.Egg") | Type(Foo::class) | Type(Bar::class.java)
        is ExpressionValueArgument -> evaluateLiteralReflectionType(argument.valueArgument?.getArgumentExpression())
        // case: Type<Foo>()
        is DefaultValueArgument -> call.typeArguments.values.singleOrNull()?.toJvmDescriptor(trace)
        else -> null
      }

      // case: "com.ham.Egg".type | Foo::class.type | Bar::class.java.type
      ReceiveAPIs equalTo callee -> call.extensionExpression.let { extension ->
        reportIfNull(extension, termination = true) { REFLECTION_TYPE_IMPLICIT_RECEIVER_ERROR }
        evaluateLiteralReflectionType(extension)
      }

      // case:
      //   val string = String::class.type
      //   call(string)
      else -> CompileTimeInitializerAnalyzer.analyze(call, analyzed)
        ?.resolvedCall?.let(::rebuild)?.analyzeReflectionTypeUsage()
    }
  }.onNull { REFLECTION_TYPE_ANALYZE_ERROR.report() }?.also(::log)

/**
 * Evaluate the input expression used to create the reflection type.
 *
 * For example, the `Cloneable::class` expression will be evaluated as `java.lang.Cloneable`.
 *
 * @author 凛 (RinOrz)
 */
private fun AnalyzerContext<*>.evaluateLiteralReflectionType(expression: KtExpression?): String? {
  if (expression == null) return null

  return trace.getOrRecordNotNull(ClassLiteralToReflectionType, expression) {
    val expression = expression.deparenthesize()

    // Try converting directly to a string constant to process the case where the expression is a string
    expression.toStringConstant(trace)?.toJvmTypeDescriptor()?.also { return it }

    // Processing the case where the expression is a class reference literal
    when (expression) {
      // case 1: Foo::class
      is KtClassLiteralExpression -> expression.classLiteralType?.toJvmDescriptor(trace) ?: return null

      // case 2: Bar::class.java**
      is KtDotQualifiedExpression -> {
        // First check the left side of '.' whether is a class reference literal
        val classType = expression.receiverExpression.castOrNull<KtClassLiteralExpression>()
          ?.classLiteralType ?: return null
        // Then check the right side of '.' whether is a legal suffix
        val suffix = expression.selectorExpression.castOrNull<KtNameReferenceExpression>() ?: return null

        fun illegalSuffix() = REFLECTION_TYPE_ILLEGAL_CLASS_SUFFIX_ERROR.on(suffix).report(termination = true)

        if (suffix.getReferencedName() !in AllowedJavaClassSuffixes) illegalSuffix()
        // .java | .javaObjectType | .javaPrimitiveType
        when (suffix.resolvedCallee?.fqNameSafe) {
          // We need to be consistent with Kotlin's behavior and
          //   default `.java` returns primitive type instead of object type
          JavaExt, JavaPrimitiveTypeExt -> classType.toJvmDescriptor(trace)
          JavaObjectTypeExt -> classType.toJvmDescriptor(trace, forcedObject = true)
          else -> {
            illegalSuffix()
            null
          }
        }
      }

      // case 3: Bar::class.javaPrimitiveType!!
      is KtPostfixExpression -> when (expression.operationReference.text) {
        "!!" -> evaluateLiteralReflectionType(expression.baseExpression)
        else -> null
      }

      // case 4: clazz
      //   val clazz = String::class
      else -> expression.resolvedCall?.let {
        evaluateLiteralReflectionType(CompileTimeInitializerAnalyzer.analyze(it, expression))
      }
    }
  }
}

@LazyInit private val JavaExt = FqName("kotlin.jvm.java")
@LazyInit private val JavaObjectTypeExt = FqName("kotlin.jvm.javaObjectType")
@LazyInit private val JavaPrimitiveTypeExt = FqName("kotlin.jvm.javaPrimitiveType")
@LazyInit private val AllowedJavaClassSuffixes = arrayOf("java", "javaObjectType", "javaPrimitiveType")
@LazyInit private val CallToReflectionType: WritableSlice<Call, String> = createSimpleSlice()
@LazyInit private val ClassLiteralToReflectionType: WritableSlice<KtExpression, String> = createSimpleSlice()
