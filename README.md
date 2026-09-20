# WeChat NFC HCE Test

一个最小 Android HCE 测试 App：把 Android 手机模拟成 **NFC Forum Type 4 NDEF Tag**，用于测试 NFC 拉起微信小程序。

## 使用
1. Android 手机支持 NFC + HCE。
2. 安装 APK，输入微信 NFC openlink 或 HTTPS 小程序 URL Link。
3. 点击“开始模拟”，保持 App 前台、屏幕亮着。
4. 用另一台 Android 或 iPhone 靠近测试。

GitHub Actions 会自动构建 Debug APK，产物名：`WeChatNfcHce-debug-apk`。
