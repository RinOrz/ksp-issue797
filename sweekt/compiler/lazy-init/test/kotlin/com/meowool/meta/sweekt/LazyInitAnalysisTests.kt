package com.meowool.meta.sweekt

import com.meowool.meta.testing.MetaTester
import kotlin.test.Test

/**
 * @author 凛 (RinOrz)
 */
class LazyInitAnalysisTests : MetaTester(lazyInitPropertyAnalyzer, lazyInitCallAnalyzer) {
  @Test fun `error on @LazyInit property has a getter`() = compile(
    """
      import com.meowool.sweekt.LazyInit

      @LazyInit
      val a: Int
        get() = 0

      class A {
        @LazyInit
        val b: Boolean
          get() = true
      }
    """
  ).shouldContains(
    "Property marked with @LazyInit cannot declare getter (`get() = 0`) at the same time.".atLocation(5, 3),
    "Property marked with @LazyInit cannot declare getter (`get() = true`) at the same time.".atLocation(10, 5),
  )
}