package com.murphy.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import com.intellij.psi.impl.getFieldOfGetter
import com.intellij.psi.impl.getFieldOfSetter
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.refactoring.RefactoringFactory
import com.murphy.util.LogUtil
import kotlinx.coroutines.Runnable
import java.util.concurrent.TimeUnit

/**
 * 这段Kotlin代码定义了一个扩展函数 childrenDfsSequence，用于遍历给定 PsiElement 的所有子元素，并以深度优先搜索（DFS）的方式生成一个序列。
 * 函数定义：fun PsiElement.childrenDfsSequence(): Sequence<PsiElement> 定义了一个扩展函数，接收一个 PsiElement 对象并返回一个 Sequence<PsiElement> 序列。
 * 序列生成：sequence { ... } 创建一个序列生成器，允许在其中定义异步操作。
 * 递归访问：suspend fun SequenceScope<PsiElement>.visit(element: PsiElement) 是一个递归函数，用于深度优先遍历 PsiElement 的所有子元素。
 * 遍历子元素：element.children.forEach { visit(it) } 遍历当前元素的所有子元素，并对每个子元素递归调用 visit 函数。
 * 生成元素：yield(element) 将当前元素添加到生成的序列中。
 * 初始调用：visit(this@childrenDfsSequence) 从当前 PsiElement 开始进行深度优先遍历。
 */
fun PsiElement.childrenDfsSequence(): Sequence<PsiElement> =
    sequence {
        suspend fun SequenceScope<PsiElement>.visit(element: PsiElement) {
            element.children.forEach { visit(it) }
            yield(element)
        }
        visit(this@childrenDfsSequence)
    }

/**
 * 这段 Kotlin 代码定义了一个名为 `dumbReadAction` 的内联函数，用于在 `DumbService` 中执行一个“愚蠢”读取操作。让我解释这段代码的含义：
 *
 * - `inline fun <reified T> DumbService.dumbReadAction(r: Computable<T>): T`：这里定义了一个内联函数 `dumbReadAction`，它接受一个类型为 `Computable<T>` 的参数 `r`，并且返回类型为 `T`。
 *   - `<reified T>`：使用 `reified` 关键字表示在运行时可以获取泛型类型 `T` 的信息。
 *   - `DumbService`：函数的接收者类型是 `DumbService`，这意味着这个函数作为 `DumbService` 的扩展函数。
 *   - `r: Computable<T>`：这个参数是一个 `Computable<T>` 类型的函数，用于执行计算并返回类型 `T` 的结果。
 *
 * - `runReadActionInSmartMode`：这个函数看起来是一个内部函数或者是一个扩展函数，它可能是 `DumbService` 的一部分，用于在“智能模式”下执行读取操作。
 *
 * - `{ r.compute() }`：在 `runReadActionInSmartMode` 函数中直接调用了参数 `r` 的 `compute` 方法，这是 `Computable<T>` 接口的方法，用于执行计算并返回结果。
 *
 * 综合来看，这段代码的目的是定义一个内联函数 `dumbReadAction`，用于在 `DumbService` 中执行一个“愚蠢”读取操作。它接受一个计算函数 `r`，然后在一个特定的执行模式下调用这个计算函数，并返回计算结果。这种设计可以帮助在插件开发中简化对 `DumbService` 的操作，并在后台线程中执行一些读取操作，以避免阻塞用户界面。
 */
inline fun <reified T> DumbService.dumbReadAction(r: Computable<T>): T {
    return runReadActionInSmartMode<T> { r.compute() }
}

/**
 * normal rename
 */
fun PsiNamedElement.rename(
    newName: String,
    desc: String,
    myProject: Project,
    dumbService: DumbService = DumbService.getInstance(myProject)
) {
    val pair = dumbService.dumbReadAction { Pair(!isValid, name) }
    //区别点
    if (pair.first || pair.second == null) return
    LogUtil.info(myProject, String.format("[$desc] %s >>> %s", pair.second, newName))
    rename(newName, myProject, dumbService)
}


/**
 * xml rename
 */
