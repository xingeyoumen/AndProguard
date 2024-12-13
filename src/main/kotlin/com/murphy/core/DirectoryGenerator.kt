package com.murphy.core

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.jetbrains.kotlin.idea.core.getPackage

/**
 * 修改文件夹名
 */
object DirectoryGenerator : AbstractGenerator<PsiDirectory>() {
    override val name: String get() = "Directory"

    /**
     * 这段Kotlin代码定义了一个 process 方法，用于处理项目目录。具体功能如下：
     * 调用父类方法：首先调用父类的 process 方法。
     * 检查配置：如果 config.directoryRule 不为空，则继续执行后续操作。
     * 映射并重命名：对 data 列表中的每个 PsiDirectory 对象执行 getPackage 操作，并使用 renameEach 方法进行重命名，重命名类型为 PsiDirectory。
     *
     */
    override fun process(first: Project, second: ProgressIndicator, data: List<PsiDirectory>) {
        super.process(first, second, data)
        //PsiDirectory 的修改规则
        if (config.directoryRule.isNotEmpty()) {
            //对每个 PsiDirectory 对象执行 getPackage 操作，并使用 renameEach 方法进行重命名，重命名类型为 PsiDirectory
            data.mapNotNull { service.dumbReadAction { it.getPackage() } }
                .renameEach(RefactorType.PsiDirectory)
        }
    }
}