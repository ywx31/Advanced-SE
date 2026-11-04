# Vibe Coding Introduce

《Vibe Coding：当自然语言成为编程接口》课程分享材料，包含可编辑源文件、网页幻灯片和 PDF 版本。

## 直接阅读

**推荐直接下载 PDF 阅读，无需安装任何开发环境**：

- [下载 PDF：vibe-coding.pdf](./vibe-coding.pdf)
- [网页幻灯片入口：dist/index.html](./dist/index.html)
- [Markdown 源文件：vibe-coding.md](./vibe-coding.md)

> GitHub 页面通常不能直接运行完整的 Reveal.js 网页幻灯片。要查看网页版，请先克隆仓库，再使用本地静态文件服务器打开。

## 内容概览

这份 29 页的分享围绕一个核心观点展开：

> Vibe Coding 改变的是软件生产的杠杆，而不是工程责任。

主要内容包括：

1. Vibe Coding 的定义、来源和适用边界
2. 从自动补全到 Agentic Engineering 的人机协作光谱
3. 为什么现在出现 Vibe Coding
4. 上下文工程、任务拆分和 Prompt 设计
5. Agent 执行循环、验证阶梯和代码审查
6. 真实代码库中的工作方式与可逆性设计
7. 场景选择、案例、团队协作和效果度量
8. 安全、风险、反模式与工程化实践
9. 从 Vibe Coding 走向 Vibe Engineering

## 拉取文件

### 首次克隆

```bash
git clone https://github.com/ywx31/Advanced-SE.git
cd Advanced-SE/vibe-coding-introduce
```

### 已有仓库时更新

```bash
cd Advanced-SE
git pull
cd vibe-coding-introduce
```

## 查看内容

### 方式一：PDF

直接打开或下载：

```text
vibe-coding.pdf
```

macOS 也可以执行：

```bash
open vibe-coding.pdf
```

### 方式二：网页幻灯片

只需要 Python 3 自带的静态文件服务器，或其他任意 HTTP 服务器：

```bash
cd vibe-coding-introduce
python3 -m http.server 8000
```

然后浏览器打开：

```text
http://localhost:8000/dist/index.html
```

查看网页版不需要 Poetry，也不需要安装项目依赖。

## 从 Markdown 重新生成网页

如果需要修改 `vibe-coding.md` 并重新生成 `dist/`，需要准备以下环境：

- Python 3.10 或更高版本
- Poetry
- Git

检查环境：

```bash
python3 --version
poetry --version
```

安装 Poetry：

```bash
# macOS + Homebrew
brew install poetry
```

或使用跨平台方式：

```bash
python3 -m pip install --user pipx
pipx install poetry
```

克隆并安装 jyyslide-md 主题：

```bash
git clone https://github.com/zweix123/jyyslide-md.git
cd jyyslide-md
poetry install
```

重新生成网页：

```bash
poetry run python main.py /path/to/Advanced-SE/vibe-coding-introduce/vibe-coding.md
```

生成结果会写入 Markdown 同级的 `dist/` 目录：

```text
vibe-coding-introduce/dist/index.html
vibe-coding-introduce/dist/static/
```

## 重新导出 PDF

网页幻灯片基于 Reveal.js，可使用浏览器的打印功能导出 PDF：

1. 在本地服务器中打开 `dist/index.html?print-pdf`
2. 选择“打印”
3. 目标选择“另存为 PDF”
4. 纸张方向选择横向
5. 边距选择无
6. 开启“背景图形”或“打印背景”

本目录已经包含导出的 `vibe-coding.pdf`，日常分享无需重复导出。

## 目录结构

```text
vibe-coding-introduce/
├── README.md
├── vibe-coding.md
├── vibe-coding.pdf
└── dist/
    ├── index.html
    └── static/
        ├── css/
        ├── img/
        ├── jyy/
        ├── katex/
        └── reveal/
```

## 相关链接

- [返回仓库主页](../README.md)
- [jyyslide-md 项目](https://github.com/zweix123/jyyslide-md)
