# 阿里云 OSS 头像上传配置

## 配置示例

```text
Bucket：填写自己的Bucket名称
地域：华北2（北京）
Endpoint：oss-cn-beijing.aliyuncs.com
Bucket ACL：公共读（按实际配置填写）
CORS 来源：http://localhost:5173、http://127.0.0.1:5173
CORS Methods：GET、PUT、HEAD
CORS Headers：*
CORS Expose Headers：ETag
```

## 推荐架构

```text
浏览器
  → 请求 Spring Boot 获取预签名 PUT URL
  → 浏览器直接 PUT 文件到 OSS
  → 浏览器请求 Spring Boot 确认 objectKey
  → 后端 HEAD 校验文件大小和类型
  → 数据库保存 avatar_url 和 avatar_object_key
  → 后端异步清理旧头像
```

AccessKey 只存在后端环境变量中，不会返回浏览器。

## 后端环境变量

```bash
export OSS_ENABLED=true
export OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
export OSS_BUCKET=填写自己的Bucket名称
export OSS_ACCESS_KEY_ID='<RAM AccessKey ID>'
export OSS_ACCESS_KEY_SECRET='<RAM AccessKey Secret>'
export OSS_PUBLIC_BASE_URL='https://填写自己的Bucket域名'
```

不要将 AccessKey 写入 Git、前端代码或聊天记录。

## RAM 最小权限策略

```json
{
  "Version": "1",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "oss:PutObject",
        "oss:GetObject",
        "oss:DeleteObject"
      ],
      "Resource": [
        "acs:oss:*:填写自己的阿里云账号ID:填写自己的Bucket名称/avatars/*"
      ]
    }
  ]
}
```

## 本地开发回退

未配置 OSS 时，后端自动使用：

```text
app.avatar.oss.enabled=false
```

文件保存至：

```text
backend/uploads/avatars/{userId}/
```

公开 URL：

```text
/uploads/avatars/{userId}/{uuid}.png
```

## API

```text
GET  /api/settings/avatar/config
POST /api/settings/avatar/presign
POST /api/settings/avatar/confirm
POST /api/settings/avatar/local
```

## 安全注意事项

- Bucket 为公共读，只保存公开头像，不保存隐私文件。
- 对象名使用随机 UUID，避免被枚举。
- 禁止上传 HTML、SVG、JavaScript 等可执行内容。
- AccessKey 必须使用 RAM 用户，不能使用阿里云主账号 AccessKey。
- 定期轮换 AccessKey，并配置 RAM 最小权限策略。
