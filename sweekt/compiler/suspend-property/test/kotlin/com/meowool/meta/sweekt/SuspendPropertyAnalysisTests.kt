package com.meowool.meta.sweekt

import com.meowool.meta.testing.MetaTester
import kotlin.test.Test

/**
 * @author 凛 (RinOrz)
 */
class SuspendPropertyAnalysisTests : MetaTester(
  SuspendPropertyAnalyzer,
  overriddenSuspendPropertyAnalyzer,
  SuspendPropertyCallAnalyzer
) {
  @Test fun `error on accessor of @Suspend property not initialize specific blocks`() = compile(
    """
      import com.meowool.sweekt.Suspend

      @Suspend
      var a: Int
        get() = 0
        set(value) {
          println(0)
        }
    """
  ).shouldContains(
    SUSPEND_GETTER_INITIALIZER_ERROR.atLocation(5, 11),
    SUSPEND_SETTER_INITIALIZER_ERROR.atLocation(6, 14),
  )

  @Test fun `error on @Suspend property marked with @JvmField`() = compile(
    """
      import com.meowool.sweekt.*

      @Suspend
      @JvmField
      val a: Int get() = suspendGetter { 0 }
    """
  ).shouldContain(SUSPEND_PROPERTY_MARKED_JVM_FIELD_ERROR.atLocation(4, 1))

  @Test fun `error on @Suspend property without accessors`() = compile(
    """
      import com.meowool.sweekt.Suspend

      @Suspend const val a: Int = 0

      @Suspend val b: Int = "0".toInt()
    """
  ).shouldContains(
    SUSPEND_PROPERTY_WITHOUT_ACCESSORS_ERROR.atLocation(3, 1),
    SUSPEND_PROPERTY_WITHOUT_ACCESSORS_ERROR.atLocation(5, 1)
  )

  @Test fun `error on normal property overrides the @Suspend property`() = compile(
    """
      import com.meowool.sweekt.Suspend

      interface A {
        @Suspend val a: Int
      }
      
      class AImpl : A {
        override val a: Int = 0
      }
    """
  ).shouldContain(OVERRIDDEN_SUSPEND_PROPERTY_NOT_MARKED_ERROR.atLocation(8, 3))
}