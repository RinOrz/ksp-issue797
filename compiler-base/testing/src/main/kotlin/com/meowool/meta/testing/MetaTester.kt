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
@file:Suppress("SpellCheckingInspection", "MemberVisibilityCanBePrivate", "unused", "JAVA_CLASS_ON_COMPANION")

package com.meowool.meta.testing

import com.meowool.meta.Meta
import com.meowool.meta.MetaExtension
import com.meowool.meta.analysis.Analyzer
import com.meowool.meta.codegen.CodeProcessor
import com.meowool.meta.diagnostics.BaseDiagnosticFactoryProvider
import com.meowool.meta.internal.processMeta
import com.meowool.meta.synthesizers.CompanionObjectSynthesizer
import com.meowool.meta.synthesizers.ConstructorsSynthesizer
import com.meowool.meta.synthesizers.FunctionsSynthesizer
import com.meowool.meta.synthesizers.PropertiesSynthesizer
import com.meowool.sweekt.castOrNull
import com.meowool.sweekt.firstCharTitlecase
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.assertions.withClue
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
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
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import java.io.File
import java.lang.invoke.MethodHandles
import java.util.ArrayList
import java.util.ServiceLoader
import java.util.concurrent.atomic.AtomicInteger

/**
 * Dedicated tester for testing compilation of compiler meta plugins.
 *
 * @param metaExtensions Extension instances annotated with [Meta].
 * @author 凛 (RinOrz)
 */
open class MetaTester(vararg val metaExtensions: MetaExtension) {
  init {
    // Initialize all diagnostic extensions
    ServiceLoader.load(DefaultErrorMessages.Extension::class.java).forEach { _ -> }
  }

  /**
   * Simple render diagnostic factory.
   *
   * ''{0}'', {1} -> 'arg0', arg1
   */
  operator fun BaseDiagnosticFactoryProvider.invoke(vararg args: Any): String =
    args.foldIndexed(this.message) { index, acc, arg ->
      acc.replace("'{$index}'", arg.toString()).replace("{$index}", arg.toString())
    }.replace(Regex("(?<!')''(?!')"), "'") // Replace '' with ': ''50'' -> '50'

  /**
   * Represents a message with a location.
   *
   * @author 凛 (RinOrz)
   */
  data class LocationMessage(val row: Int, val column: Int?, val message: String) {
    val regex: Regex = Regex("\\($row, ${column ?: ".+"}\\): ${Regex.escape(message)}")
  }

  fun String.atLocation(row: Int, column: Int? = null): LocationMessage = LocationMessage(row, column, this)

  fun BaseDiagnosticFactoryProvider.atLocation(row: Int, column: Int? = null): LocationMessage =
    LocationMessage(row, column, this.message)

