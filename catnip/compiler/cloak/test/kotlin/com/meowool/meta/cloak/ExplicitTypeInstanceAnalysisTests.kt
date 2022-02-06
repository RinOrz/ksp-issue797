@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.meowool.meta.cloak

import com.meowool.meta.cloak.analyzes.ExplicitTypeInstanceCallAnalyzer
import com.meowool.meta.cloak.diagnostics.EXPLICIT_TYPE_INSTANCE_ANALYZE_ERROR
import com.meowool.meta.testing.MetaTester
import org.junit.jupiter.api.Test

/**
 * @author 凛 (RinOrz)
 */
class ExplicitTypeInstanceAnalysisTests : MetaTester(ExplicitTypeInstanceCallAnalyzer) {
  @Test fun `no error reports`() = compile(
    """
      import com.meowool.cloak.objectTyped
      import com.meowool.cloak.primitiveTyped
      import com.meowool.cloak.typed

      fun case1() = 0.typed()
      fun case2() = print(0.objectTyped)
      fun case3() = print(true.primitiveTyped)
    """
  ) {
    shouldNotContainWarns()
    shouldBeOK()
  }

  @Test fun `error on unsupported creation expression`() = compile(
    """
      import com.meowool.cloak.ExplicitTypeInstance
      import com.meowool.cloak.type
      import com.meowool.cloak.typed

      fun Any.numberTyped(): ExplicitTypeInstance<Any> = typed(Number::class.type)
      
      val number = 0.numberTyped()
    """
  ).shouldContain(EXPLICIT_TYPE_INSTANCE_ANALYZE_ERROR.atLocation(7, 16))

  @Test fun `accuracy of analysis results`() = compile(
    """
      import com.meowool.cloak.type
      import com.meowool.cloak.Type
      import com.meowool.cloak.primitiveTyped
      import com.meowool.cloak.objectTyped
      import com.meowool.cloak.typed

      val case = arrayOf(
        arrayListOf<String>().typed<List<String>>(),
        "".typed<CharSequence>(),
        Byte.MAX_VALUE.typed(),
        0.primitiveTyped,
        true.primitiveTyped,
        10L.objectTyped,
      )
    """
  ).shouldContainsInOrder(
    baseRow = 8,
    objectDescriptor<List<*>>(),
    objectDescriptor<CharSequence>(),
    primitiveDescriptor<Byte>(),
    primitiveDescriptor<Int>(),
    primitiveDescriptor<Boolean>(),
    objectDescriptor<Long>(),
  )
}