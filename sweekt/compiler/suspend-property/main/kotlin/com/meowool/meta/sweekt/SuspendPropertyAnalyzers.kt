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
package com.meowool.meta.sweekt

import com.meowool.meta.Meta
import com.meowool.meta.analysis.CallAnalyzerContext
import com.meowool.meta.analyzers
import com.meowool.meta.diagnostics.DiagnosticFactory0Provider
import com.meowool.meta.utils.hasAnnotation
import com.meowool.sweekt.cast
import com.meowool.sweekt.castOrNull
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.isCallableReference
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

/**
 * Analyzes getters of properties annotated with @Suspend.
 *
 * ```
 * @Suspend var foo: String
 *   // REQUIRED
 *   get() = suspendGetter {}
 *   // REQUIRED
 *   set(value) = suspendSetter {}
 * ```
 *
 * @author 凛 (RinOrz)
 */
@Meta val SuspendPropertyAnalyzer = analyzers.property(
  premise = { it.hasAnnotation(SUSPEND) && it.modality != Modality.ABSTRACT },
  analyzing = {
    fun KtPropertyAccessor.reportIfAccessorBody(condition: KtPropertyAccessor.() -> Boolean, message: DiagnosticFactory0Provider) {
      reportIfNot(condition()) {
        message.on(initializer ?: bodyBlockExpression ?: bodyExpression ?: this)
      }
    }

    reportIf(property.accessors.isEmpty(), termination = true) { SUSPEND_PROPERTY_WITHOUT_ACCESSORS_ERROR }

    property.getter?.reportIfAccessorBody(
      condition = { initializer.resolvedCallee?.fqNameSafe == SUSPEND_GETTER },
      message = SUSPEND_GETTER_INITIALIZER_ERROR
    )
    property.setter?.reportIfAccessorBody(
      condition = { initializer.resolvedCallee?.fqNameSafe == SUSPEND_SETTER },
      message = SUSPEND_SETTER_INITIALIZER_ERROR
    )
    descriptor.backingField?.reportIfMark(JVM_FIELD_ANNOTATION_FQ_NAME) { SUSPEND_PROPERTY_MARKED_JVM_FIELD_ERROR }
  }
)

/**
 * Analyze whether it is the overrides of @Suspend property.
 * Overridden property also need to be marked with @Suspend annotation.
 *
 * @author 凛 (RinOrz)
 */
@Meta val overriddenSuspendPropertyAnalyzer = analyzers.property(
  premise = { descriptor -> descriptor.overriddenDescriptors.any { it.hasAnnotation(SUSPEND) } },
  analyzing = {
    descriptor.reportIfNotMark(SUSPEND) { OVERRIDDEN_SUSPEND_PROPERTY_NOT_MARKED_ERROR }
  }
)

/**
 * Analyze whether the call targets are properties marked with the @Suspend and
 * ensure that they are called in suspend context.
 *
 * [Copy of Kotlin](https://github.com/JetBrains/kotlin/blob/1.6.20/compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/checkers/coroutineCallChecker.kt)
 *
 * @author 凛 (RinOrz)
 */
@Meta val SuspendPropertyCallAnalyzer = analyzers.call(
  premise = {
    when (val callee = resultingDescriptor) {
      is PropertyAccessorDescriptor -> callee.correspondingProperty
      else -> callee
    }.hasAnnotation(SUSPEND)
  },
  analyzing = {
    val callElement = requireNotNull(rawCall.callElement as? KtExpression)
    val resolution = requireNotNull(resolution)
    val scope = requireNotNull(scope)
    val enclosingSuspendFunction = findEnclosingSuspendFunction()

    when {
      enclosingSuspendFunction != null -> {
        if (!InlineUtil.checkNonLocalReturnUsage(enclosingSuspendFunction, callElement, resolution)) {
          var shouldReport = true

          // Do not report for KtCodeFragment in a suspend function context
          val containingFile = callElement.containingFile
          if (containingFile is KtCodeFragment) {
            val c = containingFile.context?.getParentOfType<KtExpression>(false)
            if (c != null && InlineUtil.checkNonLocalReturnUsage(enclosingSuspendFunction, c, resolution)) {
              shouldReport = false
            }
          }

          if (shouldReport) {
            trace.report(Errors.NON_LOCAL_SUSPENSION_POINT.on(analyzed))
          }
        } else if (scope.parentsWithSelf.any { it.isScopeForDefaultParameterValuesOf(enclosingSuspendFunction) }) {
          trace.report(
            Errors.UNSUPPORTED.on(
              analyzed,
              "suspend function calls in a context of default parameter value"
            )
          )
        }

        trace.record(
          BindingContext.ENCLOSING_SUSPEND_FUNCTION_FOR_SUSPEND_FUNCTION_CALL,
          rawCall,
          enclosingSuspendFunction
        )

        checkRestrictsSuspension(enclosingSuspendFunction)
      }
      rawCall.isCallableReference() -> {
        // do nothing: we can get callable reference to suspend function outside suspend context
      }
      else -> {
        when (callee) {
          is FunctionDescriptor -> trace.report(Errors.ILLEGAL_SUSPEND_FUNCTION_CALL.on(analyzed, callee))
          is PropertyDescriptor -> trace.report(Errors.ILLEGAL_SUSPEND_PROPERTY_ACCESS.on(analyzed, callee))
        }
      }
    }
  }
)