  /**
   * Used to reflect and test compilation result.
   *
   * @author 凛 (RinOrz)
   */
  open class ReflectionTest(
    private val className: String,
    protected val result: KotlinCompilation.Result
  ) {
    val reflectClass: Class<*> by lazy { result.classLoader.loadClass(className) }
    val instance: Any? by lazy { reflectClass.constructors.singleOrNull()?.newInstance() }

    private val lookup by lazy { MethodHandles.privateLookupIn(reflectClass, MethodHandles.lookup()) }

    fun classOf(name: String, block: ReflectionTest.() -> Unit = {}): ReflectionTest =
      ReflectionTest(name, result).apply(block)

    fun companionObjectOf(name: String, block: ReflectionTest.() -> Unit = {}): ReflectionTest =
      ReflectionTest("$name\$Companion", result).apply(block)

    fun callFunction(name: String, vararg args: Any?): Any? =
      lookup.unreflect(reflectClass.shouldHaveMethod(name)).invokeFast(instance, *args)

    fun getField(name: String): Any? =
      lookup.unreflectGetter(reflectClass.shouldHaveField(name)).invokeFast(instance)

    fun setField(name: String, newValue: Any?) =
      lookup.unreflectSetter(reflectClass.shouldHaveField(name)).invokeFast(instance, newValue)

    fun getProperty(name: String): Any? = callFunction("get${name.firstCharTitlecase()}")

    fun setProperty(name: String, newValue: Any?): Any? = callFunction("set${name.firstCharTitlecase()}", newValue)

    @JvmName("callTypedFunction")
    inline fun <reified R : Any> callFunction(name: String, vararg args: Any?): R? =
      callFunction(name, *args)?.shouldBeInstanceOf()

    @JvmName("callTypedField")
    inline fun <reified R : Any> getField(name: String): R? =
      getField(name)?.shouldBeInstanceOf()

    @JvmName("callTypedProperty")
    inline fun <reified R : Any> getProperty(name: String): R? =
      callFunction<R>("get${name.firstCharTitlecase()}")

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
  ) : ReflectionTest("${sourceFileName.replace('$', '_')}Kt", result) {

    val messages: String get() = result.messages

    val outputFiles: List<File> get() = result.compiledClassAndResourceFiles

    fun shouldBeOK() {
      result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    fun shouldBeError() {
      result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    fun shouldEmptyMessage() {
      result.messages.shouldBeEmpty()
    }

    fun shouldNotContainWarns() {
      result.messages shouldNotContain Regex("^w: ")
      result.messages shouldNotContain Regex("^e: ")
    }

    fun shouldNotContainErrors() {
      result.messages shouldNotContain Regex("^e:")
    }

    fun shouldContainsInOrder(baseRow: Int, vararg messages: String?) {
      messages.mapIndexedNotNull { index, message ->
        message?.let { LocationMessage(baseRow + index, null, it) }
      }.forAll { result.messages shouldContain it.regex }
    }

    fun shouldContainsInOrder(baseRow: Int, vararg messages: BaseDiagnosticFactoryProvider?) =
      shouldContainsInOrder(baseRow, *messages.map { it?.message }.toTypedArray())

    fun shouldContains(vararg messages: String) {
      messages.forAll { result.messages shouldContain it }
    }

    fun shouldContains(vararg messages: LocationMessage) {
      messages.forAll { result.messages shouldContain it.regex }
    }

    fun shouldContains(vararg messages: BaseDiagnosticFactoryProvider) =
      shouldContains(*messages.map { it.message }.toTypedArray())

    infix fun shouldContain(message: String) {
      result.messages shouldContain message
    }

    infix fun shouldContain(message: LocationMessage) {
      result.messages shouldContain message.regex
    }

    infix fun shouldContain(message: BaseDiagnosticFactoryProvider) = shouldContain(message.message)

    fun shouldNotContains(vararg message: String) {
      message.forAll { result.messages shouldNotContain it }
    }

    fun shouldNotContains(vararg message: LocationMessage) {
      message.forAll { result.messages shouldNotContain it.regex }
    }

    fun shouldNotContains(vararg message: BaseDiagnosticFactoryProvider) =
      shouldNotContains(*message.map { it.message }.toTypedArray())

    infix fun shouldNotContain(message: String) {
      result.messages shouldNotContain message
    }

    infix fun shouldNotContain(message: LocationMessage) {
      result.messages shouldNotContain message.regex
    }

    infix fun shouldNotContain(message: BaseDiagnosticFactoryProvider) = shouldNotContain(message.message)

    infix fun String.shouldAppear(count: Int) {
      withClue("The printed messages should appear substring '$this' $count time(s).") {
        Regex(Regex.escape(this)).findAll(messages).count() shouldBe count
      }
    }

    infix fun Regex.shouldAppear(count: Int) {
      withClue("The printed messages should appear substring regex '${this.pattern}' $count time(s).") {
        this.findAll(messages).count() shouldBe count
      }
    }
  }

  fun compile(@Language("kotlin") sourcecode: String): CompilationTest = createKotlinCompilation(sourcecode) {
    CompilationTest(it, compile())
  }.apply {
    println("// OUTPUT:")
    outputFiles.forEach(::println)
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
      metaExtensions.forEach { it.context = TestMetaContext }
      StorageComponentContainerContributor.registerExtension(
        project,
        object : StorageComponentContainerContributor {
          override fun registerModuleComponents(
            container: StorageComponentContainer,
            platform: TargetPlatform,
            moduleDescriptor: ModuleDescriptor
          ) {
            metaExtensions.filterIsInstance<Analyzer>().forEach {
              it.componentContainer = container
              container.useInstance(it)
            }
          }
        }
      )
      SyntheticResolveExtension.registerExtension(
        project,
        object : SyntheticResolveExtension {
          override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? {
            metaExtensions.filterIsInstance<CompanionObjectSynthesizer>().forEach {
              it.getName(thisDescriptor)?.apply { return this }
            }
            return super.getSyntheticCompanionObjectNameIfNeeded(thisDescriptor)
          }

          override fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> = buildList {
            metaExtensions.filterIsInstance<PropertiesSynthesizer>().forEach {
              this += it.getNames(thisDescriptor)
            }
          }

          override fun generateSyntheticProperties(
            thisDescriptor: ClassDescriptor,
            name: Name,
            bindingContext: BindingContext,
            fromSupertypes: ArrayList<PropertyDescriptor>,
            result: MutableSet<PropertyDescriptor>
          ) {
            metaExtensions.filterIsInstance<PropertiesSynthesizer>().forEach {
              it.generate(name, thisDescriptor, bindingContext, fromSupertypes, result)
            }
            super.generateSyntheticProperties(thisDescriptor, name, bindingContext, fromSupertypes, result)
          }

          override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> = buildList {
            metaExtensions.filterIsInstance<FunctionsSynthesizer>().forEach {
              this += it.getNames(thisDescriptor)
            }
          }

          override fun generateSyntheticMethods(
            thisDescriptor: ClassDescriptor,
            name: Name,
            bindingContext: BindingContext,
            fromSupertypes: List<SimpleFunctionDescriptor>,
            result: MutableCollection<SimpleFunctionDescriptor>
          ) {
            metaExtensions.filterIsInstance<FunctionsSynthesizer>().forEach {
              it.generate(name, thisDescriptor, bindingContext, fromSupertypes, result)
            }
            super.generateSyntheticMethods(thisDescriptor, name, bindingContext, fromSupertypes, result)
          }

          override fun generateSyntheticSecondaryConstructors(
            thisDescriptor: ClassDescriptor,
            bindingContext: BindingContext,
            result: MutableCollection<ClassConstructorDescriptor>
          ) {
            metaExtensions.filterIsInstance<ConstructorsSynthesizer>().forEach {
              it.generate(thisDescriptor, bindingContext, result)
            }
            super.generateSyntheticSecondaryConstructors(thisDescriptor, bindingContext, result)
          }
        }
      )
      IrGenerationExtension.registerExtension(
        project,
        object : IrGenerationExtension {
          override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
            metaExtensions.forEach {
              it.castOrNull<CodeProcessor.Callback.Start>()?.action(pluginContext, moduleFragment)
            }
            moduleFragment.processMeta(pluginContext) { context, module, referencedSymbolRemapper ->
              metaExtensions.forEach {
                if (it is CodeProcessor) {
                  it.pluginContext = context
                  it.moduleFragment = module
                  it.referencedSymbolRemapper = referencedSymbolRemapper
                  moduleFragment.transformChildrenVoid(it)
                }
              }
            }
            metaExtensions.forEach {
              it.castOrNull<CodeProcessor.Callback.End>()?.action(pluginContext, moduleFragment)
            }
          }
        }
      )
    }
  }
}
