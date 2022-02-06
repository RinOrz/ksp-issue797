package com.meowool.meta.cloak

import com.meowool.sweekt.toJvmTypeDescriptor

inline fun <reified T : Any> primitiveDescriptor() =
  T::class.javaPrimitiveType!!.name.toJvmTypeDescriptor()

inline fun <reified T : Any> objectDescriptor() =
  T::class.javaObjectType.name.toJvmTypeDescriptor()
