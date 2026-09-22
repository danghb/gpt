# WeChat NFC HCE Test

Android HCE 模拟 **NFC Forum Type 4 NDEF Tag**，用于验证微信官方“NFC 标签打开小程序”流程。

## v1.0.2

默认测试值：

```
weixin://dl/business/?t=B6pHVrURvPk
```

该 Scheme 来自 2026-04-24 的公开 `generateNFCScheme` 实战记录；同一记录中包含真实 `model_id`、`sn` 和 NFC Scheme 生成参数。它比普通 `https://wxaurl.cn/...` URL Link 更符合微信 NFC 官方文档要求。

> 这是公开测试样本，无法在无微信实机的情况下确认其当前服务端业务状态。最终仍以真机 NFC + 微信测试为准。

## 模拟格式

- URI Record
  - TNF: `0x01`
  - Type: `U`
  - Payload: `weixin://...` NFC Scheme
- Android Application Record
  - TNF: `0x04`
  - Type: `android.com:pkg`
  - Payload: `com.tencent.mm`

GitHub Actions 会先跑 NFC/NDEF/APDU 协议单元测试，再构建 APK。

正式下载：Releases → `WeChatNfcHce-v1.0.2.apk`
