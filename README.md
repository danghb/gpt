# WeChat NFC HCE Test

Android NFC 测试工具：既能模拟 **NFC Forum Type 4 NDEF Tag**，也能把同样的数据写入实体 NFC 卡。

## v1.0.5

默认 URI：

```
weixin://dl/business/?t=QDZVQEO2z9f
```

支持：

```
weixin://...
https://...
http://...
```

### 实体卡写入

点击 **写入实体 NFC 卡** 后贴卡，应用会写入：

1. URI Record
2. Android Application Record
   - Type: `android.com:pkg`
   - Payload: `com.tencent.mm`

写入后会立即读回并逐字节比较 NDEF 数据。

- 已经是 NDEF 的卡：写入后立即校验。
- 未格式化但支持 NDEF Format 的空白卡：先格式化并写入，然后提示移开卡片再贴一次完成校验。
- 会检查卡片是否可写和容量是否足够。
- **不会锁卡，也不会设置只读。**

模拟与实体写卡使用同一个 `NdefBuilder` 数据，因此两边 NDEF 内容保持一致。

GitHub Actions 会先跑 NFC/NDEF/APDU 单元测试，再构建 APK。

正式下载：Releases → `WeChatNfcHce-v1.0.5.apk`
