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
package learn

import com.meowool.meta.codes
import com.meowool.meta.testing.MetaTester
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import kotlin.test.Test

/**
 * @author 凛 (RinOrz)
 */
class IrTreeLearningTests : MetaTester(irGetValueDumper) {
  @Test fun `learn the ir representation of 'access this'`() = compile(
    """
      class B {
        fun foo() = 0
      }
      class A {
        private val a get() = 0

        private val caseP1 = a + 100
        private val caseP2 = B().run {
          this@A.a + this.foo()
        }
        private val caseP3 = B().let {
          this.a + it.foo()
        }
        private val caseP4 = this.a + 100

        fun case1() = this
        fun case2() = B().run {
          this@A.a + this.foo()
        }
      }
    """
  ).shouldBeOK()

  companion object {
    val irGetValueDumper = codes.onStart { _, module ->
      module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

        override fun visitPropertyNew(declaration: IrProperty): IrStatement {
          println("PROPERTY START ==================================================")
          println(declaration.dumpKotlinLike())
          val result = super.visitPropertyNew(declaration)
          println("PROPERTY END ==================================================")
          return result
        }

        override fun visitFunctionNew(declaration: IrFunction): IrStatement {
          println("FUNCTION START ==================================================")
          println(declaration.dumpKotlinLike())
          val result = super.visitFunctionNew(declaration)
          println("FUNCTION END ==================================================")
          return result
        }

        override fun visitGetValue(expression: IrGetValue): IrExpression {
          println("  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
          println(expression.dump().prependIndent("  "))
          println("  //// owner: " + expression.symbol.owner.dump())
          println("  //// type: " + expression.type.dumpKotlinLike())
          println("  //// class: " + expression.javaClass.name)
          println("  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
          return super.visitGetValue(expression)
        }
      })
    }
  }
}
