package com.murphy.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.murphy.util.RandomNode
import com.murphy.util.RandomNode.Companion.parseNode

@State(
    name = "AndProguardConfigState",
    storages = [Storage("AndProguardConfigState.xml")],
)

/**
 * 1 标识 []：四位标识确定随机字符的范围
 * [	0 | 1	0 | 1	0 | 1	0 | 1	]
 * *	大写字母	小写字母	数字	下划线	*
 * *	A-Z	a-z	0-9	_	*
 *
 * 2 长度 ()：随机字符或组合的重复次数
 * 闭区间表示 (start, end) 或固定长度表示 (length)
 *
 * 3 简单示例：
 * [1000](1)[0100](3,9) （大驼峰伪单词）表示大写字母开头，后接3至9位小写字母
 *
 * 4 复用组合 {}：
 * {[1000](1)[0100](3,9)}(2,3) 表示2至3个大驼峰伪单词
 *
 * 5 固定字符串 <>：
 * {[1000](1)[0100](3,9)}(1,2)<Activity> 表示1至2个大驼峰伪单词，后接 Activity
 *
 *
 * applicationService
 * 在 IntelliJ IDEA 插件开发中，@State 注解通常与 PersistentStateComponent 接口一起使用，用于在插件生命周期之间保持持久化状态。
 * 这个注解用于标记一个类，使其能够保存和恢复状态信息。
 */
class AndProguardConfigState : PersistentStateComponent<AndProguardConfigState> {
    var classRule: String = "{[1000](1)[0100](6,12)}(2,3)"
    var methodRule: String = "[0100](7,13){[1000](1)[0100](6,12)}(0,2)"
    var fieldRule: String = "[0100](7,13){[1000](1)[0100](6,12)}(0,1)"
    var idResRule: String = "[0100](7,11){<_>[0100](7,11)}(0,1)"
    var layoutResRule: String = "[0100](7,11){<_>[0100](7,11)}(1,2)"
    // Fill the package name. Use the symbol ; to connect multiple package names. The filled-in directory will be ignored when performing tasks on the folder
    var excludePath: String = ""

    /**
     * @Scope：Java/Kotlin
     * 是否忽略数据内容不处理：
     * 1. Java Bean： Getter/Setter 函数名，以及类中相对应的成员变量名
     * 2. Data Class： 主构造方法的参数名
     */
    var skipData: Boolean = true
    private lateinit var randomNodeList: List<RandomNode>
    lateinit var excludeList: List<String>

    val randomClassName get() = randomNodeList[0].randomString
    val randomMethodName get() = randomNodeList[1].randomString
    val randomFieldName get() = randomNodeList[2].randomString
    val randomIdResName get() = randomNodeList[3].randomString
    val randomLayoutResName get() = randomNodeList[4].randomString

    override fun getState(): AndProguardConfigState = this

    override fun loadState(state: AndProguardConfigState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun initRandomNode() {
        randomNodeList = listOf(
            classRule.parseNode(),
            methodRule.parseNode(),
            fieldRule.parseNode(),
            idResRule.parseNode(),
            layoutResRule.parseNode()
        )
    }

    fun initExcludePackage() {
        excludeList = excludePath.split(';').filter { it.isNotBlank() }
    }

    companion object {
        fun getInstance(): AndProguardConfigState {
            return ApplicationManager.getApplication().getService(AndProguardConfigState::class.java)
        }
    }
}