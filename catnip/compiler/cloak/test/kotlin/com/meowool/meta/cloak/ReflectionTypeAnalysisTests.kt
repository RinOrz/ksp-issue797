@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.meowool.meta.cloak

import com.meowool.meta.cloak.analyzes.ReflectionTypeCallAnalyzer
import com.meowool.meta.cloak.diagnostics.NOT_A_COMPILE_TIME_INITIALIZER_ERROR
import com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_ANALYZE_ERROR
import com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_ILLEGAL_CLASS_SUFFIX_ERROR
import com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_IMPLICIT_RECEIVER_ERROR
import com.meowool.meta.testing.MetaTester
import org.junit.jupiter.api.Test

/**
 * @author 凛 (RinOrz)
 */
class ReflectionTypeAnalysisTests : MetaTester(ReflectionTypeCallAnalyzer) {
  @Test fun `no error reports`() = compile(
    """
      import com.meowool.cloak.Type
      import com.meowool.cloak.type

      val casePassJava = Type(Int::class.java)

      fun casePassJavaObject() = Type(Boolean::class.javaObjectType)
      fun casePassJavaPrimitive() = Type(Boolean::class.javaPrimitiveType!!)
      fun casePassLiteral() {
        println(Type(Cloneable::class))
      }

      val caseReceiveLiteral = Cloneable::class.type
      val caseReceiveJava = Cloneable::class.java.type
      val caseReceiveJavaObject = Cloneable::class.javaObjectType.type
      val caseReceiveJavaPrimitive = Cloneable::class.javaPrimitiveType!!.type

      fun caseTypeArgument() = Type<String>()
    """
  ) {
    shouldNotContainWarns()
    shouldBeOK()
  }

  @Test fun `error on unsupported creation expression`() = compile(
    """
      import com.meowool.cloak.type

      fun createType() = String::class.type
      
      val type = createType()
    """
  ).shouldContain(REFLECTION_TYPE_ANALYZE_ERROR.atLocation(5, 12))

  @Test fun `error on illegal suffix of creation expression`() = compile(
    """
      @file:Suppress("unused")

      import com.meowool.cloak.Type
      import com.meowool.cloak.type
      import kotlin.reflect.KClass

      val <T : Any> KClass<T>.javaObjectType: Class<T> get() = error("")
      val <T : Any> KClass<T>.javaXX: Class<T> get() = error("")
      
      fun case1() = buildList {
        add(Type(String::class.javaObjectType))
        add(String::class.javaXX.type)
      }
    """
  ).shouldContains(
    REFLECTION_TYPE_ILLEGAL_CLASS_SUFFIX_ERROR.atLocation(11, 26),
    REFLECTION_TYPE_ILLEGAL_CLASS_SUFFIX_ERROR.atLocation(12, 21),
  )

  @Test fun `error on extension receiver is ambiguous`() = compile(
    """
      import com.meowool.cloak.type

      fun case1() {
        "java.lang.String".apply { type }
      }

      fun case2() {
        "java.lang.String".also { it.type }
      }
    """
  ).shouldContains(
    REFLECTION_TYPE_IMPLICIT_RECEIVER_ERROR.atLocation(4, 30),
    NOT_A_COMPILE_TIME_INITIALIZER_ERROR("it").atLocation(8, 29),
  )

  @Test fun `accuracy of analysis results`() = compile(
    """
      import com.meowool.cloak.type
      import com.meowool.cloak.Type

      val case = arrayOf(
        Type(String::class),
        Type<List<*>>(ArrayList::class),
        Type(Int::class.javaObjectType),
        Type(ShortArray::class.javaPrimitiveType!!),
        Type(Unit::class.javaPrimitiveType!!),
        Type(Unit::class.javaObjectType),
        Type(Void::class.javaPrimitiveType!!),
        Type(Void::class.javaObjectType),

        Type<List<*>>(),
        Type<MutableList<*>>(),
        Type<Long>(),
        Type<String>(),

        Int::class.type,
        Boolean::class.java.type,
        Boolean::class.javaPrimitiveType!!.type,
        ShortArray::class.java.type,
        Array<Short>::class.java.type,
        Array<Short>::class.javaObjectType.type,
        Array<Short>::class.javaPrimitiveType!!.type,
      )
    """
  ).shouldContainsInOrder(
    baseRow = 5,
    objectDescriptor<String>(),
    objectDescriptor<ArrayList<*>>(),
    objectDescriptor<Int>(),
    objectDescriptor<ShortArray>(),
    objectDescriptor<Unit>(),
    objectDescriptor<Unit>(),
    primitiveDescriptor<Void>(),
    objectDescriptor<Void>(),
    null,
    objectDescriptor<java.util.List<*>>(),
    objectDescriptor<java.util.List<*>>(),
    primitiveDescriptor<Long>(),
    objectDescriptor<String>(),
    null,
    primitiveDescriptor<Int>(),
    primitiveDescriptor<Boolean>(),
    primitiveDescriptor<Boolean>(),
    objectDescriptor<ShortArray>(),
    objectDescriptor<Array<Short>>(),
    objectDescriptor<Array<Short>>(),
    objectDescriptor<Array<Short>>(),
  )
}