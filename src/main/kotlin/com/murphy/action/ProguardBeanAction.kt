package com.murphy.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.murphy.core.BeanGenerator
import com.murphy.core.childrenDfsSequence
import com.murphy.core.computeTime
import com.murphy.core.dumbReadAction
import com.murphy.util.LogUtil
import com.murphy.util.PLUGIN_NAME
import com.murphy.util.notifyError
import com.murphy.util.notifyInfo


/**
 * 修改马甲包对应的属性接口
 * 接口变量名和api名
 * url 请求头 请求参数 响应参数
 * 2f160d1f-6e09-442a-85af-59a73912e7aa.json
 * 前面是原项目对应的，后面是当前使用的。将项目中前面的命名替换为json中的后面的。
 *
 * {"pX7jTczYizh":"ya3cwgjMqYzjuN07k","/acXA/joAbPS/cFuK9j1CRz":"/ux00Zx9Ha9g/f7sgJLrGd9ZW/p94Ly4fspu_/nzVfX8taSxX1s"}
 */
class ProguardBeanAction : AnAction() {

    override fun actionPerformed(action: AnActionEvent) {
        val myPsi = action.getData(PlatformDataKeys.PSI_ELEMENT) ?: return
        val myProject = action.project ?: return
        // 预处理准备时，选中一个当前需要匹配对应关系的json文件
        if (!BeanGenerator.prepare(myProject)) return
        val label = "JSON Mapping Interface"
        LogUtil.logRecord(myProject, label, false)
        val startTime = System.currentTimeMillis()
        ProgressManager.getInstance().run(object : Task.Modal(myProject, PLUGIN_NAME, false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0

                DumbService.getInstance(myProject).dumbReadAction {
                    myPsi.childrenDfsSequence().toList()
                }.run {
                    BeanGenerator.process(myProject, indicator, this)
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