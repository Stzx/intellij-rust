/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.miscExtensions

import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.rust.lang.RsLanguage
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.RsElement
import org.rust.lang.core.psi.ext.RsNamedElement
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement
import org.rust.lang.core.psi.ext.skipParens

class RsBreadcrumbsInfoProvider : BreadcrumbsProvider {

    private interface RsElementHandler<T : RsElement> {
        fun accepts(e: PsiElement): Boolean

        fun elementInfo(e: T): String

        fun elementTooltip(e: T): String
    }

    private val handlers = listOf<RsElementHandler<*>>(
        RsNamedHandler,
        RsImplHandler,
        RsBlockExprHandler,
        RsMacroHandler,
        RsFunctionHandler,
        RsIfHandler,
        RsElseHandler,
        RsLoopHandler,
        RsForHandler,
        RsWhileHandler,
        RsMatchHandler,
        RsMatchArmHandler,
        RsLambdaHandler
    )

    private object RsNamedHandler : RsElementHandler<RsNamedElement> {
        override fun accepts(e: PsiElement): Boolean =
            e is RsModItem || e is RsStructOrEnumItemElement || e is RsTraitItem
                || e is RsConstant || e is RsTypeAlias

        override fun elementInfo(e: RsNamedElement): String = e.name.let { "$it" }

        override fun elementTooltip(e: RsNamedElement): String = e.name.let { "$it" }
    }

    private object RsImplHandler : RsElementHandler<RsImplItem> {
        override fun accepts(e: PsiElement): Boolean = e is RsImplItem

        override fun elementInfo(e: RsImplItem): String = e.buildText()

        override fun elementTooltip(e: RsImplItem): String = e.buildText()

        private fun RsImplItem.buildText(): String {
            val typeName = run {
                val typeReference = typeReference
                (typeReference?.skipParens() as? RsBaseType)?.path?.referenceName
                    ?: typeReference?.text
            } ?: return ""

            val traitName = traitRef?.path?.referenceName
            val start = if (traitName != null) "$traitName for" else "impl"

            return "$start $typeName"
        }
    }

    private object RsBlockExprHandler : RsElementHandler<RsBlockExpr> {
        override fun accepts(e: PsiElement): Boolean =
            e is RsBlockExpr && (e.parent is RsBlock || e.parent is RsLetDecl)

        override fun elementInfo(e: RsBlockExpr): String = e.buildText()

        override fun elementTooltip(e: RsBlockExpr): String = e.buildText()

        private fun RsBlockExpr.buildText(): String {
            val element = this

            return buildString {
                if (element.labelDecl != null) {
                    append(element.labelDecl?.text).append(' ')
                }

                append("{...}")
            }
        }
    }

    private object RsMacroHandler : RsElementHandler<RsMacro> {
        override fun accepts(e: PsiElement): Boolean = e is RsMacro

        override fun elementInfo(e: RsMacro): String = e.name.let { "$it!" }

        override fun elementTooltip(e: RsMacro): String = e.name.let { "$it!" }
    }

    private object RsFunctionHandler : RsElementHandler<RsFunction> {
        override fun accepts(e: PsiElement): Boolean = e is RsFunction

        override fun elementInfo(e: RsFunction): String = e.name.let { "$it()" }

        override fun elementTooltip(e: RsFunction): String = e.name.let { "$it()" }
    }

    private object RsIfHandler : RsElementHandler<RsIfExpr> {
        override fun accepts(e: PsiElement): Boolean = e is RsIfExpr

        override fun elementInfo(e: RsIfExpr): String = e.buildText(TextKind.INFO)

        override fun elementTooltip(e: RsIfExpr): String = e.buildText(TextKind.TOOLTIP)

        private fun RsIfExpr.buildText(kind: TextKind): String {
            val element = this

            return buildString {
                append("if")

                val condition = element.condition
                if (condition != null) {
                    if (condition.expr is RsBlockExpr) {
                        append(" {...}")
                    } else {
                        append(' ').append(condition.text.truncate(kind))
                    }
                }
            }
        }
    }

    private object RsElseHandler : RsElementHandler<RsElseBranch> {
        override fun accepts(e: PsiElement): Boolean = e is RsElseBranch

        override fun elementInfo(e: RsElseBranch): String = "else"

        override fun elementTooltip(e: RsElseBranch): String = "else"
    }

    private object RsLoopHandler : RsElementHandler<RsLoopExpr> {
        override fun accepts(e: PsiElement): Boolean = e is RsLoopExpr

        override fun elementInfo(e: RsLoopExpr): String = e.buildText()

        override fun elementTooltip(e: RsLoopExpr): String = e.buildText()

