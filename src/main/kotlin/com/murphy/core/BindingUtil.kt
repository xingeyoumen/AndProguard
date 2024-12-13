package com.murphy.core

import com.android.tools.idea.databinding.module.LayoutBindingModuleCache
import com.android.tools.idea.databinding.util.DataBindingUtil
import com.android.tools.idea.res.psi.ResourceReferencePsiElement
import com.android.tools.idea.util.androidFacet
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.murphy.util.LogUtil
import com.murphy.util.PLUGIN_NAME

fun ResourceReferencePsiElement.findIdReference(scope: SearchScope): List<PsiReference>? {
    val bindingModuleCache = LayoutBindingModuleCache.getInstance(delegate.androidFacet ?: return null)
    val groups = bindingModuleCache.bindingLayoutGroups
    val fieldName = DataBindingUtil.convertAndroidIdToJavaFieldName(resourceReference.name)
    return groups.flatMap { bindingModuleCache.getLightBindingClasses(it) }
        .mapNotNull { it -> it.allFields.find { field -> field.name == fieldName } }
        .map { ReferencesSearch.search(it, scope).findAll() }
        .flatten()
}

/**
 * 这段Kotlin代码定义了一个扩展函数 findLayoutReference，用于在给定的搜索范围内查找与布局资源相关的引用。具体功能如下：
 * 获取 LayoutBindingModuleCache 实例。
 * 检查是否有有效的 androidFacet，如果没有则返回 null。
 * 获取所有绑定布局组。
 * 将资源名称转换为Java类名，并查找匹配的布局组。
 * 如果找到匹配的布局组，则获取其轻量级绑定类。
 * 对每个轻量级绑定类进行引用搜索，并返回所有找到的引用。
 */
fun ResourceReferencePsiElement.findLayoutReference(scope: SearchScope): List<PsiReference>? {
    val bindingModuleCache = LayoutBindingModuleCache.getInstance(delegate.androidFacet ?: return null)
    val groups = bindingModuleCache.bindingLayoutGroups
    val className = DataBindingUtil.convertFileNameToJavaClassName(resourceReference.name) + "Binding"
    val layoutGroup = groups.firstOrNull { it.mainLayout.className == className }
    return bindingModuleCache.getLightBindingClasses(layoutGroup ?: return null)
        .map { ReferencesSearch.search(it, scope).findAll() }
        .flatten()
}

fun List<PsiReference>.handleReferenceRename(project: Project, newRefName: String) {
    WriteCommandAction.writeCommandAction(project)
        .withName(PLUGIN_NAME)
        .run<Throwable> {
            forEach { it.handleElementRename(newRefName) }
        }
}

//layout view ID 自适应是否是viewBinding
fun ResourceReferencePsiElement.renameId(
    newName: String,
    project: Project,
    service: DumbService,
    scope: SearchScope
) {
    val originalPsi = delegate
    if (originalPsi !is XmlAttributeValue) return
    val psiReferences = service.dumbReadAction { findIdReference(scope) }?.takeIf { it.isNotEmpty() }
    originalPsi.renameX(newName, "IdAttribute", project, service)
    psiReferences?.run {
        val newRefName = DataBindingUtil.convertAndroidIdToJavaFieldName(newName)
        LogUtil.info(project, String.format("[IdBinding] >>> %s", newRefName))
        handleReferenceRename(project, newRefName)
    }
}

/**
 * ResourceReferencePsiElement
 * ResourceType.ID is ResourceReferencePsiElement
 */
fun ResourceReferencePsiElement.renameId(newName: String, project: Project) =
    renameId(newName, project, DumbService.getInstance(project), GlobalSearchScope.projectScope(project))


//修改Layout xml文件名称，DataBindingUtil 修改方法
fun ResourceReferencePsiElement.renameLayout(
    newName: String,
    project: Project,
    service: DumbService,
    scope: SearchScope
) {
    val originalPsi = delegate
    if (originalPsi !is XmlFile) return
    val psiReferences = service.dumbReadAction { findLayoutReference(scope) }?.takeIf { it.isNotEmpty() }
    originalPsi.rename(newName, "Layout", project, service)
    psiReferences?.run {
        val newRefName = DataBindingUtil.convertFileNameToJavaClassName(newName) + "Binding"
        LogUtil.info(project, String.format("[LayoutBinding] >>> %s", newRefName))
        handleReferenceRename(project, newRefName)
    }
}

/**
 * XmlFile
 * ResourceType.LAYOUT is XmlFile
 *
 */

fun ResourceReferencePsiElement.renameLayout(newName: String, project: Project) =
    renameLayout(newName, project, DumbService.getInstance(project), GlobalSearchScope.projectScope(project))