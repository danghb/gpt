# WeChat NFC HCE Test

一个最小 Android HCE 测试 App：把 Android 手机模拟成 **NFC Forum Type 4 NDEF Tag**，用于测试 NFC 拉起微信小程序。

## 使用
1. Android 手机支持 NFC + HCE。
2. v1.0.1 已内置当前可访问的 Kimi 小程序 URL Link：`https://wxaurl.cn/9JHGjdJl7fd`。
3. 安装后可以直接点击“开始模拟”，也可以把输入框改成你自己的微信 URL Link。
4. 保持 App 前台、屏幕亮着，用另一台 Android 或 iPhone 靠近测试。

GitHub Actions 会先跑 NFC 协议单元测试，再构建 APK。

正式下载请使用 Releases 中的 `WeChatNfcHce-v1.0.1.apk`。
