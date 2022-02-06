        package com.meowool.meta.catnip

        import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
        import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

        /**
         * Automatically generated, do not edit.
         *
         * @author 凛 (RinOrz)
         */
        @OptIn(com.meowool.meta.internal.InternalCompilerApi::class)
        class DiagnosticsMessagesExtension : DefaultErrorMessages.Extension {
          override fun getMap(): DiagnosticFactoryToRendererMap = rendererMap

          init {
            com.meowool.meta.cloak.diagnostics.NOT_A_COMPILE_TIME_INITIALIZER_ERROR renderTo rendererMap
com.meowool.meta.cloak.diagnostics.EXPLICIT_TYPE_INSTANCE_ANALYZE_ERROR renderTo rendererMap
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_CREATING_EXPR_ANALYZE_ERROR renderTo rendererMap
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_INCOMPATIBLE_MODIFIER_ERROR renderTo rendererMap
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_HAVE_CONSTRUCTOR_ERROR renderTo rendererMap
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_NO_OVERRIDE_TYPE_ERROR renderTo rendererMap
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_MUTABLE_TYPE_ERROR renderTo rendererMap
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_UNINITIALIZED_TYPE_ERROR renderTo rendererMap
com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_ANALYZE_ERROR renderTo rendererMap
com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_IMPLICIT_RECEIVER_ERROR renderTo rendererMap
com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_ILLEGAL_CLASS_SUFFIX_ERROR renderTo rendererMap
          }

          companion object {
            private val rendererMap = DiagnosticFactoryToRendererMap("com.meowool.meta.catnip")

            init {
              com.meowool.meta.cloak.diagnostics.NOT_A_COMPILE_TIME_INITIALIZER_ERROR.apply {
  factory.initializeName("NOT_A_COMPILE_TIME_INITIALIZER_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
com.meowool.meta.cloak.diagnostics.EXPLICIT_TYPE_INSTANCE_ANALYZE_ERROR.apply {
  factory.initializeName("EXPLICIT_TYPE_INSTANCE_ANALYZE_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_CREATING_EXPR_ANALYZE_ERROR.apply {
  factory.initializeName("INSTANCE_MOCK_CREATING_EXPR_ANALYZE_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_INCOMPATIBLE_MODIFIER_ERROR.apply {
  factory.initializeName("INSTANCE_MOCK_INCOMPATIBLE_MODIFIER_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_HAVE_CONSTRUCTOR_ERROR.apply {
  factory.initializeName("INSTANCE_MOCK_HAVE_CONSTRUCTOR_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_NO_OVERRIDE_TYPE_ERROR.apply {
  factory.initializeName("INSTANCE_MOCK_NO_OVERRIDE_TYPE_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_MUTABLE_TYPE_ERROR.apply {
  factory.initializeName("INSTANCE_MOCK_MUTABLE_TYPE_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
com.meowool.meta.cloak.diagnostics.INSTANCE_MOCK_UNINITIALIZED_TYPE_ERROR.apply {
  factory.initializeName("INSTANCE_MOCK_UNINITIALIZED_TYPE_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_ANALYZE_ERROR.apply {
  factory.initializeName("REFLECTION_TYPE_ANALYZE_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_IMPLICIT_RECEIVER_ERROR.apply {
  factory.initializeName("REFLECTION_TYPE_IMPLICIT_RECEIVER_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
com.meowool.meta.cloak.diagnostics.REFLECTION_TYPE_ILLEGAL_CLASS_SUFFIX_ERROR.apply {
  factory.initializeName("REFLECTION_TYPE_ILLEGAL_CLASS_SUFFIX_ERROR")
  factory.initDefaultRenderer(rendererMap.get(factory))
}
            }
          }
        }