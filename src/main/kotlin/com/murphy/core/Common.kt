package com.murphy.core

import com.intellij.psi.*
import com.intellij.psi.impl.getFieldOfGetter
import com.intellij.psi.impl.getFieldOfSetter
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.refactoring.RefactoringFactory
import com.murphy.config.AndProguardConfigState
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.search.isImportUsage
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import java.util.concurrent.TimeUnit

/**
 *
 * 通用修改执行类
 */


/**
 * 根据给定的排除列表来跳过特定的文件夹。
 * 对路径地址进行判断，是否存在于当前忽略给定的列表中，
 *
 * 用于判断当前文件夹是否需要跳过（即是否存在于给定的路径列表中）
 *
 * 这个方法用于判断当前文件夹是否需要跳过，根据传入的路径列表 list 进行判断。
 * 它首先将当前文件夹的路径转换为点分隔的字符串形式。
 * 然后使用 list.firstOrNull { path.contains(it) } 来检查路径中是否包含列表中的任何一个字符串。
 * 如果存在匹配的字符串则返回 true，表示需要跳过；否则返回 false，表示不需要跳过。
 *
 *
 * list 配置需忽略的路径的总和列表。判断当前文件夹是否存在于给定的路径列表中
 *
 * true 表示当前文件夹的路径中包含了列表中的某个字符串
 */
fun PsiDirectory.skip(list: List<String>): Boolean {
    // virtualFile 是 PsiDirectory 中代表该目录的虚拟文件。
    // 这行代码获取了该虚拟文件的路径，并用点号替换斜杠，将路径转换为一个点分隔的字符串
    val path = virtualFile.path.replace('/', '.')
    // 这行代码使用 Kotlin 的函数式编程方式进行处理。它会尝试在给定的路径列表 list 中找到第一个满足条件 path.contains(it) 的元素。
    // 如果存在这样的元素，则 firstOrNull 返回该元素，否则返回 null。
    // 最终表达式判断 firstOrNull 的结果是否不为 null，如果不为 null，则表示当前文件夹的路径中包含了列表中的某个字符串，返回 true
    return list.firstOrNull { path.contains(it) } != null
}


/**
 * 用于处理文件夹（PsiDirectory）的扩展函数，主要用于在文件夹结构中生成一个文件列表，同时根据给定的排除列表来跳过特定的文件夹。
 * 当前传入的遍历的文件夹，
 *
 * exclude 配置需忽略的路径的总和列表
 */
fun PsiDirectory.fileList(exclude: List<String>): MutableList<PsiFile> {
    val list: MutableList<PsiFile> = ArrayList()
    // 如果当前文件夹需要被跳过（通过 skip(exclude) 函数判断），则直接返回空列表。
    // true 表示当前文件夹的路径中包含了列表中的某个字符串。忽略的文件夹路径返空列表
    if (skip(exclude)) return list
    // 此目录的子目录列表。对当前文件夹的所有子文件夹，递归调用 fileList(exclude) 方法，将结果添加到 list 中。
    subdirectories.forEach { list.addAll(it.fileList(exclude)) }
    // 将当前文件夹中的文件添加到 list 中
    list.addAll(files)
    return list
}

/**
 *
 * 这段代码实现了一个深度优先遍历函数，该函数以当前 PsiElement 对象为根节点，遍历其所有子元素，并将它们添加到一个序列中返回。
 * 这种序列可以用于对 PSI 树进行深度优先遍历，以便在插件开发中对代码结构进行分析和处理
 *
 * 这是一个 Kotlin 扩展方法，它为 PsiElement 类添加了一个名为 childrenDfsSequence() 的方法。
 * 方法返回一个序列（Sequence），其中包含 PsiElement 类型的元素。
 */
fun PsiElement.childrenDfsSequence(): Sequence<PsiElement> =
    // 这是 Kotlin 中的一个标准库函数，用于创建一个序列
    sequence {
        // 这是一个内部定义的挂起函数，用于在序列中进行深度优先遍历。
        // SequenceScope 是用于生成序列元素的接口。
        suspend fun SequenceScope<PsiElement>.visit(element: PsiElement) {
            element.children.forEach { visit(it) }
            // yield 是一个挂起函数，用于将当前元素添加到序列中
            yield(element)
        }
        // 这里调用了 visit() 函数，开始对当前元素及其子元素进行深度优先遍历
        visit(this@childrenDfsSequence)
    }


/**
 * 1、获取 RefactoringFactory 实例：通过 RefactoringFactory.getInstance(project) 方法获得一个 RefactoringFactory 实例，用于创建重构操作。
 * 2、创建重命名操作：使用 createRename(this, newName, false, false) 方法创建一个重命名操作。这里的参数含义如下：
 * this：表示要执行重命名操作的目标元素。
 * newName：表示要重命名成的新名称。
 * false：第一个 false 参数表示是否进行全局搜索以解决冲突（一般不需要）。
 * false：第二个 false 参数表示是否进行自动重构（一般情况下也不需要）。
 * 3、执行重命名操作：调用 run() 方法来立即执行创建的重命名操作。这将触发 IntelliJ IDEA 执行重命名操作，更新所有引用和使用了被重命名元素的地方。
 *
 *
 * Class File XmlFile Field Reference Method Function Property Parameter Variable
 */
fun PsiNamedElement.rename(newName: String, desc: String) {
    println(String.format("[$desc] %s >>> %s", name, newName))
    val project = project
    RefactoringFactory.getInstance(project)
        .createRename(this, newName, false, false)
        .run()
}


