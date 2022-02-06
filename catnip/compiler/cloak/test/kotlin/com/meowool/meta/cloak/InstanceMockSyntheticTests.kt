@file:Suppress("SpellCheckingInspection")

package com.meowool.meta.cloak

import com.meowool.meta.cloak.analyzes.InstanceMockDeclarationAnalyzer
import com.meowool.meta.cloak.processes.InstanceMockSyntheticConstructorProcessor
import com.meowool.meta.cloak.processes.InstanceMockSyntheticFunctionProcessor
import com.meowool.meta.cloak.processes.InstanceMockSyntheticPropertiesProcessor
import com.meowool.meta.cloak.synthetics.InstanceMockCompanionObjectSynthesizer
import com.meowool.meta.cloak.synthetics.InstanceMockPropertySynthesizer
import com.meowool.meta.cloak.synthetics.InstanceMockStaticFunctionSynthesizer
import com.meowool.meta.testing.IrBeforeDumper
import com.meowool.meta.testing.KotlinLikeAfterDumper
import com.meowool.meta.testing.MetaTester
import org.junit.jupiter.api.Test

/**
 * @author 凛 (RinOrz)
 */
class InstanceMockSyntheticTests : MetaTester(
  InstanceMockDeclarationAnalyzer,
  InstanceMockCompanionObjectSynthesizer,
  InstanceMockStaticFunctionSynthesizer,
  InstanceMockPropertySynthesizer,
  InstanceMockSyntheticPropertiesProcessor,
  InstanceMockSyntheticConstructorProcessor,
  InstanceMockSyntheticFunctionProcessor,
  IrBeforeDumper,
  KotlinLikeAfterDumper
) {
  companion object {
    private const val FoundationTestCase = """
      import com.meowool.cloak.InstanceMock
      import com.meowool.cloak.Type
      import com.meowool.cloak.type

      class MyClass : InstanceMock<String> {
        override val actualType: Type<String> = String::class.type
      }
      
      @Suppress("ConvertSecondaryConstructorToPrimary")
      class MyClass2 {
        constructor(actual: String)
      }
    """
  }

  @Test fun `synthetic properties has been added`() = compile(FoundationTestCase) {
    shouldBeOK()
    "open val actual: String" shouldAppear 1
    """
      |    val actualType: Type<String>
      |      field = Type(value = "Ljava/lang/String;")
    """.trimMargin() shouldAppear 1
  }

  @Test fun `synthetic constructor has been added`() = compile(FoundationTestCase) {
    shouldBeOK()
    Regex("constructor\\(.+: String\\)") shouldAppear 2
    Regex("actual = \\w+\\s") shouldAppear 1
  }

  @Test fun `synthetic 'mock' function has been added`() = compile(FoundationTestCase) {
    shouldBeOK()
    "inline fun mock(actual: String): MyClass" shouldAppear 1
    Regex("MyClass\\(\\w+ = actual\\)") shouldAppear 1
  }

}