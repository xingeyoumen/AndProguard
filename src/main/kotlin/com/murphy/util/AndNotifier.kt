package com.murphy.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project


//不同类型的通知显示
/**
 * 这个方法用于显示信息通知，通常用于通知用户操作成功或一般信息。
 * 通过 NotificationGroupManager.getInstance().getNotificationGroup("AndProguard Notification") 获取特定名称的通知组。
 * 使用 createNotification() 创建一个信息通知，设置标题为 "AndProguard finished!"，内容为传入的 content 字符串，类型为 NotificationType.INFORMATION，表示信息性通知。
 * 最后，使用 .notify(project) 方法将通知显示给用户，project 参数表示通知显示在哪个项目中。
 */
fun notifyInfo(project: Project?, content: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("AndProguard Notification")
        .createNotification(title = "AndProguard finished!", content = content, type = NotificationType.INFORMATION)
        .notify(project)
}

/**
 * 这个方法用于显示警告通知，通常用于提示用户潜在的问题或警告。
 * 通过 NotificationGroupManager.getInstance().getNotificationGroup("AndProguard Notification") 获取通知组。
 * 创建一个警告通知，标题为 "AndProguard warning!"，内容为传入的 content 字符串，类型为 NotificationType.WARNING，表示警告通知。
 * 最后，将通知显示给用户，显示在指定的项目中。
 */
fun notifyWarn(project: Project?, content: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("AndProguard Notification")
        .createNotification(title = "AndProguard warning!", content = content, type = NotificationType.WARNING)
        .notify(project)
}

/**
 * 这个方法用于显示错误通知，通常用于指示操作失败或错误情况。
 * 获取通知组，创建一个错误通知，标题为 "AndProguard failed!"，内容为传入的 content 字符串，类型为 NotificationType.ERROR，表示错误通知。
 * 最后，将错误通知显示给用户，显示在指定的项目中。
 */
fun notifyError(project: Project?, content: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("AndProguard Notification")
        .createNotification(title = "AndProguard failed!", content = content, type = NotificationType.ERROR)
        .notify(project)
}