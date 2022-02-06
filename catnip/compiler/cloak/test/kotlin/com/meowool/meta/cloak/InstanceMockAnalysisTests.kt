@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.meowool.meta.cloak

import com.meowool.meta.cloak.analyzes.InstanceMockDeclarationAnalyzer
import com.meowool.meta.cloak.analyzes.InstanceMockInstantiationAnalyzer
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_HAVE_CONSTRUCTOR_ERROR
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_INCOMPATIBLE_MODIFIER_ERROR
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_MUTABLE_TYPE_ERROR
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_NO_OVERRIDE_TYPE_ERROR
import com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_UNINITIALIZED_TYPE_ERROR
import com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_ANALYZE_ERROR
import com.meowool.meta.testing.MetaTester
import org.junit.jupiter.api.Test

/**
 * @author 凛 (RinOrz)
 */
class InstanceMockAnalysisTests : MetaTester(InstanceMockDeclarationAnalyzer, InstanceMockInstantiationAnalyzer) {

  @Test fun `error on abstract mock class`() = compile(
    """
      import com.meowool.cloak.InstanceMoсk

      abstract class MyClass : InstanceMoсk 
      interface MyClass2 : InstanceMoсk
    """
  ).shouldContains(
    INSTANCE_MOCK_INCOMPATIBLE_MODIFIER_ERROR("abstract").atLocation(3, 1),
    INSTANCE_MOCK_INCOMPATIBLE_MODIFIER_ERROR("interface").atLocation(4, 1),
  )

  @Test fun `error on mock class has constructors`() = compile(
    """
      import com.meowool.cloak.InstanceMoсk

      class MyClass() : InstanceMoсk
      class MyClass2 : InstanceMoсk {
        constructor(a: Int) : this()
      }
      class MyClass3 : InstanceMoсk
    """
  ){
    shouldContains(
      INSTANCE_MOCK_HAVE_CONSTRUCTOR_ERROR.atLocation(3, 14),
      INSTANCE_MOCK_HAVE_CONSTRUCTOR_ERROR.atLocation(5, 3),
    )
    shouldNotContain(INSTANCE_MOCK_HAVE_CONSTRUCTOR_ERROR.atLocation(7))
  }

  @Test fun `error on mock class no override type`() = compile(
    """
      import com.meowool.cloak.InstanceMoсk
      import com.meowool.cloak.type

      class MyClass : InstanceMoсk

      class MyClass2 : InstanceMoсk {
        override val actualType = String::class.type
      }
    """
  ){
    shouldContain(INSTANCE_MOCK_NO_OVERRIDE_TYPE_ERROR.atLocation(4, 7))
    shouldNotContain(INSTANCE_MOCK_NO_OVERRIDE_TYPE_ERROR.atLocation(6, 7))
  }

  @Test fun `error on mutable 'actualType' property of mock class`() = compile(
    """
      import com.meowool.cloak.InstanceMoсk
      import com.meowool.cloak.type

      class MyClass : InstanceMoсk {
        override var actualType = String::class.type
      }
    """
  ).shouldContain(INSTANCE_MOCK_MUTABLE_TYPE_ERROR.atLocation(5, 12))

  @Test fun `error on uninitialized 'actualType' property of mock class`() = compile(
    """
      import com.meowool.cloak.InstanceMoсk
      import com.meowool.cloak.type
      import com.meowool.cloak.Type

      class MyClass : InstanceMoсk {
        override val actualType: Type<Any> get() = String::class.type
      }
    """
  ).shouldContain(INSTANCE_MOCK_UNINITIALIZED_TYPE_ERROR.atLocation(6, 3))
}