# WeChat NFC HCE Test

Android HCE 模拟 **NFC Forum Type 4 NDEF Tag**。

## v1.0.4

支持三种 URI：

```
weixin://...
https://...
http://...
```

新安装默认使用：

```
weixin://dl/business/?t=QDZVQEO2z9f
```

从 v1.0.3 升级时，如果仍保存的是旧内置默认值，会自动切换到新的默认 Scheme；用户自己保存的 HTTP/HTTPS 或其他 weixin:// 地址不会被覆盖。

GitHub Actions 会分别测试 weixin://、https:// 和 http:// 的 NDEF 编码，再构建 APK。

正式下载：Releases → `WeChatNfcHce-v1.0.4.apk`