fun XmlAttributeValue.renameX(
    newName: String,
    desc: String,
    myProject: Project,
    dumbService: DumbService = DumbService.getInstance(myProject)
) {
    val pair = dumbService.dumbReadAction { Pair(!isValid, value) }
    //区别点
    if (pair.first) return
    LogUtil.info(myProject, String.format("[$desc] %s >>> %s", pair.second, newName))
    rename(newName, myProject, dumbService)
}

/**
 * rename方法 具体调用
 *
 *
 * 这段 Kotlin 代码定义了一个名为 `rename` 的私有函数，用于重命名 `PsiElement` 对象，其中包括了新的名称、项目对象和 `DumbService` 服务。让我解释这段代码的主要功能：
 *
 * - `private fun PsiElement.rename(newName: String, myProject: Project, service: DumbService)`：这是一个扩展函数，它接受一个新名称 `newName`，项目对象 `myProject` 和 `DumbService` 服务对象 `service` 作为参数。
 *
 * - `val runnable = Runnable { ... }`：在这里创建了一个 `Runnable` 实例，用于执行重命名操作。在 `run` 方法中，使用 `RefactoringFactory` 创建一个重命名操作，并运行它。
 *
 * - `ApplicationManager.getApplication().invokeAndWait { ... }`：在主线程中调用代码块，确保在主线程中执行其中的操作。
 *
 *   - 如果 `service` 是 "愚蠢"（Dumb）状态，即正在进行索引或其他耗时操作，会调用 `runWhenSmart` 方法，以确保在智能（Smart）模式下执行所需的操作。在这种情况下，会调用 `service.smartInvokeLater(runnable)`，在智能模式下延迟执行 `runnable`。
 *
 *   - 如果 `service` 不是 "愚蠢" 状态，即处于智能状态，会直接运行 `runnable`，即立即执行重命名操作。
 *
 * 综合来看，这段代码的作用是在主线程中执行重命名操作。根据 `DumbService` 的状态，它会在智能模式下延迟执行或立即执行重命名操作。这种设计可以确保在进行耗时操作时不会阻塞用户界面，同时在适当的时机执行重命名操作。
 *
 */
private fun PsiElement.rename(newName: String, myProject: Project, service: DumbService) {
    val runnable = Runnable {
        RefactoringFactory.getInstance(myProject)
            .createRename(this, newName, false, false)
            .run()
    }
    ApplicationManager.getApplication().invokeAndWait {
        if (service.isDumb) {
            service.runWhenSmart { service.smartInvokeLater(runnable) }
        } else {
            runnable.run()
        }
    }
}


/**
 * 忽略修改方法名
 */
private val PsiMethod.isSetter: Boolean
    get() = name.startsWith("set")
val PsiMethod.isGetterOrSetter
    get() = name.run { startsWith("set") || startsWith("get") || startsWith("is") }
val PsiMethod.fieldOfGetterOrSetter
    get() = if (isSetter) getFieldOfSetter(this) else getFieldOfGetter(this)


/**
 * 修改消耗时间
 * In summary, the computeTime function takes a start time as a parameter, calculates the time difference between the current time and the start time, and formats the result as a string in the format of "days hours minutes seconds"
 */
fun computeTime(startTime: Long): String {
    val time = System.currentTimeMillis() - startTime
    val days = TimeUnit.MILLISECONDS.toDays(time)
    val toHours = TimeUnit.MILLISECONDS.toHours(time)
    val toMinutes = TimeUnit.MILLISECONDS.toMinutes(time)
    val toSeconds = TimeUnit.MILLISECONDS.toSeconds(time)
    val hours = toHours - TimeUnit.DAYS.toHours(days)
    val minutes = toMinutes - TimeUnit.HOURS.toMinutes(toHours)
    val seconds = toSeconds - TimeUnit.MINUTES.toSeconds(toMinutes)
    val strBuilder = StringBuilder()
    if (days > 0) strBuilder.append(days).append(" d ")
    if (hours > 0) strBuilder.append(hours).append(" h ")
    if (minutes > 0) strBuilder.append(minutes).append(" m ")
    if (seconds >= 0) strBuilder.append(seconds).append(" s ")
    return strBuilder.toString()
}