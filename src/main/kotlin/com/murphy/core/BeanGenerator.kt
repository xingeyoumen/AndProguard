package com.murphy.core

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiType
import com.intellij.psi.impl.source.PsiFieldImpl
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl
import com.intellij.psi.util.PropertyUtilBase.findGetterForField
import com.intellij.psi.util.PropertyUtilBase.findSetterForField
import com.intellij.psi.util.PropertyUtilBase.suggestGetterName
import com.intellij.psi.util.PropertyUtilBase.suggestSetterName
import com.intellij.psi.util.PsiLiteralUtil.getStringLiteralContent
import com.murphy.util.PLUGIN_NAME
import com.murphy.util.notifyWarn
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.plainContent

object BeanGenerator : AbstractGenerator<PsiElement>() {
    private lateinit var beanMap: Map<String, String>
    override val name: String get() = "Bean"

    //选中当前需要使用的json文件。
    // 函数的主要目的是从用户选择的 JSON 文件中提取数据，并将其存储在 `beanMap` 中，最终返回一个布尔值表示是否成功准备数据。
    /**
     * 这段代码是一个 Kotlin 函数，命名为 `prepare`，接收一个 `Project` 类型的参数。函数的主要目的是从用户选择的 JSON 文件中提取数据，并将其存储在 `beanMap` 中，最终返回一个布尔值表示是否成功准备数据。
     *
     * 函数的具体步骤如下：
     *
     * 1. 创建一个 `FileChooserDescriptor` 对象，指定了文件选择器的一些属性，比如只允许选择 JSON 文件。
     * 2. 弹出文件选择对话框，让用户选择一个文件。
     * 3. 将选择的文件转换为 `JsonFile` 类型，然后提取其顶层值。
     * 4. 如果顶层值不是 `JsonObject` 类型，则发出警告通知并返回 `false`。
     * 5. 将 JSON 文件中的属性列表转换为一个映射（`Map`），并将其存储在 `beanMap` 中。
     * 6. 最后，检查 `beanMap` 是否包含数据，如果有则返回 `true`，否则返回 `false`。
     *
     * 整体来说，这个函数的作用是从用户选择的 JSON 文件中提取数据，并将其存储在一个映射中，以备进一步处理或使用。
     */
    fun prepare(project: Project): Boolean {
        //1. 创建一个 `FileChooserDescriptor` 对象，指定了文件选择器的一些属性，比如只允许选择 JSON 文件。
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withFileFilter { it.extension == "json" }
        //2. 弹出文件选择对话框，让用户选择一个文件。
        val chooseFile = FileChooser.chooseFile(descriptor, project, null)
        //3. 将选择的文件转换为 `JsonFile` 类型，然后提取其顶层值。
        val jsonFile = chooseFile?.toPsiFile(project) as? JsonFile
        val jsonValue = jsonFile?.topLevelValue
        //4. 如果顶层值不是 `JsonObject` 类型，则发出警告通知并返回 `false`。
        if (jsonValue !is JsonObject) {
            notifyWarn(project, "Only JsonObject is supported")
            return false
        }
        //5. 将 JSON 文件中的属性列表转换为一个映射（`Map`），并将其存储在 `beanMap` 中。
        beanMap = jsonValue.propertyList.mapNotNull {
            val literal = it.value
            if (literal !is JsonStringLiteral) null
            else Pair(it.name, literal.value)
        }.toMap()
        //6. 最后，检查 `beanMap` 是否包含数据，如果有则返回 `true`，否则返回 `false`。
        return beanMap.isNotEmpty()
    }

    override fun process(first: Project, second: ProgressIndicator, data: List<PsiElement>) {
        super.process(first, second, data)
        data.psiFilter<PsiFieldImpl> { beanMap.containsKey(it.name) }
            .map { service.dumbReadAction { JavaBeanStub(it) } }
            .alsoReset("Java")
            .forEach { it.stubRename() }
        data.psiFilter<KtClass>()
            .map { service.dumbReadAction { KtBeanStub(it) } }
            .alsoReset("Kotlin")
            .forEach { it.stubRename() }
        data.filterIsInstance<PsiLiteralExpressionImpl>()
            .mapNotNull {
                service.dumbReadAction {
                    val content = getStringLiteralContent(it)
                    if (content == null || !beanMap.containsKey(content)) null
                    else Pair(it, content)
                }
            }.alsoReset("PsiLiteral")
            .updateText()
        data.filterIsInstance<KtStringTemplateExpression>()
            .mapNotNull {
                service.dumbReadAction {
                    val content = it.plainContent
                    if (!beanMap.containsKey(content)) null
                    else Pair(it, content)
                }
            }.alsoReset("KtString")
            .updateText()
    }

    private fun <T : PsiLanguageInjectionHost> List<Pair<T, String>>.updateText() {
        WriteCommandAction.writeCommandAction(myProject)
            .withName(PLUGIN_NAME)
            .run<Throwable> {
                forEach {
                    val value = beanMap[it.second]!!
                    it.first.updateText("\"$value\"")
                    increase()
                }
            }
    }

    class JavaBeanStub(
        private val psiField: PsiField,
        private val getterForField: PsiMethod? = findGetterForField(psiField),
        private val setterForField: PsiMethod? = findSetterForField(psiField),
        private val type: PsiType = psiField.type,
        private val name: String = psiField.name
    ) {
        fun stubRename() {
            val newName = beanMap[name]!!
            getterForField?.rename(suggestGetterName(newName, type), "PsiFieldGetter")
            setterForField?.rename(suggestSetterName(newName), "PsiFieldSetter")
            psiField.rename(newName, "PsiField")
            increase()
        }
    }

    class KtBeanStub(
        ktClass: KtClass,
        private val ktp1: List<KtParameter> = ktClass.getPrimaryConstructorParameters().filterNamed(),
        private val ktp2: List<KtProperty> = ktClass.getProperties().filterNamed()
    ) {
        fun stubRename() {
            ktp1.forEach {
                val oldName = service.dumbReadAction { it.name }
                it.rename(beanMap[oldName]!!, "KtParameter")
            }
            ktp2.forEach {
                val oldName = service.dumbReadAction { it.name }
                it.rename(beanMap[oldName]!!, "KtProperty")
            }
            increase()
        }
    }

    private fun <T : PsiNamedElement> List<T>.filterNamed() = filter {
        it.name.run { this != null && beanMap.containsKey(this) }
    }
}