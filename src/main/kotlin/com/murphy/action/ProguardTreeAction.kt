package com.murphy.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiNamedElement
import com.murphy.core.*
import com.murphy.util.LogUtil
import com.murphy.util.PLUGIN_NAME
import com.murphy.util.notifyError
import com.murphy.util.notifyInfo

class ProguardTreeAction : AnAction() {

    override fun actionPerformed(action: AnActionEvent) {
        val myPsi = action.getData(PlatformDataKeys.PSI_ELEMENT) ?: return
        val myProject = action.project ?: return
        val label = "Obfuscate Tree"
        LogUtil.logRecord(myProject, label, false)
        val startTime = System.currentTimeMillis()

        // 生成器 全部修改，这个方法有点鸡肋。不太还用，可以之前的配合着使用
        val generators = arrayOf(
            KotlinPreGenerator, JavaPreGenerator,
            KotlinGenerator, JavaGenerator,
            ResourceGenerator, FileGenerator
        )
        ProgressManager.getInstance().run(object : Task.Modal(myProject, PLUGIN_NAME, false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0

                DumbService.getInstance(myProject).dumbReadAction {
                    myPsi.childrenDfsSequence().filterIsInstance<PsiNamedElement>().toList()
                }.run {
                    generators.forEach { it.process(myProject, indicator, this) }
                }
            }

            override fun onSuccess() {
                notifyInfo(myProject, "refactor finished, take ${computeTime(startTime)}")
            }

            override fun onThrowable(error: Throwable) {
                notifyError(myProject, "${error.message}")
                error.printStackTrace()
            }
        })
        LogUtil.logRecord(myProject, label, true)
    }

}