        private fun RsLoopExpr.buildText(): String {
            val element = this

            return buildString {
                appendLabelInfo(element.labelDecl)
                append("loop")
            }
        }
    }

    private object RsForHandler : RsElementHandler<RsForExpr> {
        override fun accepts(e: PsiElement): Boolean = e is RsForExpr

        override fun elementInfo(e: RsForExpr): String = e.buildText(TextKind.INFO)

        override fun elementTooltip(e: RsForExpr): String = e.buildText(TextKind.TOOLTIP)

        private fun RsForExpr.buildText(kind: TextKind): String {
            val element = this
            return buildString {
                appendLabelInfo(element.labelDecl)
                append("for")

                if (element.block != null) {
                    val pat = element.pat
                    if (pat != null) {
                        append(' ').append(pat.text)
                    }

                    append(" in ").append(element.expr?.text?.truncate(kind))
                } else {
                    append(" {...}")
                }
            }
        }
    }

    private object RsWhileHandler : RsElementHandler<RsWhileExpr> {
        override fun accepts(e: PsiElement): Boolean = e is RsWhileExpr

        override fun elementInfo(e: RsWhileExpr): String = e.buildText(TextKind.INFO)

        override fun elementTooltip(e: RsWhileExpr): String = e.buildText(TextKind.TOOLTIP)

        private fun RsWhileExpr.buildText(kind: TextKind): String {
            val element = this

            return buildString {
                appendLabelInfo(element.labelDecl)
                append("while")

                val condition = element.condition
                if (condition != null) {
                    if (condition.expr is RsBlockExpr) {
                        append(" {...}")
                    } else {
                        append(' ').append(condition.text.truncate(kind))
                    }
                }
            }
        }
    }

    private object RsMatchHandler : RsElementHandler<RsMatchExpr> {
        override fun accepts(e: PsiElement): Boolean = e is RsMatchExpr

        override fun elementInfo(e: RsMatchExpr): String = e.buildText(TextKind.INFO)

        override fun elementTooltip(e: RsMatchExpr): String = e.buildText(TextKind.TOOLTIP)

        private fun RsMatchExpr.buildText(kind: TextKind): String {
            val element = this

            return buildString {
                append("match")

                val expr = element.expr
                if (expr != null) {
                    if (expr is RsBlockExpr && element.matchBody == null) {
                        append(" {...}")
                    } else {
                        append(' ').append(expr.text.truncate(kind))
                    }
                }
            }
        }
    }

    private object RsMatchArmHandler : RsElementHandler<RsMatchArm> {
        override fun accepts(e: PsiElement): Boolean = e is RsMatchArm

        override fun elementInfo(e: RsMatchArm): String = e.buildText(TextKind.INFO)

        override fun elementTooltip(e: RsMatchArm): String = e.buildText(TextKind.TOOLTIP)

        private fun RsMatchArm.buildText(kind: TextKind): String = "${orPats.text.truncate(kind)} =>"
    }

    private object RsLambdaHandler : RsElementHandler<RsLambdaExpr> {
        override fun accepts(e: PsiElement): Boolean = e is RsLambdaExpr

        override fun elementInfo(e: RsLambdaExpr): String = "${e.valueParameterList.text} {...}"

        override fun elementTooltip(e: RsLambdaExpr): String = "${e.valueParameterList.text} {...}"
    }

    @Suppress("UNCHECKED_CAST")
    private fun handler(e: PsiElement): RsElementHandler<in RsElement>? {
        return if (e is RsElement)
            handlers.firstOrNull { it.accepts(e) } as RsElementHandler<in RsElement>?
        else null
    }

    override fun getLanguages(): Array<RsLanguage> = LANGUAGES

    override fun acceptElement(e: PsiElement): Boolean = handler(e) != null

    override fun getElementInfo(e: PsiElement): String = handler(e)!!.elementInfo(e as RsElement)

    override fun getElementTooltip(e: PsiElement): String? = handler(e)!!.elementTooltip(e as RsElement)

    companion object {
        private enum class TextKind(val maxTextLength: Int) {
            INFO(16), TOOLTIP(100)
        }

        private val LANGUAGES = arrayOf(RsLanguage)

        const val ellipsis = "${Typography.ellipsis}"

        private fun String.truncate(kind: TextKind): String {
            val maxLength = kind.maxTextLength
            return if (length > maxLength)
                "${substring(0, maxLength - ellipsis.length)}$ellipsis"
            else this
        }

        private fun StringBuilder.appendLabelInfo(labelDecl: RsLabelDecl?) {
            if (labelDecl != null) {
                append(labelDecl.text).append(' ')
            }
        }
    }
}
