package com.murphy.action

import com.ibm.icu.text.SimpleDateFormat
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.psi.PsiBinaryFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.xml.XmlFile
import com.murphy.config.AndProguardConfigState
import com.murphy.core.*
import com.murphy.util.PLUGIN_NAME
import com.murphy.util.notifyError
import com.murphy.util.notifyInfo
import com.murphy.util.notifyWarn
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

/**
 * Install from Plugin Marketplace
 * https://plugins.jetbrains.com/plugin/23840-andproguard
 *
 * Program Structure Interface 缩写为：PSI，主要负责解析文件，为平台众多功能提供语法和语义代码模型。
 * https://link.juejin.cn/?target=https%3A%2F%2Fplugins.jetbrains.com%2Fdocs%2Fintellij%2Fpsi.html
 *
 * The PsiElement class is the common base class for PSI elements.
 *
 * 【code implementation - 实现Action的具体代码逻辑】：决定了这个action在哪个context下有效，并且在UI中被选择后的功能（继承父类AnAction并重写actionPerformed()方法，用于Action被执行后的回调）。
 * 【registered - 在配置文件中注册】：决定了这个action在IDE界面的哪个位置出现（创建新的group或存放进现有的ActionGroup，以及在group中的位置）
 *
 *
 *  各类和文件夹，图片名
 */
class AndProguardAction : AnAction() {

    // 继承父类AnAction并重写actionPerformed()方法，用于Action被执行后的回调
    override fun actionPerformed(action: AnActionEvent) {
        /**
         * How do I get a PSI element?
         * AnActionEvent.getData(CommonDataKeys.PSI_ELEMENT)
         * The PsiElement class is the common base class for PSI elements.
         *
         *
         * A PSI (Program Structure Interface) file is the root of a structure representing a file's contents as a hierarchy of elements in a particular programming language.
         * PSI（程序结构接口）文件是一种结构的根，它将文件的内容表示为特定编程语言中的元素层次结构。
         *
         *
         */
        // PlatformDataKeys extends CommonDataKeys
        // psi的类型为PsiElement
        // 首先尝试从 action 中获取 PSI_ELEMENT 数据，如果获取到了，就根据 psi 的类型进行不同的处理。如果获取到的数据为 null，则直接提前返回，不再执行后续的逻辑。
        val psi = action.getData(PlatformDataKeys.PSI_ELEMENT) ?: return
        val dateStart = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> $dateStart [Refactor Start] <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
        val config = AndProguardConfigState.getInstance()

        // 初始化配置随机规则
        config.initRandomNode()
        // 这里使用 when 表达式对 psi 进行模式匹配，类似于 switch 语句，但更加强大
        when (psi) {
            // 当 psi 是 KtClassOrObject 类型时，执行 processKotlin(psi) 方法。单个执行文件
            is KtClassOrObject -> processKotlin(psi)
            is PsiClass -> processJava(psi)
            is PsiJavaFile -> processJava(psi)
            is KtFile -> processKotlin(psi)
            is XmlFile -> processXml(psi)
            is PsiBinaryFile -> psi.rename(config.randomLayoutResName, "File")
            //文件夹中的继续操作，当前事件是文件夹
            is PsiDirectory -> {
                config.initExcludePackage()
                // 除去所有需要忽略的 package name 的集合。待修改的
                val fileList = psi.fileList(config.excludeList)
                // 返回空列表，表示是需要忽略的，跳过
                if (fileList.isEmpty()) {
                    notifyWarn(action.project, "Nothing to do")
                    return
                }
                val startTime = System.currentTimeMillis()
                val size = fileList.size
                val total: Double = size.toDouble()
                var count = 0
                val resIdList: MutableList<String> = LinkedList()

                /**
                 *
                 * Progress小弹窗
                 * 使用 Progress Manager 来运行一个任务（Task），并在任务执行的不同阶段执行相应的操作。
                 *
                 * 在这里，通过 ProgressManager.getInstance().run() 方法运行一个模态（Modal）任务。
                 * 任务被实例化为一个匿名对象，继承自 Task.Modal 类。这个任务被设置为模态，意味着它会阻止用户与 IDE 进行交互，直到任务完成。
                 *
                 */
                ProgressManager.getInstance().run(object : Task.Modal(action.project, PLUGIN_NAME, false) {
                    override fun run(indicator: ProgressIndicator) {
                        // 设置了进度指示器（indicator）为非确定性进度（isIndeterminate = false），然后遍历文件列表并处理不同类型的文件
                        indicator.isIndeterminate = false
                        val iterator = fileList.iterator()
                        while (iterator.hasNext()) {

                            // 于在 UI 线程上执行某些操作
                            /**
                             * 在 IntelliJ 应用程序的 UI 线程上同步执行传入的代码块，确保这部分代码在 UI 线程上执行，以避免可能的线程安全问题
                             *
                             * ApplicationManager 是 IntelliJ 平台中用于管理应用程序状态和操作的类。
                             * getApplication() 方法用于获取当前应用程序实例
                             *
                             * invokeAndWait 是一个方法，用于在 UI 线程上执行传入的代码块，并阻塞当前线程，直到 UI 线程上的操作完成。
                             * 这是确保在 UI 线程上执行操作的一种方式，因为在大多数 GUI 应用程序中，UI 操作必须在 UI 线程上执行，以避免界面冲突和不确定的行为
                             */
                            ApplicationManager.getApplication().invokeAndWait {
                                //文件夹中的遍历执行
                                when (val next = iterator.next()) {
                                    is PsiJavaFile -> processJava(next)
                                    is KtFile -> processKotlin(next)
                                    is XmlFile -> processXml(next, resIdList)
                                    is PsiBinaryFile -> next.rename(config.randomLayoutResName, "File")
                                }
                            }

                            // 在处理文件的过程中更新进度指示器的分数和文本信息
                            indicator.fraction = ++count / total
                            indicator.text = "$count files of $size files"
                            iterator.remove()
                        }
                    }

                    override fun onSuccess() {
                        notifyInfo(action.project, "$size files refactor finished, take ${computeTime(startTime)}")
                    }

                    override fun onThrowable(error: Throwable) {
                        notifyError(action.project, "${error.message}")
                        error.printStackTrace()
                    }

                    override fun onFinished() {
                        resIdList.clear()
                    }
                })
            }
        }
        val dateEnd = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> $dateEnd [Refactor End] <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
    }

}