@file:Suppress("SpellCheckingInspection", "MemberVisibilityCanBePrivate", "unused", "JAVA_CLASS_ON_COMPANION")

package com.meowool.meta.testing

import com.meowool.meta.MetaExtension
import com.meowool.meta.annotations.Meta
import com.meowool.meta.codegen.CodeProcessor
import com.meowool.meta.internal.transformRemapping
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.reflection.shouldHaveFunction
import io.kotest.matchers.reflection.shouldHaveMemberProperty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.platform.TargetPlatform
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties

/**
 * Dedicated tester for testing compilation of compiler meta plugins.
 *
 * @param metaExtensions Extension instances annotated with [Meta].
 * @author 凛 (RinOrz)
 */
open class MetaTester(vararg val metaExtensions: MetaExtension) {

  /**
   * Represents a message with a location.
   *
   * @author 凛 (RinOrz)
   */
  class LocationMessage(val row: Int, val column: Int, val message: String) {
    override fun toString(): String = "($row, $column): $message"
  }

  fun String.atLocation(row: Int, column: Int): LocationMessage = LocationMessage(row, column, this)

  /**
   * Used to reflect and test compilation result.
   *
   * @author 凛 (RinOrz)
   */
  open class ReflectionTest(
    private val className: String,
    protected val result: KotlinCompilation.Result
  ) {
    val javaReflection: Class<*> by lazy { result.classLoader.loadClass(className) }
    val kotlinReflection: KClass<*> by lazy { javaReflection.kotlin }
    val instance: Any? by lazy { javaReflection.constructors.shouldHaveSize(1).first().newInstance() }

    fun classOf(name: String, block: ReflectionTest.() -> Unit = {}): ReflectionTest =
      ReflectionTest(name, result).apply(block)

    fun callFunction(name: String, vararg args: Any?): Any? {
      kotlinReflection.shouldHaveFunction(name)
      return kotlinReflection.declaredFunctions.firstOrNull { it.name == name }!!.call(*args)
    }

    fun getProperty(name: String): Any? {
      kotlinReflection.shouldHaveMemberProperty(name)
      return kotlinReflection.declaredMemberProperties.firstOrNull { it.name == name }!!.call()
    }

    @JvmName("callTypedFunction")
    inline fun <reified R : Any> callFunction(name: String, vararg args: Any?): R? =
      callFunction(name, *args)?.shouldBeInstanceOf()

    @JvmName("callTypedProperty")
    inline fun <reified R : Any> getProperty(name: String): R? =
      getProperty(name)?.shouldBeInstanceOf()

    override fun equals(other: Any?): Boolean = other == instance
    override fun hashCode(): Int = instance.hashCode()
    override fun toString(): String = instance.toString()
  }

  /**
   * Used to test compilation result.
   *
   * @author 凛 (RinOrz)
   */
  class CompilationTest(
    sourceFileName: String,
    result: KotlinCompilation.Result
  ) : ReflectionTest("${sourceFileName}Kt", result) {

    val messages: String get() = result.messages

    fun shouldBeOK() {
      result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    fun shouldBeError() {
      result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    fun shouldReport(row: Int, column: Int, message: String) {
      result.messages shouldContain "($row, $column): $message"
    }

    fun shouldContains(vararg message: String) {
      message.forAll { result.messages shouldContain it }
    }

    fun shouldContains(vararg message: LocationMessage) {
      message.forAll { result.messages shouldContain it.toString() }
    }

    infix fun shouldContain(message: LocationMessage) {
      result.messages shouldContain message.toString()
    }

    fun shouldNotContains(vararg message: String) {
      message.forAll { result.messages shouldNotContain it }
    }

    fun shouldNotContains(vararg message: LocationMessage) {
      message.forAll { result.messages shouldNotContain it.toString() }
    }

    infix fun shouldNotContain(message: LocationMessage) {
      result.messages shouldNotContain message.toString()
    }
  }

  fun compile(@Language("kotlin") sourcecode: String): CompilationTest = createKotlinCompilation(sourcecode) {
    CompilationTest(it, compile())
  }

  fun compile(
    @Language("kotlin") sourcecode: String,
    testing: CompilationTest.() -> Unit = {},
  ) = compile(sourcecode).let(testing)

  fun addSource(@Language("kotlin") sourcecode: String) {
    baseSources += SourceFile.kotlin(nextFileName() + ".kt", sourcecode)
  }

  protected val baseSources = mutableListOf<SourceFile>()
  private val sourcesId = AtomicInteger()

  protected fun nextFileName() = javaClass.simpleName + "$" + sourcesId.incrementAndGet()

  protected inline fun <R> createKotlinCompilation(
    @Language("kotlin") sourcecode: String,
    block: KotlinCompilation.(sourceFileName: String) -> R
  ): R {
    val sourceFileName = nextFileName()
    return KotlinCompilation().let {
      it.sources = baseSources + listOf(SourceFile.kotlin("$sourceFileName.kt", sourcecode))
      it.compilerPlugins = listOf(CompilerComponentRegistrar())
      it.kotlincArguments = listOf("-Xjvm-default=compatibility")
      it.jvmDefault = JvmTarget.JVM_11.description
      it.jvmTarget = JvmTarget.JVM_11.description
      it.inheritClassPath = true
      it.verbose = false
      it.useIR = true
      it.block(sourceFileName)
    }
  }

  /**
   * @author 凛 (RinOrz)
   */
  protected inner class CompilerComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
      StorageComponentContainerContributor.registerExtension(project, object : StorageComponentContainerContributor {
        override fun registerModuleComponents(
          container: StorageComponentContainer,
          platform: TargetPlatform,
          moduleDescriptor: ModuleDescriptor
        ) {
          metaExtensions.filterNot { it is CodeProcessor }.forEach {
            it.context = TestMetaContext
            container.useInstance(it)
          }
        }
      })
      IrGenerationExtension.registerExtension(project, object : IrGenerationExtension {
        override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
          transformRemapping(moduleFragment, pluginContext) { context, module, symbolRemapper ->
            metaExtensions.forEach {
              if (it is CodeProcessor) {
                it.context = TestMetaContext
                it.pluginContext = context
                it.moduleFragment = module
                it.symbolRemapper = symbolRemapper
                moduleFragment.transformChildrenVoid(it)
              }
            }
          }
        }
      })
    }
  }
}