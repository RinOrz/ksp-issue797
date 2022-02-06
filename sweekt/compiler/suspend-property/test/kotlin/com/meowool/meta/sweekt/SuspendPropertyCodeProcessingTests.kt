package com.meowool.meta.sweekt

import com.meowool.meta.testing.MetaTester
import com.meowool.meta.testing.IrAfterDumper
import com.meowool.meta.testing.KotlinLikeAfterDumper
import org.intellij.lang.annotations.Language
import java.lang.System.lineSeparator
import kotlin.test.Test

/**
 * @author 凛 (RinOrz)
 */
class SuspendPropertyCodeProcessingTests : MetaTester(SuspendPropertyProcessor, IrAfterDumper, KotlinLikeAfterDumper) {
  companion object {
    /**
     * Source code for testing IR code processing.
     *
     * @author 凛 (RinOrz)
     */
    private const val IrTestCase = """
      import com.meowool.sweekt.*
      import kotlinx.coroutines.runBlocking

      interface A {
        @Suspend val id: Int
      }

      class AImpl : A {
        @Suspend override val id: Int = 1
      }
  
      @Suspend var name: String
        get() = suspendGetter { "0" }
        set(value) = suspendSetter {}

      fun case() = runBlocking {
        print(AImpl().id)
        print(name)
        name = "Hello"
      }
    """
  }

  @Test fun `@Suspend property no accessors after processing`() = compile(IrTestCase).shouldNotContains(
    "get(): String",
    "set(value: String)",
  )

  @Test fun `@Suspend property accessors have been moved to new suspend functions`() = compile(IrTestCase).shouldContains(
    "suspend fun <get-id>(): Int",
    "suspend fun <get-name>(): String",
    "suspend fun <set-name>(value: String)",
  )

  @Test fun `replaced suspended block with inlined block`() = compile(IrTestCase) {
    shouldNotContains(
      "suspendSetter(",
      "suspendGetter<String>(",
    )
    shouldContains(
      "suspendSetterInlined(",
      "suspendGetterInlined<String>(",
    )
  }

  @Test fun `all references have been remapped to new suspend functions`() = compile(IrTestCase) {
    shouldNotContains(
      lineSeparator() + "fun <get-name>()",
      lineSeparator() + "fun <set-name>(value: String)",
    )
    shouldContains(
      lineSeparator() + "suspend fun <get-name>()",
      lineSeparator() + "suspend fun <set-name>(value: String)",
    )
  }
}