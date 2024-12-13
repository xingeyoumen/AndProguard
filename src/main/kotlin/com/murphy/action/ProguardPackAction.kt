package com.murphy.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiDirectory
import com.murphy.core.*
import com.murphy.util.LogUtil
import com.murphy.util.PLUGIN_NAME
import com.murphy.util.notifyError
import com.murphy.util.notifyInfo

/**
 * 单纯修改文件夹名
 *
 * 当前文件夹名和递归此文件夹下的所有文件夹名
 */
class ProguardPackAction : AnAction() {

    override fun actionPerformed(action: AnActionEvent) {
        val myPsi = action.getData(PlatformDataKeys.PSI_ELEMENT) ?: return
        val myProject = action.project ?: return
        val label = "Refactor Packages"
        LogUtil.logRecord(myProject, label, false)
        val startTime = System.currentTimeMillis()

        /**
         * 这段 Kotlin 代码展示了使用 `ProgressManager` 来运行一个任务（Task），并定义了该任务的行为。在这个例子中，任务是以模态方式运行的，即阻止用户交互，直到任务完成。让我解释一下这段代码的主要内容：
         *
         * 1. `ProgressManager.getInstance().run(...)`：这里使用 `ProgressManager` 获取其实例，并调用 `run` 方法来执行一个任务。
         *
         * 2. `object : Task.Modal(myProject, PLUGIN_NAME, false) {...}`：这是创建一个匿名内部类来继承 `Task.Modal` 类，定义了任务的行为。
         *
         *    - `myProject` 是项目对象。
         *    - `PLUGIN_NAME` 是插件的名称。
         *    - `false` 表示该任务不是后台任务，而是阻塞用户界面的模态任务。
         *
         * 3. `override fun run(indicator: ProgressIndicator) {...}`：在 `run` 方法中定义了任务的具体操作。在这里，任务设置了进度指示器的一些属性，然后使用 `DumbService` 在后台执行一个读取操作，并调用 `DirectoryGenerator.process` 处理结果。
         *
         * 4. `override fun onSuccess() {...}`：在任务成功完成时调用 `onSuccess` 方法，这里发送一个通知，通知用户重构已完成，并显示重构所需的时间。
         *
         * 5. `override fun onThrowable(error: Throwable) {...}`：如果任务遇到异常，则调用 `onThrowable` 方法，这里发送一个错误通知，并打印异常信息。
         *
         * 这段代码的主要目的是在一个模态任务中执行特定的操作（例如处理目录），并在任务成功或出现异常时发送通知给用户，以便及时通知用户任务的状态。
         */
        ProgressManager.getInstance().run(object : Task.Modal(myProject, PLUGIN_NAME, false) {
            override fun run(indicator: ProgressIndicator) {
                //Marks the progress indeterminate (for processes that can't estimate the amount of work to be done) or determinate (for processes that can display the fraction of the work done using setFraction(double)
                indicator.isIndeterminate = false
                //设置分数:介于0.0和1.0之间的数字，反映已经完成的工作量比例(0.0表示没有完成，1.0表示全部完成)。仅适用于确定指标。分数应为用户提供剩余时间的粗略估计，如果无法做到这一点，请考虑使进度不确定。
                indicator.fraction = 0.0

                // 这里使用 DumbService 在后台执行一个读取操作，然后调用 DirectoryGenerator.process 处理结果

                /**
                 * 这段代码展示了在 IntelliJ 平台插件开发中的一种常见模式。让我解释这段代码的含义：
                 *
                 * 1. `DumbService.getInstance(myProject)`：获取项目的 `DumbService` 实例。`DumbService` 用于执行“愚蠢”操作，这些操作通常在后台线程中运行，以避免阻塞主线程。在 IntelliJ 平台中，有时会禁用某些功能，直到项目的索引（或其他任务）完成。`DumbService` 允许在这种情况下执行代码。
                 *
                 * 2. `.dumbReadAction { ... }`：在 `DumbService` 的 `dumbReadAction` 方法中执行一段代码块。这表明在“愚蠢”模式下执行一些读取操作，这些操作可以是耗时的，因此应该在后台线程中运行。
                 *
                 * 3. `myPsi.childrenDfsSequence().filterIsInstance<PsiDirectory>().toList()`：这部分代码对 `myPsi`（可能是一个 `PsiElement`）进行操作，获取其所有子元素中的 `PsiDirectory` 对象，并将它们转换为列表。
                 *
                 * 4. `.run { ... }`：对上一步操作的结果执行一个 lambda 表达式。
                 *
                 * 5. `DirectoryGenerator.process(myProject, indicator, this)`：在这个 lambda 表达式中，调用 `DirectoryGenerator` 的 `process` 方法，传递了项目对象 `myProject`、进度指示器 `indicator` 和上一步获得的 `PsiDirectory` 列表。
                 *
                 * 综合起来，这段代码的目的是在“愚蠢”模式下执行一些读取操作，从 `myPsi` 中获取所有子目录，然后使用 `DirectoryGenerator` 处理这些子目录。这种模式通常用于在后台执行耗时操作，以确保不会阻塞 IntelliJ 平台的用户界面响应。
                 */
                DumbService.getInstance(myProject).dumbReadAction {
                    //`myPsi`（可能是一个 `PsiElement`）进行操作，获取其所有子元素中的 `PsiDirectory` 对象，并将它们转换为列表。
                    myPsi.childrenDfsSequence().filterIsInstance<PsiDirectory>().toList()
                }.run {
                    //this 表示上一步操作的结果，即 `PsiDirectory` 列表。在这个 lambda 表达式中，调用 `DirectoryGenerator` 的 `process` 方法，传递了项目对象 `myProject`、进度指示器 `indicator` 和上一步获得的 `PsiDirectory` 列表。
                    DirectoryGenerator.process(myProject, indicator, this)
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