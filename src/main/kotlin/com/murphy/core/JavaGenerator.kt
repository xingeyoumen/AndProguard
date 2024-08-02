package com.murphy.core

import com.intellij.psi.PsiField
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.source.*
import com.intellij.psi.impl.source.tree.java.PsiLocalVariableImpl
import com.murphy.config.AndProguardConfigState
import org.jetbrains.kotlin.j2k.isMainMethod
/**
 * PSI 是 IntelliJ IDEA 中的一个关键概念，它表示了代码的抽象语法树（Abstract Syntax Tree，AST），允许插件分析、操作和修改代码
 * PsiNamedElement 接口扩展自 PsiElement 接口，它添加了方法来获取和设置元素的名称。这个接口通常用于表示具有名称的代码元素，比如类、方法、变量等。
 *
 * 在 IntelliJ IDEA 插件开发中，PsiNamedElement 可能会用于以下情况：
 *
 * 表示具有名称的代码元素，允许插件访问和操作这些元素的名称。
 * 支持代码导航和查找功能，通过元素的名称快速定位到特定的代码位置。
 * 在代码编辑器中显示有关代码元素名称的信息。
 * 通过实现 PsiNamedElement 接口，你可以为自定义的 PSI 元素提供名称相关的功能，使得插件可以更方便地处理具有名称的代码元素。
 *
 * 总的来说，PsiNamedElement 是表示具有名称的 PSI 元素的接口，用于在 IntelliJ IDEA 插件开发中处理代码中具有名称的元素，提供访问和操作名称的方法。
 *
 * PsiElement PsiNamedElement PsiClassImpl，进入传递
 *
 * 这段代码看起来是对给定的 PsiNamedElement 及其子元素进行某种重命名处理的逻辑，根据元素的类型和一些配置信息来执行不同的操作。
 * 这可能是插件中的一个功能，用于对 Java 代码进行一些自动化操作，例如重命名类、方法、字段等
 *
 * PsiClass PsiJavaFile
 */
fun processJava(psi: PsiNamedElement) {
    println("============================== ${psi.name} ==============================")
    // 用于获取一些配置信息
    val config = AndProguardConfigState.getInstance()
    val skipElements: MutableSet<PsiField> = HashSet()
    // 这行代码生成了一个深度优先遍历的元素序列，然后过滤出其中的 PsiNamedElement 类型的元素
    psi.childrenDfsSequence().filterIsInstance<PsiMethodImpl>().forEach {
        val skip = config.skipData && it.isGetterOrSetter()
        if (skip) it.getFieldOfGetterOrSetter()?.let { e -> skipElements.add(e) }
    }
    psi.childrenDfsSequence().filterIsInstance<PsiNamedElement>().toList().reversed().forEach {
        if (it.isValid.not()) return@forEach
        when (it) {
            is PsiAnonymousClassImpl -> return@forEach
            is PsiClassImpl -> {
                // 执行修改引用的操作
                it.renameReference()
                // 具体修改
                it.rename(config.randomClassName, "Class")
            }

            is PsiMethodImpl -> {
                // 配置了跳过，并且是getter 或 setter 方法。忽略的
                val skip = config.skipData && it.isGetterOrSetter()
                // 非忽略，非此几种类型的，执行方法名字的。父类中有重写的方法，构造函数，入口方法（main 方法）
                if (!skip && it.findSuperMethods().isEmpty() && !it.isConstructor && !it.isMainMethod())
                    // 修改方法名字
                    it.rename(config.randomMethodName, "Method")
            }

            // 用于表示参数的类之一
            is PsiParameterImpl -> it.rename(config.randomFieldName, "Parameter")
            is PsiLocalVariableImpl -> it.rename(config.randomFieldName, "Variable")
            is PsiFieldImpl -> if (!skipElements.contains(it)) it.rename(config.randomFieldName, "Field")

            /**
             * 在 Kotlin 的集合操作中，subtract 是一个用于从集合中去除另一个集合中包含的元素的函数。
             * 具体来说，subtract 函数返回一个新的集合，其中包含原始集合中存在但不在给定集合中的元素。
             * 大集合除掉了一些小集合，对新集合进行操作。方法名字中忽略跳过一部分方法。
             *
             * val list1 = listOf(1, 2, 3, 4, 5)
             * val list2 = listOf(2, 4)
             * val result = list1.subtract(list2)
             * println(result) // 输出 [1, 3, 5]
             */

        }
    }
}