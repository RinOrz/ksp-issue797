package com.meowool.meta.sweekt

import com.meowool.meta.testing.MetaTester
import com.meowool.meta.testing.KotlinLikeAfterDumper
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * @author 凛 (RinOrz)
 */
class LazyInitCodeProcessingTests : MetaTester(
  LazyInitPropertyProcessor,
  LazyInitCallProcessor,
  KotlinLikeAfterDumper
) {
  @Test fun `property is mutable after processing`() = compile(
    """
      import com.meowool.sweekt.LazyInit

      @LazyInit val a: Any = false
    """
  ).shouldContain("var a: Any")

  @Test fun `correct bridge was generated for property`() = compile(
    """
      import com.meowool.sweekt.LazyInit

      @LazyInit val a: Any = false

      class A {
        @LazyInit var b = "10-" + a
      }
    """
  ).shouldContains(
    """
      var isLazyInited${'$'}a
        field = false
    """.trimIndent(),
    // in class:
    """
      |  var isLazyInited${'$'}b
      |    field = false
    """.trimMargin(),
  )

  @Test fun `lazy value behavior is correct`() {
    fun ReflectionTest.verify() {
      // Values are lazy
      getField("boolean").shouldBeNull()
      getField("list").shouldBeNull()
      getField("block").shouldBeNull()

      val expectedBoolean = getProperty("boolean")
      val expectedList = getProperty("list")
      val expectedBlock = getProperty<() -> Boolean>("block")?.invoke()

      // Values are initialized
      expectedBoolean.also {
        it shouldBe false
        getField("boolean") shouldBe it
      }
      expectedList.also {
        it shouldBe listOf("a", "b")
        getField("list") shouldBe it
      }
      expectedBlock.also {
        it shouldBe true
        getField<() -> Boolean>("block")?.invoke() shouldBe it
      }
    }

    compile(
      """
        import com.meowool.sweekt.LazyInit
  
        @LazyInit val boolean: Any = false
        @LazyInit val list: List<String> = listOf("a", "b")
        @LazyInit val block: () -> Boolean = fun() = true

        class Foo {
          @LazyInit val boolean: Any = false
          @LazyInit val list: List<String> = listOf("a", "b")
          @LazyInit val block: () -> Boolean = fun() = true
        }
      """
    ) {
      verify()
      classOf("Foo").verify()
    }
  }

  @Test fun `replaced the correct reset expressions`() {
    fun ReflectionTest.verify(resetAllCase: Int) {
      // Step 1: Check initial bridges
      getField<Boolean>(LAZY_INIT_BRIDGE_PREFIX + "a")!!.shouldBeFalse()
      getField<Boolean>(LAZY_INIT_BRIDGE_PREFIX + "b")!!.shouldBeFalse()

      // Step 2: Get values for the first time
      getProperty<Int>("a") shouldBe 200
      getProperty<Int>("b") shouldBe 300

      // Step 3: Bridges has been initialized
      getField<Boolean>(LAZY_INIT_BRIDGE_PREFIX + "a")!!.shouldBeTrue()
      getField<Boolean>(LAZY_INIT_BRIDGE_PREFIX + "b")!!.shouldBeTrue()

      // Step 4: Change values
      setProperty("a", 10)
      setProperty("b", 10)

      // Step 5: Check values
      getProperty<Int>("a") shouldBe 10
      getProperty<Int>("b") shouldBe 10

      // Step 6: Reset values
      callFunction("resetAllCase$resetAllCase")

      // Step 7: Check bridges have been reset
      getField<Boolean>(LAZY_INIT_BRIDGE_PREFIX + "a")!!.shouldBeFalse()
      getField<Boolean>(LAZY_INIT_BRIDGE_PREFIX + "b")!!.shouldBeFalse()

      // Step 8: Check values have been reset
      getProperty<Int>("a") shouldBe 200
      getProperty<Int>("b") shouldBe 300

      // Step 9: Reset values again to ensure that the next verification is correct
      callFunction("resetAllCase$resetAllCase")
    }

    compile(
      """
        import com.meowool.sweekt.LazyInit
        import com.meowool.sweekt.resetLazyValue
        import com.meowool.sweekt.resetLazyValues
  
        private val staticExtra1 = "100".toInt()

        @LazyInit var a: Int = 100 + staticExtra1
        @LazyInit var b: Int = run { 200 + staticExtra1() }
        
        fun resetAllCase1() { 
          resetLazyValues(a, b)
        }
  
        fun resetAllCase2() { 
          a.resetLazyValue()
          b.resetLazyValue()
        }

        private fun staticExtra1() = 100
  
        class A {
          private val extra1 = "100".toInt()
          @LazyInit var a: Int = 100 + staticExtra1
          @LazyInit var b: Int = let { 200 + it.extra1() }
          
          fun resetAllCase1() { 
            resetLazyValues(a, b)
          }
  
          fun resetAllCase2() { 
            a.resetLazyValue()
            b.resetLazyValue()
          }

          private fun extra1() = 100
        }
      """
    ) {
      verify(1)
      verify(2)

      classOf("A").verify(1)
      classOf("A").verify(2)
    }
  }
}