/**
 *
 * 修改执行操作
 *
 *
 * Resource XmlId ResId
 */
fun XmlAttributeValue.rename(newName: String, desc: String) {
    println(String.format("[$desc] %s >>> %s", value, newName))
    val project = project
    RefactoringFactory.getInstance(project)
        .createRename(this, newName, false, false)
        .run()
}

/**
 * 这个方法的名字是否是以set开头的，如果是则返回 true
 */
fun PsiMethod.isSetter() = name.startsWith("set")

/**
 * 这个方法的名字是否是以set get is 开头的。
 * 用于判断给定的 PsiMethod 是否为 getter 或 setter 方法。
 * 如果方法名称以其中任何一个开头，则返回 true。
 * run { ... } 是 Kotlin 标准库中的一个扩展函数，它会将调用者对象（这里是 name）作为 lambda 表达式的接收者，
 * 从而可以在 lambda 中直接操作调用者对象的属性和方法
 */
fun PsiMethod.isGetterOrSetter() = name.run { startsWith("set") || startsWith("get") || startsWith("is") }

/**
 * 判断方法类型并相应地获取相应的字段。
 * 是否为空判断操作
 *
 */
fun PsiMethod.getFieldOfGetterOrSetter() = if (isSetter()) getFieldOfSetter(this) else getFieldOfGetter(this)


/**
 * 用于在给定的 PsiNamedElement 上执行重命名操作
 *
 * 这个函数的主要功能是在当前 PsiNamedElement 上执行引用的重命名操作。它首先搜索所有引用该元素的地方，然后对这些引用进行处理，
 * 根据引用的类型进行不同的重命名操作，最终使用配置中的随机字段名和 "Reference" 标识来重命名这些引用
 */
fun PsiNamedElement.renameReference() {
    // 这行代码执行一个搜索操作，查找所有引用了当前 PsiNamedElement 的地方，并返回一个包含这些引用的列表
    ReferencesSearch.search(this, GlobalSearchScope.projectScope(project)).findAll()
        // 对搜索到的引用列表进行映射操作，通过调用 getNamedElement(name) 函数获取每个引用对应的 PsiNamedElement，并过滤掉为 null 的结果
        .mapNotNull { it.getNamedElement(name) }
        // 对获取到的 PsiNamedElement 列表进行去重操作，然后根据是否为 PsiField 类型的元素进行分区操作
        .distinct().partition { it is PsiField }
        // 对分区后的元素进行处理
        .run {
            val config = AndProguardConfigState.getInstance()
            // 执行重命名操作，使用配置中的随机字段名和 "Reference" 标识
            second.forEach { it.rename(config.randomFieldName, "Reference") }
            first.forEach { it.rename(config.randomFieldName, "Reference") }
        }
}

/**
 * 这段 Kotlin 代码定义了一个私有函数 getNamedElement(other: String?): PsiNamedElement?，
 * 该函数接受一个可选的字符串参数 other 并返回一个 PsiNamedElement 对象或者 null
 *
 * 从给定的 PsiReference 对象中获取符合特定条件的 PsiNamedElement
 */
private fun PsiReference.getNamedElement(other: String?): PsiNamedElement? {
    // 如果当前引用是用于导入（import）或者 other 参数为 null，则直接返回 null，表示没有找到符合条件的 PsiNamedElement
    if (isImportUsage() || other == null) return null
    // 通过 Kotlin 的 let 函数对 element 进行操作，并返回操作结果
    return element.let {
        // 检查 element 是否是 PsiJavaCodeReferenceElement 或 KtNameReferenceExpression 类型的实例
        if (it is PsiJavaCodeReferenceElement || it is KtNameReferenceExpression) {
            // 如果 element 的 namedUnwrappedElement 不为 null，则执行 run 作用域函数
            it.namedUnwrappedElement?.run {
                // 检查 namedUnwrappedElement 是否是 PsiVariable 或 KtCallableDeclaration 类型的实例
                if (this is PsiVariable || this is KtCallableDeclaration) {
                    // 如果元素的名称包含 other 字符串（不区分大小写），则返回该元素，否则返回 null
                    if (name?.contains(other, true) == true) this
                    else null
                } else null
            }
        } else null
    }
}

/**
 * 传入开始时间，计算耗时总时间。计算从 startTime 到当前时间经过的总毫秒数
 * 将总毫秒数转换为天数、小时数、分钟数和秒数，并构建一个可读的时间字符串
 */
fun computeTime(startTime: Long): String {
    val time = System.currentTimeMillis() - startTime
    // 天数，总小时，总分钟，总秒数
    val days = TimeUnit.MILLISECONDS.toDays(time)
    val toHours = TimeUnit.MILLISECONDS.toHours(time)
    val toMinutes = TimeUnit.MILLISECONDS.toMinutes(time)
    val toSeconds = TimeUnit.MILLISECONDS.toSeconds(time)
    // 剩余不足的时间计算降级
    val hours = toHours - TimeUnit.DAYS.toHours(days)
    val minutes = toMinutes - TimeUnit.HOURS.toMinutes(toHours)
    val seconds = toSeconds - TimeUnit.MINUTES.toSeconds(toMinutes)
    // 时间字符串拼接
    val strBuilder = StringBuilder()
    if (days > 0) strBuilder.append(days).append(" d ")
    if (hours > 0) strBuilder.append(hours).append(" h ")
    if (minutes > 0) strBuilder.append(minutes).append(" m ")
    if (seconds >= 0) strBuilder.append(seconds).append(" s ")
    return strBuilder.toString()
}