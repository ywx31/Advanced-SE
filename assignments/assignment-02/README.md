# 作业2：四则运算练习卷 PDF 生成器

## 功能

- 自定义练习卷名称
- 自定义题目数量，支持 1～300 道，默认 300 道
- 选择“练习题”或“含答案”打印方式
- 自动避免重复题
- 每题固定三个数、两个运算符
- 中间结果与最终结果均为 0～100 的整数
- 除法保证整除且除数不为 0
- 在线预览
- 浏览器本地生成并下载 A4 PDF

## 项目页面

![img](Images/img.png)

## 环境要求

- Node.js 18+
- npm

## 拉取与启动

```bash
git clone https://github.com/ywx31/Advanced-SE.git
cd Advanced-SE/assignments/assignment-02
npm install
npm run dev
```

浏览器访问终端中显示的地址，通常是：

```text
http://localhost:5173
```

## 测试与构建

在 `assignments/assignment-02` 目录执行：

```bash
npm test
npm run build
```

构建结果位于 `dist/`。

## 项目结构

```text
assignment-02/
├── index.html
├── src/
│   ├── generator.js   题目生成、合法性校验、去重
│   ├── main.js        页面交互与 PDF 导出
│   └── style.css      页面和 PDF 排版
├── tests/
│   └── generator.test.js
└── package.json
```
