# WeChat NFC HCE Test

Android HCE 模拟 **NFC Forum Type 4 NDEF Tag**。

## v1.0.3

支持三种 URI：

```
weixin://...
https://...
http://...
```

新安装默认仍使用微信 NFC Scheme 测试样本：

```
weixin://dl/business/?t=B6pHVrURvPk
```

但 HTTP/HTTPS 功能完整保留，升级时也不会再强制替换之前保存的 URL。

### 模式说明

- `weixin://...`
  - 用于微信官方“NFC 标签打开小程序”场景
  - URI Record + 微信 AAR
- `http://...` / `https://...`
  - 用于普通 NDEF URL、URL Link、网页跳转测试
  - 同样使用 URI Record + 微信 AAR

GitHub Actions 会分别测试 `weixin://`、`https://` 和 `http://` 的 NDEF 编码，再构建 APK。

正式下载：Releases → `WeChatNfcHce-v1.0.3.apk`
