package com.murphy.core

import com.android.SdkConstants.*
import com.android.resources.ResourceType
import com.android.tools.idea.res.psi.ResourceReferencePsiElement
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.murphy.config.AndProguardConfigState
import java.util.*

const val TAG_INTEGER = "integer"

fun processXml(psi: XmlFile, resIdList: MutableList<String> = LinkedList()) {
    println("============================== ${psi.name} ==============================")
    val config = AndProguardConfigState.getInstance()
    // XmlTag用于表示 XML 标签的类之一，提供了一种表示 XML 标签及其属性、子标签等信息的方式
    val tagSeq = psi.childrenDfsSequence().filterIsInstance<XmlTag>()
    tagSeq.forEach { tag ->
        when (tag.name) {
            TAG_STRING, TAG_STRING_ARRAY, TAG_INTEGER, TAG_INTEGER_ARRAY, TAG_STYLE, TAG_COLOR, TAG_DIMEN -> {
                tag.getAttribute(ATTR_NAME)?.valueElement?.rename(config.randomIdResName, "Resource")
            }

            TAG_ITEM -> {
                val name = tag.parentTag?.name
                if (name != TAG_STYLE)
                    tag.getAttribute(ATTR_NAME)?.valueElement?.rename(config.randomIdResName, "Resource")
            }

            else -> {
                val attr = tag.getAttribute(ANDROID_NS_NAME_PREFIX + ATTR_ID) ?: return@forEach
                if (resIdList.contains(attr.value)) return@forEach
                val newName = config.randomIdResName
                resIdList.add(NEW_ID_PREFIX + newName)
                attr.valueElement?.rename(newName, "ResId")
            }
        }
    }
    if (psi.checkRename()) psi.rename(config.randomLayoutResName, "ResFile")
}

/**
 * 用于检查 XML 文件是否应该被重命名。
 * 用于在 XmlFile 类型对象上执行检查操作，并返回一个布尔值
 * 首先尝试创建一个资源引用对象，然后检查该资源引用的类型是否不属于特定类型（INTEGER、STYLE、COLOR、STRING、DIMEN 或 ATTR），
 * 如果不属于这些类型，则返回 true，表示应该执行重命名操作；否则返回 false，表示不需要重命名
 */
private fun XmlFile.checkRename(): Boolean {
    // 代码尝试创建一个 ResourceReferencePsiElement 对象，该对象可能是用于处理资源引用的类。
    // 如果创建失败（返回 null），则直接返回 false，表示无法进行重命名操作
    val resPsi = ResourceReferencePsiElement.create(this) ?: return false
    // 代码获取了资源引用对象的资源类型，并将其存储在 type 变量中
    val type = resPsi.resourceReference.resourceType
    // 这一行是一个返回语句，根据资源类型的条件来判断是否需要进行重命名操作。
    // 如果资源类型不是 INTEGER、STYLE、COLOR、STRING、DIMEN 或 ATTR 中的任何一种，那么返回 true，表示应该执行重命名操作。
    // 否则，如果资源类型是上述列出的某种类型，返回 false，表示不需要进行重命名操作
    return type != ResourceType.INTEGER && type != ResourceType.STYLE && type != ResourceType.COLOR
            && type != ResourceType.STRING && type != ResourceType.DIMEN && type != ResourceType.ATTR
}