private val ALLOWED_SCOPE_KINDS = setOf(
  LexicalScopeKind.FUNCTION_INNER_SCOPE,
  LexicalScopeKind.FUNCTION_HEADER_FOR_DESTRUCTURING
)

private fun CallAnalyzerContext<*>.findEnclosingSuspendFunction(): FunctionDescriptor? =
  requireNotNull(scope).parentsWithSelf.firstOrNull {
    it is LexicalScope &&
      it.kind in ALLOWED_SCOPE_KINDS &&
      it.ownerDescriptor.castOrNull<FunctionDescriptor>()?.isSuspend == true
  }?.cast<LexicalScope>()?.ownerDescriptor?.cast()

private fun HierarchicalScope.isScopeForDefaultParameterValuesOf(enclosingSuspendFunction: FunctionDescriptor) =
  this is LexicalScope && this.kind == LexicalScopeKind.DEFAULT_VALUE && this.ownerDescriptor == enclosingSuspendFunction

private fun KotlinType.isRestrictsSuspensionReceiver() = (listOf(this) + this.supertypes()).any {
  it.constructor.declarationDescriptor?.annotations?.hasAnnotation(
    StandardNames.COROUTINES_PACKAGE_FQ_NAME.child(Name.identifier("RestrictsSuspension"))
  ) == true
}

private fun CallAnalyzerContext<*>.checkRestrictsSuspension(enclosingSuspendCallableDescriptor: CallableDescriptor) {
  fun ReceiverValue.isRestrictsSuspensionReceiver() = type.isRestrictsSuspensionReceiver()

  infix fun ReceiverValue.sameInstance(other: ReceiverValue?): Boolean {
    if (other == null) return false
    // Implicit receiver should be reference equal
    if (this.original === other.original) return true

    val referenceExpression = ((other as? ExpressionReceiver)?.expression as? KtThisExpression)?.instanceReference
    val referenceTarget = referenceExpression?.let {
      trace.get(BindingContext.REFERENCE_TARGET, referenceExpression)
    }

    val referenceReceiverValue = when (referenceTarget) {
      is CallableDescriptor -> referenceTarget.extensionReceiverParameter?.value
      is ClassDescriptor -> referenceTarget.thisAsReceiverParameter.value
      else -> null
    }

    return this === referenceReceiverValue
  }

  fun reportError() {
    trace.report(Errors.ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL.on(analyzed))
  }

  val enclosingSuspendExtensionReceiverValue = enclosingSuspendCallableDescriptor.extensionReceiverParameter?.value
  val enclosingSuspendDispatchReceiverValue = enclosingSuspendCallableDescriptor.dispatchReceiverParameter?.value

  val receivers = listOfNotNull(call.dispatchReceiver, call.extensionReceiver)
  for (receiverValue in receivers) {
    if (!receiverValue.isRestrictsSuspensionReceiver()) continue
    if (enclosingSuspendExtensionReceiverValue?.sameInstance(receiverValue) == true) continue
    if (enclosingSuspendDispatchReceiverValue?.sameInstance(receiverValue) == true) continue

    reportError()
    return
  }

  if (enclosingSuspendExtensionReceiverValue?.isRestrictsSuspensionReceiver() != true) return

  // member of suspend receiver
  if (enclosingSuspendExtensionReceiverValue sameInstance call.dispatchReceiver) return

  if (enclosingSuspendExtensionReceiverValue sameInstance call.extensionReceiver &&
    callee.extensionReceiverParameter!!.value.isRestrictsSuspensionReceiver()
  ) return

  reportError()
}
