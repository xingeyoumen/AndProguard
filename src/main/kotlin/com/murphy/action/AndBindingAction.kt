package com.murphy.action

import com.android.resources.ResourceType
import com.android.tools.idea.databinding.module.LayoutBindingModuleCache
import com.android.tools.idea.databinding.util.DataBindingUtil
import com.android.tools.idea.res.psi.ResourceReferencePsiElement
import com.ibm.icu.text.SimpleDateFormat
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.murphy.config.AndProguardConfigState
import com.murphy.core.childrenDfsSequence
import com.murphy.core.computeTime
import com.murphy.core.rename
import com.murphy.util.PLUGIN_NAME
import com.murphy.util.notifyError
import com.murphy.util.notifyInfo
import org.jetbrains.android.facet.AndroidFacet
import java.util.*


/**
 * 区分可以进行哪些操作
 * 修改layout的文件夹，对应修改里面的layout name 和 id。
 * 使用viewBinding 的对应修改
 *
 */
class AndBindingAction : AnAction() {

    override fun actionPerformed(action: AnActionEvent) {
        /**
         * 从特定操作 action 中获取与之相关的 PSI 元素（如 XML 文件或目录），如果未能成功获取，则提前结束方法的执行。
         * 进一步，获取相关联的项目对象，如果未能成功获取，则同样提前结束方法的执行。
         */
        // 从操作中检索 PSI 元素
        val psi = action.getData(PlatformDataKeys.PSI_ELEMENT) ?: return
        // 这个条件语句检查 psi 是否不是 XmlFile 类型且不是 PsiDirectory 类型。
        // 如果 psi 既不是 XmlFile 也不是 PsiDirectory，则直接返回，提前结束当前方法的执行。
        if (psi !is XmlFile && psi !is PsiDirectory) return
        val project = action.project ?: return
        val dateStart = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> $dateStart [Refactor Start] <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
        val config = AndProguardConfigState.getInstance()

        // 初始化配置随机规则
        config.initRandomNode()
        // id layout
        val startTime = System.currentTimeMillis()
        var count = 0

        // Progress窗，进行修改
        ProgressManager.getInstance().run(object : Task.Modal(project, PLUGIN_NAME, false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0
                indicator.text = "Waiting search binding element..."

                val sequenceId = runReadAction { psi.seqResId }
                val sequenceLayout = runReadAction { psi.seqLayout }
                val size = sequenceId.size + sequenceLayout.size
                val total: Double = size.toDouble()
                sequenceId.forEach {
                    renameResId(project, it.first, it.second, config.randomIdResName)
                    indicator.fraction = ++count / total
                    indicator.text = "$count element of $size element"
                }
                sequenceLayout.forEach {
                    renameLayout(project, it.first, it.second, config.randomLayoutResName)
                    indicator.fraction = ++count / total
                    indicator.text = "$count element of $size element"
                }
            }

            override fun onSuccess() {
                notifyInfo(project, "refactor finished, take ${computeTime(startTime)}")
            }

            override fun onThrowable(error: Throwable) {
                notifyError(project, "${error.message}")
                error.printStackTrace()
            }
        })
        val dateEnd = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> $dateEnd [Refactor End] <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
    }


    /**
     *
     * layout
     */
    private val PsiElement.seqLayout
        get() = childrenDfsSequence()
            .filterIsInstance<XmlFile>()
            .mapNotNull { it.layoutMap }
            .toList()

    private val XmlFile.layoutMap
        get(): Pair<XmlFile, List<PsiReference>>? {
            val resPsi = ResourceReferencePsiElement.create(this) ?: return null
            if (resPsi.resourceReference.resourceType != ResourceType.LAYOUT) return null
            val facet = AndroidFacet.getInstance(this) ?: return null
            val scope = GlobalSearchScope.projectScope(project)
            val bindingModuleCache = LayoutBindingModuleCache.getInstance(facet)
            val groups = bindingModuleCache.bindingLayoutGroups
            val className = DataBindingUtil.convertFileNameToJavaClassName(resPsi.resourceReference.name) + "Binding"
            val layoutGroup = groups.firstOrNull { it.mainLayout.className == className } ?: return null
            val psiReferences = bindingModuleCache.getLightBindingClasses(layoutGroup)
                .map { ReferencesSearch.search(it, scope).findAll() }
                .flatten()
            return Pair(this, psiReferences)
        }

    private fun renameLayout(project: Project, element: XmlFile, list: List<PsiReference>, newName: String) {
        ApplicationManager.getApplication().invokeAndWait {
            element.rename(newName, "XmlFile")
        }
        if (list.isNotEmpty()) {
            val newRefName = DataBindingUtil.convertFileNameToJavaClassName(newName) + "Binding"
            println(String.format("[LayoutViewBinding] >>> %s", newRefName))
            WriteCommandAction.writeCommandAction(project).withName(PLUGIN_NAME).run<RuntimeException> {
                list.forEach { ref -> ref.handleElementRename(newRefName) }
            }
        }
    }


    /**
     *
     * ID
     */
    private val PsiElement.seqResId
        get() = childrenDfsSequence()
            // 在序列中过滤出类型为 XmlAttributeValue 的元素
            .filterIsInstance<XmlAttributeValue>()
            //对每个 XmlAttributeValue 元素应用 resMap 方法，并且过滤掉结果为 null 的元素
            .mapNotNull { it.resMap }
            //根据资源引用的名称对结果进行去重，保留每个名称的唯一实例
            .distinctBy { it.second.resourceReference.name }
            //对每个元素应用 idMap 方法，并过滤掉结果为 null 的元素
            .mapNotNull { it.idMap }
            .toList()


    /**
     * 用于从 XmlAttributeValue 中提取资源映射
     * 这个属性是私有的，只在当前文件内可见。
     *
     */
    private val XmlAttributeValue.resMap
        get(): Pair<XmlAttributeValue, ResourceReferencePsiElement>? {
            // 尝试从当前的 XmlAttributeValue 创建一个 ResourceReferencePsiElement 对象
            val resPsi = ResourceReferencePsiElement.create(this) ?: return null
            // 检查 resPsi 中的资源引用类型是否不是 ResourceType.ID（资源类型为 ID）。
            // 如果不是，直接返回 null
            if (resPsi.resourceReference.resourceType != ResourceType.ID) return null
            // 如果资源引用类型是 ID，返回一个包含当前 XmlAttributeValue 和 resPsi 的 Pair 对象
            return Pair(this, resPsi)
        }

    /**
     * 用于从包含 XmlAttributeValue 和 ResourceReferencePsiElement 的 Pair 中提取 ID 映射
     *
     * 从包含 XmlAttributeValue 和 ResourceReferencePsiElement 的 Pair 中提取 ID 映射信息。
     * 首先，通过 AndroidFacet 获取相关信息，然后在轻量级类中查找与字段名称匹配的字段，并获取其所有引用。
     * 最终返回一个包含 XmlAttributeValue 和引用列表的 Pair 对象
     */
    private val Pair<XmlAttributeValue, ResourceReferencePsiElement>.idMap
        get(): Pair<XmlAttributeValue, List<PsiReference>>? {
            // 获取 AndroidFacet 和相关信息
            // 获取与第一个元素相关联的 AndroidFacet
            val facet = AndroidFacet.getInstance(first) ?: return null
            // 创建一个作用域对象，用于搜索项目中的元素
            val scope = GlobalSearchScope.projectScope(first.project)
            // 获取布局绑定模块缓存对象
            val bindingModuleCache = LayoutBindingModuleCache.getInstance(facet)
            // 获取绑定布局组信息
            val groups = bindingModuleCache.bindingLayoutGroups
            // 获取轻量级绑定类信息
            val lightClasses = groups.flatMap { group -> bindingModuleCache.getLightBindingClasses(group) }
            // 将 Android ID 转换为 Java 字段名称
            val fieldName = DataBindingUtil.convertAndroidIdToJavaFieldName(second.resourceReference.name)
            // 在轻量级类中查找与字段名称匹配的字段
            val psiReferences = lightClasses.mapNotNull { it.allFields.find { field -> field.name == fieldName } }
                // 对每个字段执行搜索操作，获取所有引用
                .map { ReferencesSearch.search(it, scope).findAll() }
                .flatten()
            return Pair(first, psiReferences)
        }

    private fun renameResId(project: Project, element: XmlAttributeValue, list: List<PsiReference>, newName: String) {
        ApplicationManager.getApplication().invokeAndWait {
            element.rename(newName, "XmlId")
        }
        if (list.isNotEmpty()) {
            val newRefName = DataBindingUtil.convertAndroidIdToJavaFieldName(newName)
            println(String.format("[IdViewBinding] >>> %s", newRefName))
            WriteCommandAction.writeCommandAction(project).withName(PLUGIN_NAME).run<RuntimeException> {
                list.forEach { ref -> ref.handleElementRename(newRefName) }
            }
        }
    }

}
