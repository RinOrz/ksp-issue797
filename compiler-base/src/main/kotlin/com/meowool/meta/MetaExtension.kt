package com.meowool.meta

import com.meowool.meta.annotations.InternalCompilerApi

/**
 * Represents a meta-extension for a compiler plugin.
 *
 * @author 凛 (RinOrz)
 */
interface MetaExtension {
  var context: Context
    @InternalCompilerApi set

  interface Context {
    val loggable: Boolean

    companion object Default : Context {
      override val loggable: Boolean = false
    }
  }
}