package com.murphy.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * 这段 Kotlin 代码定义了一个名为 notifyInfo 的函数，用于发送通知给用户。该函数接受两个参数：project（项目对象）和 content（通知内容）。它使用 IntelliJ 平台的通知 API 来创建和显示通知。
 *
 * NotificationGroupManager.getInstance() 获取通知组管理器的实例，用于管理通知组。
 * .getNotificationGroup("AndProguard Notification") 从通知组管理器中获取名为 "AndProguard Notification" 的通知组。
 * .createNotification(...) 创建一个通知，指定通知的标题、内容和类型（这里是 INFORMATION 类型）。
 * .notify(project) 发送通知给指定的项目。
 * 在这段代码中，notifyInfo 函数的作用是在指定的项目中显示一个信息通知，通知标题为 "AndProguard finished!"，内容为传入的 content 参数内容。
 */
fun notifyInfo(project: Project?, content: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("AndProguard Notification")
        .createNotification(title = "AndProguard finished!", content = content, type = NotificationType.INFORMATION)
        .notify(project)
}

fun notifyWarn(project: Project?, content: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("AndProguard Notification")
        .createNotification(title = "AndProguard warning!", content = content, type = NotificationType.WARNING)
        .notify(project)
}

fun notifyError(project: Project?, content: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("AndProguard Notification")
        .createNotification(title = "AndProguard failed!", content = content, type = NotificationType.ERROR)
        .notify(project)
}