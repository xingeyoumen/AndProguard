package com.murphy.config

import com.intellij.openapi.options.Configurable
import com.murphy.ui.AndProguardForm
import javax.swing.JComponent

/**
 * 这个接口通常用于定义插件的配置页面，可以让用户在应用中配置插件的设置
 *
 * 配置信息：位置打开属性页面的配置
 * <applicationConfigurable
 *                 parentId="tools"
 *                 id="and-proguard-config"
 *                 instance="com.murphy.config.AndProguardConfigurable"
 *                 displayName="AndProguard Config"/>
 *
 * applicationConfigurable 接口通常被实现为一个配置界面的主要入口点。开发者可以通过实现这个接口来定义插件的配置面板，并在插件设置中心让用户进行相关配置。
 * 实现 applicationConfigurable 接口需要提供以下功能：
 *
 * 创建一个面板或配置界面，用于展示和修改插件的设置选项。
 * 处理用户在配置界面上所做的更改，将这些更改应用到插件的逻辑中。
 * 通过 applicationConfigurable 接口，插件开发者可以将他们的插件集成到 IntelliJ 平台的设置中心，使用户可以方便地配置插件所提供的功能和选项。
 *
 */
class AndProguardConfigurable : Configurable {
    /**
     * 懒加载
     * form 是一个私有属性，并且使用了 lazy 委托。
     * lazy { ... } 函数会在首次访问 form 属性时执行大括号内的初始化代码块，并返回结果。
     *
     *
     * 当第一次访问 form 属性时，会初始化一个 AndProguardForm 对象，并使用 AndProguardConfigState 实例中的配置信息来填充该对象。
     * 这种延迟加载的方式可以在需要时才初始化 form，而不是在每次代码执行时都创建一个新的 AndProguardForm 实例，从而节省资源和提高性能
     */
    private val form by lazy {
        // 获取 AndProguardConfigState 的单例实例
        val state = AndProguardConfigState.getInstance()

        // 本地默认配置值，混淆方式规则
        // 创建一个 AndProguardForm 对象：
        // 参数包括 state.skipData、state.classRule、state.methodRule 等配置信息，用于初始化 AndProguardForm 实例。
        //初始化将本地的规则，设置到页面上，并且创建一个实例。
        AndProguardForm(
            state.skipData,
            state.classRule,
            state.methodRule,
            state.fieldRule,
            state.idResRule,
            state.layoutResRule,
            state.excludePath
        )
    }

    /**
     *
     * Creates new Swing form that enables user to configure the settings. Usually this method is called on the EDT, so it should not take a long time.
     * Also this place is designed to allocate resources (subscriptions/ listeners etc.)
     * Returns:
     * new Swing form to show, or null if it cannot be created
     * See Also:
     * disposeUIResources
     *
     * 创建新的 Swing 表单，使用户能够配置设置。通常此方法在 EDT 上调用，因此不会花费很长时间。
     * 此外，此位置还用于分配资源（订阅/侦听器等）
     * 返回：
     * 要显示的新 Swing 表单，如果无法创建则返回 null
     * 另请参阅：
     * disposeUIResources
     *
     * 创建一个组件（JComponent），并返回该组件的实例。
     *
     */
    override fun createComponent(): JComponent? {

        //JPanel 表单组件
        return form.panel
    }


    // 是否配置信息有更新，有一项改动就算有更新
    override fun isModified(): Boolean {
        val state = AndProguardConfigState.getInstance()
        return state.classRule != form.classRule || state.methodRule != form.methodRule ||
                state.fieldRule != form.fieldRule || state.idResRule != form.idResRule ||
                state.layoutResRule != form.layoutResRule || state.skipData != form.skipData ||
                state.excludePath != form.excludePath
    }

    /**
     * Stores the settings from the Swing form to the configurable component. This method is called on EDT upon user's request.
     * 将 Swing 表单中的设置存储到可配置组件。此方法在用户请求时在 EDT 上调用。
     *
     * 将页面数据传递到修改的插件之中，重新复制
     */
    override fun apply() {
        val state = AndProguardConfigState.getInstance()
        state.classRule = form.classRule
        state.methodRule = form.methodRule
        state.fieldRule = form.fieldRule
        state.idResRule = form.idResRule
        state.layoutResRule = form.layoutResRule
        state.skipData = form.skipData
        state.excludePath = form.excludePath
    }

    // 重置，恢复属性配置设置
    override fun reset() {
        val state = AndProguardConfigState.getInstance()
        form.classRule = state.classRule
        form.methodRule = state.methodRule
        form.fieldRule = state.fieldRule
        form.idResRule = state.idResRule
        form.layoutResRule = state.layoutResRule
        form.skipData = state.skipData
        form.excludePath = state.excludePath
    }


    /**
     * Returns the visible name of the configurable component. Note, that this method must return the display name that is equal to the display name declared in XML to avoid unexpected errors.
     * Returns:
     * the visible name of the configurable component
     *
     *
     * 返回可配置组件的可见名称。请注意，此方法必须返回与 XML 中声明的显示名称相同的显示名称，以避免出现意外错误。
     * 返回：
     * 可配置组件的可见名称
     *
     *
     * 标签中的配置信息
     * <applicationConfigurable
     *                 parentId="tools"
     *                 id="and-proguard-config"
     *                 instance="com.murphy.config.AndProguardConfigurable"
     *                 displayName="AndProguard Config"/>
     *
     * "AndProguard Config"
     *
     *
     *
     *
     */
    override fun getDisplayName(): String = "AndProguard Config"
}