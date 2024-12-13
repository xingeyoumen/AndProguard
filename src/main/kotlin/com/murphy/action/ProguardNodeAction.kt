package com.murphy.action

import com.android.resources.ResourceType
import com.android.tools.idea.res.psi.ResourceReferencePsiElement
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.psi.PsiBinaryFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.source.PsiClassImpl
import com.intellij.psi.impl.source.PsiEnumConstantImpl
import com.intellij.psi.impl.source.PsiFieldImpl
import com.intellij.psi.impl.source.PsiMethodImpl
import com.intellij.psi.impl.source.PsiParameterImpl
import com.intellij.psi.impl.source.tree.java.PsiLocalVariableImpl
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.murphy.config.AndConfigState
import com.murphy.core.*
import com.murphy.core.ResourceGenerator.excludedFileType
import com.murphy.core.ResourceGenerator.includedAttrType
import com.murphy.core.rename
import com.murphy.core.renameX
import com.murphy.util.KOTLIN_SUFFIX
import com.murphy.util.LogUtil
import com.murphy.util.notifyInfo
import com.murphy.util.notifyWarn
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVariableDeclaration

/**
 * 只修改当前的文件名，文件夹名
 * 类名
 * layout xml name
 * 单个修改的右键标识，类中的变量和方法等
 *
 * layout中的ID尝试了修改不成功。
 * layout的Binding也无法修改。
 */
class ProguardNodeAction : AnAction() {


    override fun actionPerformed(action: AnActionEvent) {
        /**
         * 在这段 Kotlin 代码中，actionPerformed 方法使用了 AnActionEvent 参数，从中提取了 PSI_ELEMENT 和 project。这段代码的作用可能是处理某个动作的具体逻辑，可能是在 IntelliJ IDEA 插件开发中。
         *
         * actionPerformed 方法是 AnAction 的一个回调方法，用于处理特定动作触发后的逻辑。
         * action.getData(PlatformDataKeys.PSI_ELEMENT) 用于从事件中获取 PSI_ELEMENT，这表示与代码元素相关的信息，通常用于进行代码分析或操作。
         * action.project 用于获取当前项目的引用，以便在插件中执行项目级别的操作。
         */
        val myPsi = action.getData(PlatformDataKeys.PSI_ELEMENT) ?: return
        val myProject = action.project ?: return

        /**
         *
         * public static void rename(PsiNamedElement element, String newName, String desc, Project project) {
         *         element.rename(newName, desc, project);
         *     }
         *
         *
         * public static void renameX(XmlAttributeValue attributeValue, String newName, String desc, Project project) {
         *         attributeValue.renameX(newName, desc, project);
         *     }
         *
         *
         */
        /**
         * Kotlin
         * 调用 rename 函数：调用前 rename 函数，传入 newName 和 desc 参数。
         * 传入 newName, desc, myProject：将 newName、desc 和 myProject 作为参数传递给后 rename 函数。
         *
         * ProguardNodeAction:
         * Directory is PsiDirectory
         * ResourceFile is PsiBinaryFile, is JsonFile
         * XmlFile is XmlFile
         * Property is PsiParameterImpl, is PsiFieldImpl, is PsiLocalVariableImpl, is KtVariableDeclaration, is KtParameter
         * Function is KtNamedFunction, is PsiMethodImpl
         * Class is KtObjectDeclaration, is KtClass, is PsiClassImpl, is PsiEnumConstantImpl
         * KtFile 拼.kt is KtFile
         *
         */
        fun PsiNamedElement.rename(newName: String, desc: String) = rename(newName, desc, myProject)

        /**
         *
         * XmlAttribute is ResourceReferencePsiElement
         *
         */
        fun XmlAttributeValue.renameX(newName: String, desc: String) = renameX(newName, desc, myProject)

        //LogUtil 标识
        val label = "Obfuscate Node"
        LogUtil.logRecord(myProject, label, false)
        val startTime = System.currentTimeMillis()
        val config = AndConfigState.getInstance()
        //当前是否可以修改的 PsiNamedElement
        if (myPsi !is PsiNamedElement) {
            notifyWarn(myProject, "PsiElement ${myPsi.javaClass.name} $myPsi")
            return
        }
        //修改不同类型的名字
        when (myPsi) {
            is PsiDirectory -> {
                //按照文件夹类型的随机规则，修改文件夹名
                if (config.directoryRule.isEmpty()) return
                //desc 标识修改模块记录
                //LogUtil.info(myProject, String.format("[$desc] %s >>> %s", pair.second, newName))
                myPsi.getPackage()?.rename(config.randomDirectoryName, "Directory")
            }

            is PsiBinaryFile, is JsonFile -> {
                if (config.resFileRule.isEmpty()) return
                myPsi.rename(config.randomResFileName, "ResourceFile")
            }

            is XmlFile -> {
                //res 中的文件
                if (config.resFileRule.isEmpty()) return
                val resPsi = ResourceReferencePsiElement.create(myPsi) ?: return
                //当前文件类型ResourceType
                val resourceType = resPsi.resourceReference.resourceType
                if (resourceType == ResourceType.LAYOUT) {
                    //修改xml viewBinding 文件名
                    resPsi.renameLayout(config.randomResFileName, myProject)
                } else if (!excludedFileType.contains(resourceType)) {
                    //其余xml类型的，直接修改文件名
                    myPsi.rename(config.randomResFileName, "XmlFile")
                }
            }

            is PsiParameterImpl, is PsiFieldImpl, is PsiLocalVariableImpl,
            is KtVariableDeclaration, is KtParameter -> {
                if (config.propertyRule.isEmpty()) return
                myPsi.rename(config.randomPropertyName, "Property")
            }

            is KtNamedFunction, is PsiMethodImpl -> {
                //改单独的对应的变量名和方法名
                if (config.functionRule.isEmpty()) return
                myPsi.rename(config.randomFunctionName, "Function")
            }

            is KtObjectDeclaration, is KtClass,
            is PsiClassImpl, is PsiEnumConstantImpl -> {
                if (config.classRule.isEmpty()) return
                myPsi.rename(config.randomClassName, "Class")
            }

            is KtFile -> {
                //.kt文件 KtFile
                if (config.classRule.isEmpty()) return
                myPsi.rename(config.randomClassName + KOTLIN_SUFFIX, "KtFile")
            }

            is ResourceReferencePsiElement -> {
                if (config.resourceRule.isEmpty()) return
                val resourceType = myPsi.resourceReference.resourceType
                if (resourceType == ResourceType.ID) {
                    //layout view ID
                    myPsi.renameId(config.randomResourceName, myProject)
                } else if (includedAttrType.contains(resourceType)) {
                    val delegate = myPsi.delegate
                    if (delegate is XmlAttributeValue) {
                        delegate.renameX(config.randomResourceName, "XmlAttribute")
                    }
                }
            }

            else -> notifyWarn(myProject, "PsiNamedElement ${myPsi.javaClass.name} $myPsi")
        }
        notifyInfo(myProject, "refactor finished, take ${computeTime(startTime)}")
        LogUtil.logRecord(myProject, label, true)
    }

}