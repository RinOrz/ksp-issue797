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
package com.meowool.meta.testing

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import java.lang.invoke.MethodHandle
import java.lang.reflect.Field
import java.lang.reflect.Method

fun Class<*>.shouldHaveMethod(name: String): Method? {
  this should object : Matcher<Class<*>> {
    override fun test(value: Class<*>) = MatcherResult(
      value.declaredMethods.any { it.name == name },
      { "Class $value should have method $name" },
      { "Class $value should not have method $name" }
    )
  }
  return declaredMethods.singleOrNull { it.name == name }
}

fun Class<*>.shouldHaveField(name: String): Field? {
  this should object : Matcher<Class<*>> {
    override fun test(value: Class<*>) = MatcherResult(
      value.declaredFields.any { it.name == name },
      { "Class $value should have field $name" },
      { "Class $value should not have field $name" }
    )
  }
  return declaredFields.singleOrNull { it.name == name }
}

fun MethodHandle.invokeOrStatic(instanceOrNull: Any?, vararg args: Any?): Any? =
  if (instanceOrNull == null) invoke() else invoke(instanceOrNull)

fun MethodHandle.invokeFast(instance: Any?, vararg args: Any?): Any? {
  val a = if (instance == null) args else arrayOf(instance, *args)
  return when (a.size) {
    0 -> invoke()
    1 -> invoke(a[0])
    2 -> invoke(a[0], a[1])
    3 -> invoke(a[0], a[1], a[2])
    4 -> invoke(a[0], a[1], a[2], a[3])
    5 -> invoke(a[0], a[1], a[2], a[3], a[4])
    6 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5])
    7 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5], a[6])
    8 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7])
    9 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8])
    10 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9])
    11 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10])
    12 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11])
    13 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12])
    14 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13])
    15 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14])
    16 -> invoke(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15])
    17 -> invoke(
      a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15], a[16]
    )
    18 -> invoke(
      a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15], a[16],
      a[17]
    )
    19 -> invoke(
      a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15], a[16],
      a[17], a[18]
    )
    20 -> invoke(
      a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15], a[16],
      a[17], a[18], a[19]
    )
    21 -> invoke(
      a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15], a[16],
      a[17], a[18], a[19], a[20]
    )
    22 -> invoke(
      a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15], a[16],
      a[17], a[18], a[19], a[20], a[21]
    )
    23 -> invoke(
      a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15], a[16],
      a[17], a[18], a[19], a[20], a[21], a[22]
    )
    24 -> invoke(
      a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15], a[16],
      a[17], a[18], a[19], a[20], a[21], a[22], a[23]
    )
    25 -> invoke(
      a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15], a[16],
      a[17], a[18], a[19], a[20], a[21], a[22], a[23], a[24]
    )
    26 -> invoke(
      a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15], a[16],
      a[17], a[18], a[19], a[20], a[21], a[22], a[23], a[24], a[25]
    )
    else -> invokeWithArguments(*a)
  }
}
