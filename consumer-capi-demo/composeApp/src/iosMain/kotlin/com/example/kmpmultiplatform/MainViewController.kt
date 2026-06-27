package com.example.kmpmultiplatform

import platform.UIKit.UIColor
import platform.UIKit.UIViewController
import platform.UIKit.UILabel
import platform.UIKit.UITextAlignmentCenter
import platform.UIKit.UIFont
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGPointMake

private fun uiColorFromHex(hex: String): UIColor {
    val clean = if (hex.startsWith("#")) hex.substring(1) else hex
    val value = clean.toLong(16)
    val r = ((value shr 16) and 0xFF).toDouble() / 255.0
    val g = ((value shr 8) and 0xFF).toDouble() / 255.0
    val b = (value and 0xFF).toDouble() / 255.0
    return UIColor(red = r, green = g, blue = b, alpha = 1.0)
}

private class MainViewControllerImpl : UIViewController() {
    private val label = UILabel(frame = CGRectMake(0.0, 0.0, 300.0, 80.0))
    override fun viewDidLoad() {
        super.viewDidLoad()
        val content = App()
        view.backgroundColor = UIColor.whiteColor
        label.text = content.text
        label.textAlignment = UITextAlignmentCenter
        label.textColor = uiColorFromHex(content.colorHex)
        label.font = if (content.bold) UIFont.boldSystemFontOfSize(content.sizeSp.toDouble())
        else UIFont.systemFontOfSize(content.sizeSp.toDouble())
        label.sizeToFit()
        view.addSubview(label)
    }
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        label.center = CGPointMake(view.center.x, view.center.y)
    }
}

fun MainViewController(): UIViewController {
    return MainViewControllerImpl()
}
