package com.murphy.core

import com.intellij.psi.PsiNamedElement
import com.murphy.config.AndProguardConfigState
import com.murphy.util.KOTLIN_SUFFIX
import org.jetbrains.kotlin.idea.isMainFunction
import org.jetbrains.kotlin.idea.util.isAnonymousFunction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
/**
 *
 * 这个函数的主要作用是处理给定的 PsiNamedElement 对象及其子元素，根据它们的类型和一些条件进行不同的重命名操作，
 * 涉及到 Kotlin 文件、类、函数、属性以及参数的重命名
 *
 *
 *
 * KtClassOrObject KtFile
 */
fun processKotlin(psi: PsiNamedElement) {
    println("============================== ${psi.name} ==============================")
    // 用于获取一些配置信息
    val config = AndProguardConfigState.getInstance()
    val skipElements: MutableSet<KtParameter> = HashSet()
    psi.childrenDfsSequence().filterIsInstance<KtClass>().forEach {
        if (config.skipData && it.isData())
            skipElements.addAll(it.primaryConstructorParameters)
    }
    // 生成一个深度优先遍历的元素序列，并过滤出其中的 PsiNamedElement 类型的元素。
    psi.childrenDfsSequence().filterIsInstance<PsiNamedElement>().toList().reversed().forEach {
        if (it.isValid.not()) return@forEach
        when (it) {
            is KtFile -> {
                if (it.classes.size != 1 || it.hasTopLevelCallables())
                    it.rename(config.randomClassName + KOTLIN_SUFFIX, "File")
            }

            is KtClass -> {
                it.renameReference()
                it.rename(config.randomClassName, "Class")
            }

            is KtObjectDeclaration -> {
                if (!it.isObjectLiteral() && !it.isCompanion())
                    it.rename(config.randomClassName, "Object Class")
            }

            is KtNamedFunction -> {
                // 根据一些条件执行函数名重命名操作
                if (!it.hasModifier(KtTokens.OVERRIDE_KEYWORD) && !it.isMainFunction() && !it.isAnonymousFunction)
                    it.rename(config.randomMethodName, "Function")
            }

            is KtProperty -> {
                if (!it.hasModifier(KtTokens.OVERRIDE_KEYWORD))
                    it.rename(config.randomFieldName, "Property")
            }

            is KtParameter -> {
                if (!it.hasModifier(KtTokens.OVERRIDE_KEYWORD) && !skipElements.contains(it))
                    it.rename(config.randomFieldName, "Parameter")
            }
        }
    }
}