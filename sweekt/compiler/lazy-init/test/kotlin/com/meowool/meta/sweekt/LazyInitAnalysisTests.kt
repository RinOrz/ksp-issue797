package com.meowool.meta.sweekt

import com.meowool.meta.testing.MetaTester
import kotlin.test.Test

/**
 * @author 凛 (RinOrz)
 */
class LazyInitAnalysisTests : MetaTester(LazyInitPropertyAnalyzer, LazyInitCallAnalyzer) {
  @Test fun `no error reports`() = compile(
    """
      import com.meowool.sweekt.LazyInit

      @LazyInit
      val a: Int = 1

      val b: Int
        get() = 0

      const val c: Int = 0

      @JvmField
      var d: Int = 0
    """
  ) {
    shouldEmptyMessage()
    shouldBeOK()
  }

  @Test fun `error on @LazyInit property has a getter`() = compile(
    """
      import com.meowool.sweekt.LazyInit

      @LazyInit
      val a: Int
        get() {
          val x = 1
          return 1 + x
        }

      class A {
        @LazyInit
        val b: Boolean
          get() = true
      }
    """
  ).shouldContains(
    LAZY_INIT_GETTER_ERROR("get() { ... return 1 + x }").atLocation(5, 3),
    LAZY_INIT_GETTER_ERROR("get() = true").atLocation(13, 5),
  )

  @Test fun `error on @LazyInit property has no initializer`() = compile(
    """
      import com.meowool.sweekt.LazyInit

      @LazyInit
      val a: Int by lazy { 0 }

      class A {
        @LazyInit
        lateinit var b: String
      }
    """
  ).shouldContains(
    LAZY_INIT_INITIALIZER_ERROR("val", "a").atLocation(3, 1),
    LAZY_INIT_INITIALIZER_ERROR("var", "b").atLocation(7, 3),
  )

  @Test fun `error on @LazyInit property marked with @JvmField`() = compile(
    """
      import com.meowool.sweekt.LazyInit

      @LazyInit 
      @JvmField
      val a: Int = 0
    """
  ).shouldContain(LAZY_INIT_MARKED_JVM_FIELD_ERROR.atLocation(4, 1))

  @Test fun `error on @LazyInit property use 'const' modifier`() = compile(
    """
      import com.meowool.sweekt.LazyInit

      @LazyInit 
      const val a: Int = 0
    """
  ).shouldContain(LAZY_INIT_MODIFIED_CONST_ERROR.atLocation(4, 1))

  @Test fun `error on calling 'resetLazyValue' does not use the correct receiver`() = compile(
    """
      import com.meowool.sweekt.LazyInit
      import com.meowool.sweekt.resetLazyValue

      @LazyInit
      val a: Int = 0
      const val b = 0

      fun c() = 0

      fun case1() = a.run {
        resetLazyValue()
      }

      fun case2() {
        b.resetLazyValue()
        c().resetLazyValue()
      }
    """
  ).shouldContains(
    LAZY_INIT_RESET_VALUE_NO_RECEIVER_ERROR.atLocation(11, 3),
    LAZY_INIT_RESET_VALUE_ILLEGAL_RECEIVER_ERROR.atLocation(15, 3),
    LAZY_INIT_RESET_VALUE_ILLEGAL_RECEIVER_ERROR.atLocation(16, 3),
  )

  @Test fun `error on another incorrect 'resetLazyValues' with the same qualified name is declared`() = compile(
    """
      package com.meowool.sweekt

      fun resetLazyValues(lazyProperty: Any): Unit = compilerImplementation()
      fun resetLazyValues(): Unit = compilerImplementation()

      @LazyInit
      val a: Int = 0

      fun case() {
        resetLazyValues(a)
        resetLazyValues()
      }
    """
  ).shouldContains(
    LAZY_INIT_RESET_VALUES_WITHOUT_VARARG_ERROR.atLocation(10, 3),
    LAZY_INIT_RESET_VALUES_WITHOUT_VARARG_ERROR.atLocation(11, 3),
  )

  @Test fun `error on calling 'resetLazyValue' does not pass the correct arguments`() = compile(
    """
      import com.meowool.sweekt.LazyInit
      import com.meowool.sweekt.resetLazyValues

      @LazyInit
      val a: Int = 0
      const val b = 0

      fun c() = 0

      fun case() = a.run {
        resetLazyValues(this, b, c())
      }
    """
  ).shouldContains(
    LAZY_INIT_RESET_VALUES_ILLEGAL_ARG_ERROR.atLocation(11, 19),
    LAZY_INIT_RESET_VALUES_ILLEGAL_ARG_ERROR.atLocation(11, 25),
    LAZY_INIT_RESET_VALUES_ILLEGAL_ARG_ERROR.atLocation(11, 28),
  )
}