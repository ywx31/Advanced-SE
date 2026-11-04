# Vibe Coding

## 当自然语言成为编程接口

<hr>

> Advanced Software Engineering · 2026

---

## 一个值得先回答的问题

<div style="margin:50px auto 38px;max-width:1120px;padding:36px 44px;background:#faf5fb;border-left:10px solid #6a005f;">
<p style="font-size:42px;line-height:1.45;margin:0;">如果实现一个功能只要 20 分钟，软件工程师的时间应该花在哪里？</p>
</div>

<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:18px;">
<div style="padding:18px;background:#f8fafc;text-align:center;">定义问题</div>
<div style="padding:18px;background:#f8fafc;text-align:center;">设计边界</div>
<div style="padding:18px;background:#f8fafc;text-align:center;">验证行为</div>
<div style="padding:18px;background:#f8fafc;text-align:center;">维护系统</div>
</div>

<hr>

> 这份幻灯片的迭代过程，本身也是一个小例子。

---

## 定义：Vibe Coding 是什么？

<div style="margin:26px 0 30px;padding:26px 32px;background:#f8fafc;border:1px solid #e2e8f0;">
<p style="font-size:36px;line-height:1.5;margin:0;">用自然语言驱动 AI 生成、修改、运行和调试代码；人类负责目标、上下文、反馈与验收。</p>
</div>

<div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;">
<div style="padding:18px 24px;background:#faf5fb;">自然语言优先</div>
<div style="padding:18px 24px;background:#f0fdfa;">结果导向</div>
<div style="padding:18px 24px;background:#eff6ff;">持续反馈</div>
<div style="padding:18px 24px;background:#fff7ed;">人类保留判断</div>
</div>

---

## 这个词的原始含义，其实有点激进

> “There’s a new kind of coding I call ‘vibe coding’, where you fully give in to the vibes, embrace exponentials, and forget that the code even exists.”
>
> — Andrej Karpathy, 2025

<hr>

<div style="display:grid;grid-template-columns:1fr 1fr;gap:36px;">
<div style="padding:24px 28px;background:#fff7ed;border-top:8px solid #ea580c;">
<h3>极端版</h3>
<p>先让感觉带路，跑通就好；适合一次性原型和探索。</p>
</div>
<div style="padding:24px 28px;background:#f0fdf4;border-top:8px solid #16a34a;">
<h3>工程版</h3>
<p>让 AI 写更多代码，但用规格、测试和审查管理风险。</p>
</div>
</div>

---

## Vibe Coding 不是什么

<div style="margin-top:28px;font-size:38px;line-height:1.8;">
<p>✘ 不是“不用理解计算机系统”</p>
<p>✘ 不是“跳过测试和代码审查”</p>
<p>✘ 不是“模型永远比人更懂业务”</p>
<p>✘ 不是“自然语言会取代所有编程语言”</p>
</div>

<hr>

> Vibe coding 是一种协作方式，而不是质量保证。

---

## 它不是一种工具，而是一条协作光谱

| 模式 | 人的核心动作 | AI 的核心动作 |
| :--: | :--: | :--: |
| 自动补全 | 写主体代码 | 预测下一段 |
| 对话式协助 | 描述、修改、审查 | 解释、生成片段 |
| Vibe coding | 定目标、给反馈、验收 | 端到端实现与迭代 |
| Agentic engineering | 设规格、管风险、担责任 | 规划、执行、验证 |

<hr>

> 越往右，人的投入越从“敲代码”转向“定义与判断”。

---

## 为什么是现在？

<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:22px;margin-top:28px;">
<div style="padding:26px 22px;background:#faf5fb;border-top:7px solid #6a005f;">
<h3>能力</h3>
<p>模型开始具备规划、工具使用和长链路推理能力。</p>
</div>
<div style="padding:26px 22px;background:#eff6ff;border-top:7px solid #2563eb;">
<h3>上下文</h3>
<p>能理解整个仓库，而不只是当前文件。</p>
</div>
<div style="padding:26px 22px;background:#f0fdfa;border-top:7px solid #0f766e;">
<h3>反馈</h3>
<p>能执行命令、读取错误、修改并再次验证。</p>
</div>
</div>

<hr>

<div style="text-align:center;padding:18px;background:#111827;color:white;border-radius:10px;">
<span style="font-size:34px;">LLM + Tools + Feedback Loop = Coding Agent</span>
</div>

---

## 什么变便宜了，什么反而更贵了？

<div style="display:grid;grid-template-columns:1fr 1fr;gap:42px;margin-top:28px;">
<div style="padding:24px 30px;background:#f0fdf4;border-left:9px solid #16a34a;">
<h3>变便宜</h3>
<ul>
<li>第一版实现</li>
<li>样板代码</li>
<li>测试草稿</li>
<li>方案探索</li>
</ul>
</div>
<div style="padding:24px 30px;background:#fff7ed;border-left:9px solid #ea580c;">
<h3>变昂贵</h3>
<ul>
<li>问题定义</li>
<li>领域判断</li>
<li>验证与安全</li>
<li>长期维护</li>
</ul>
</div>
</div>

<hr>

> 瓶颈从“把代码打出来”转向“判断什么代码值得留下”。

---

## 软件抽象又向上走了一层

<div style="display:flex;align-items:center;justify-content:center;gap:12px;margin:42px 0;flex-wrap:wrap;">
<span style="padding:14px 18px;background:#e5e7eb;border-radius:7px;">机器码</span>
<span>→</span>
<span style="padding:14px 18px;background:#dbeafe;border-radius:7px;">汇编</span>
<span>→</span>
<span style="padding:14px 18px;background:#bfdbfe;border-radius:7px;">高级语言</span>
<span>→</span>
<span style="padding:14px 18px;background:#a7f3d0;border-radius:7px;">框架 / API</span>
<span>→</span>
<span style="padding:14px 18px;background:#6a005f;color:white;border-radius:7px;">自然语言 + Agent</span>
</div>

<div style="display:grid;grid-template-columns:1fr 1fr;gap:28px;">
<div style="padding:22px 28px;background:#f8fafc;">
<h3>收益</h3>
<p>人可以在更高的意图层工作，减少机械转换。</p>
</div>
<div style="padding:22px 28px;background:#fff7ed;">
<h3>代价</h3>
<p>表达更模糊，风险也更难直接看见。</p>
</div>
</div>

<hr>

> 抽象越高级，验收越重要。

---

## 传统循环 vs AI 密集循环

<div style="display:grid;grid-template-columns:1fr 1fr;gap:44px;margin-top:30px;">
<div style="padding:26px;background:#f8fafc;">
<h3>传统</h3>
<p style="font-size:34px;line-height:1.8;">写代码<br>→ 编译<br>→ 调试<br>→ 提交</p>
</div>
<div style="padding:26px;background:#faf5fb;">
<h3>AI 密集</h3>
<p style="font-size:32px;line-height:1.6;">说清意图 → 提供上下文<br>→ 计划 → 执行<br>→ 观察 → 验证 → 检查点</p>
</div>
</div>

<hr>

> 工作单元从“多少行代码”变成“一个可验证的增量”。

---

## 核心循环：小步、可运行、可回退

<div style="display:flex;align-items:center;justify-content:center;gap:10px;margin:30px 0;flex-wrap:wrap;">
<span style="padding:13px 16px;background:#6a005f;color:white;border-radius:7px;">目标</span>
<span>→</span>
<span style="padding:13px 16px;background:#7c3aed;color:white;border-radius:7px;">上下文</span>
<span>→</span>
<span style="padding:13px 16px;background:#2563eb;color:white;border-radius:7px;">计划</span>
<span>→</span>
<span style="padding:13px 16px;background:#0891b2;color:white;border-radius:7px;">执行</span>
<span>→</span>
<span style="padding:13px 16px;background:#0f766e;color:white;border-radius:7px;">观察</span>
<span>→</span>
<span style="padding:13px 16px;background:#b45309;color:white;border-radius:7px;">验证</span>
<span>→</span>
<span style="padding:13px 16px;background:#111827;color:white;border-radius:7px;">检查点</span>
</div>

1. 每次只推进一个**可观察的结果**
2. 每轮都留下**可运行的证据**
3. 每个关键步骤都保持**可回退**

<hr>

> 不要一次许愿一个系统；一次证明一个增量。

---

## Context engineering：上下文也是程序的一部分

<div style="display:grid;grid-template-columns:1fr 1fr;gap:38px;margin-top:24px;">
<div style="padding:22px 28px;background:#f0fdf4;border-left:8px solid #16a34a;">
<h3>应该提供</h3>
<ul>
<li>目标与非目标</li>
<li>相关文件与仓库地图</li>
<li>领域规则与样例</li>
<li>约束与验收标准</li>
<li>真实错误与日志</li>
</ul>
</div>
<div style="padding:22px 28px;background:#fff7ed;border-left:8px solid #ea580c;">
<h3>不要堆砌</h3>
<ul>
<li>整仓库无差别倾倒</li>
<li>过期文档</li>
<li>密钥与隐私数据</li>
<li>互相矛盾的指令</li>
</ul>
</div>
</div>

<hr>

> 更好的上下文，比更长的 Prompt 更重要。

---

## 任务拆分：把“大愿望”变成“小证据”

| 反模式 | 更好的做法 |
| :--: | :--: |
| “把整个系统做出来” | 先做一条端到端纵切 |
| 同时重构和加功能 | 分开提交、分开验证 |
| “优化一下” | 给出可观察的指标 |
| 直接猜 Bug | 先稳定复现问题 |

<hr>

> 每轮只改变一个可验证的结果。

---

## Prompt 是新的“需求接口”

```text
目标：为现有项目增加 CSV 导出功能
上下文：技术栈、相关文件、数据样例
约束：不改变现有 API；不新增依赖
验收：测试通过；字段和排序正确
协作：先给计划和风险，再修改代码
```

<hr>

<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:18px;">
<div style="padding:16px;background:#f8fafc;text-align:center;">具体 &gt; 抽象</div>
<div style="padding:16px;background:#f8fafc;text-align:center;">样例 &gt; 形容词</div>
<div style="padding:16px;background:#f8fafc;text-align:center;">验收 &gt; 期待</div>
</div>

---

## 先要计划，再要代码

<div style="font-size:34px;line-height:1.65;margin-top:24px;">
<p>1. 会改哪些文件？为什么？</p>
<p>2. 有哪些关键假设？</p>
<p>3. 最大风险是什么？</p>
<p>4. 如何测试？</p>
<p>5. 最小安全改动是什么？</p>
</div>

<hr>

> 让 AI 提出方案，让人批准范围。

---

## 在真实代码库里工作

<div style="display:grid;grid-template-columns:1fr 1fr;gap:18px;margin-top:26px;">
<div style="padding:18px 24px;background:#f8fafc;">先读，再写</div>
<div style="padding:18px 24px;background:#f8fafc;">复用现有模式</div>
<div style="padding:18px 24px;background:#f8fafc;">保持 diff 小</div>
<div style="padding:18px 24px;background:#f8fafc;">不随意升级依赖</div>
<div style="padding:18px 24px;background:#f8fafc;">不确定就提问</div>
<div style="padding:18px 24px;background:#f8fafc;">运行现有检查</div>
</div>

<hr>

> 好的 Agent 像一个谨慎的新同事，而不是一个自信的陌生人。

---

## Agent 执行循环：自动化不等于失控

<div style="display:flex;align-items:center;justify-content:center;gap:10px;margin:30px 0;flex-wrap:wrap;">
<span style="padding:13px 16px;background:#6a005f;color:white;border-radius:7px;">执行</span>
<span>→</span>
<span style="padding:13px 16px;background:#2563eb;color:white;border-radius:7px;">观察</span>
<span>→</span>
<span style="padding:13px 16px;background:#0f766e;color:white;border-radius:7px;">诊断</span>
<span>→</span>
<span style="padding:13px 16px;background:#b45309;color:white;border-radius:7px;">修复</span>
<span>→</span>
<span style="padding:13px 16px;background:#111827;color:white;border-radius:7px;">报告</span>
</div>

- 把**真实错误**和上下文一起反馈
- 要求先解释假设，再继续修改
- 连续失败时停下来，而不是反复试错
- 最后总结改动、测试和剩余风险

<hr>

> 没有可观测性的自主，只是赌博。

---

## 验证阶梯：自主性越强，证据要越硬

<div style="margin-top:22px;font-size:28px;line-height:1.75;">
<div style="padding:10px 18px;background:#fef2f2;border-left:8px solid #dc2626;">L5 生产可观测性：日志、指标、告警</div>
<div style="padding:10px 18px;background:#fff7ed;border-left:8px solid #ea580c;">L4 审查与安全：静态检查、依赖、威胁面</div>
<div style="padding:10px 18px;background:#fefce8;border-left:8px solid #ca8a04;">L3 人工验收：真实场景与业务规则</div>
<div style="padding:10px 18px;background:#f0fdf4;border-left:8px solid #16a34a;">L2 集成 / E2E：组件之间真的能协作</div>
<div style="padding:10px 18px;background:#eff6ff;border-left:8px solid #2563eb;">L1 单元测试：行为与边界条件</div>
<div style="padding:10px 18px;background:#f8fafc;border-left:8px solid #64748b;">L0 编译 / 类型 / lint：最基本的正确性</div>
</div>

<hr>

> “能运行”只是起点，不是验收。

---

## 如何审查 AI 生成的代码？

<div style="display:grid;grid-template-columns:1fr 1fr;gap:18px;margin-top:24px;">
<div style="padding:18px 22px;background:#f8fafc;">边界条件与失败模式</div>
<div style="padding:18px 22px;background:#f8fafc;">错误处理与重试策略</div>
<div style="padding:18px 22px;background:#f8fafc;">安全、密钥与权限</div>
<div style="padding:18px 22px;background:#f8fafc;">依赖变化与供应链</div>
<div style="padding:18px 22px;background:#f8fafc;">隐藏耦合与复杂度</div>
<div style="padding:18px 22px;background:#f8fafc;">测试是否真的验证行为</div>
</div>

<hr>

> 生成代码没有更低的审查标准。尤其要检查它“没做什么”。

---

## 检查点与可逆性

<div style="display:grid;grid-template-columns:1fr 1fr;gap:38px;margin-top:26px;">
<div style="padding:24px 30px;background:#f0fdf4;border-left:8px solid #16a34a;">
<h3>推荐</h3>
<ul>
<li>每个任务一个分支</li>
<li>小 commit</li>
<li>功能开关</li>
<li>备份与回滚方案</li>
</ul>
</div>
<div style="padding:24px 30px;background:#fff7ed;border-left:8px solid #ea580c;">
<h3>高风险</h3>
<ul>
<li>不可逆数据操作</li>
<li>大范围自动替换</li>
<li>无审查依赖升级</li>
<li>没有基线就重构</li>
</ul>
</div>
</div>

<hr>

> 跑得越快，干净的“撤销”就越值钱。

---

## 什么场景适合 Vibe Coding？

| 场景 | 可逆 | 不可逆 |
| :--: | :--: | :--: |
| **熟悉领域** | ✅ 优先尝试 | ⚠️ 强化审查与回滚 |
| **未知领域** | 🧪 适合快速探索 | ⛔ 先研究，再让人决策 |

<hr>

> 原型、脚本、测试、文档、局部重构通常是低风险入口；
> 认证、支付、迁移、合规与安全关键路径需要更高门槛。

---

## 案例：一个 CSV 对账工具

```text
目标：实现 CSV 对账 CLI
约束：不覆盖输入；金额使用 Decimal
验收：3 个样例通过；差异写入 report.csv
协作：先读项目，再给计划和风险
```

<div style="display:grid;grid-template-columns:1fr 1fr;gap:22px;margin-top:22px;">
<div style="padding:18px 24px;background:#eff6ff;">AI：生成实现、测试和文档</div>
<div style="padding:18px 24px;background:#f0fdf4;">人：确认业务规则与异常口径</div>
</div>

<hr>

> AI 很快做出了“能跑的版本”；真正的价值在于发现：空值、时区和舍入规则必须先定义。

---

## 团队工作流：人仍然是变更的所有者

<div style="display:flex;align-items:center;justify-content:center;gap:9px;margin:28px 0;flex-wrap:wrap;">
<span style="padding:12px 14px;background:#6a005f;color:white;border-radius:6px;">Issue</span>
<span>→</span>
<span style="padding:12px 14px;background:#7c3aed;color:white;border-radius:6px;">Spec</span>
<span>→</span>
<span style="padding:12px 14px;background:#2563eb;color:white;border-radius:6px;">Plan</span>
<span>→</span>
<span style="padding:12px 14px;background:#0891b2;color:white;border-radius:6px;">Branch</span>
<span>→</span>
<span style="padding:12px 14px;background:#0f766e;color:white;border-radius:6px;">CI</span>
<span>→</span>
<span style="padding:12px 14px;background:#b45309;color:white;border-radius:6px;">Review</span>
<span>→</span>
<span style="padding:12px 14px;background:#111827;color:white;border-radius:6px;">Merge</span>
</div>

- 工程师：定义验收、架构边界和风险
- Agent：实现、运行、迭代、汇报
- CI：提供可重复的证据
- Reviewer：对可维护性与后果负责

<hr>

> 可以委托执行，不能委托责任。

---

## 如何衡量它是否真的有用？

| 容易误导的指标 | 更有意义的指标 |
| :--: | :--: |
| 生成代码行数 | 交付周期 |
| Prompt 数量 | 变更失败率 |
| “AI 写了多少” | 逃逸缺陷与返工 |
| Token 消耗 | 审查延迟与开发者体验 |

<hr>

> 速度如果没有质量，只是在更快地积累技术债。

---

## 风险不会消失，只会转移

<div style="font-size:31px;line-height:1.75;margin-top:22px;">
<p>幻觉 API / 逻辑错误 → 可执行测试、类型检查</p>
<p>提示注入 / 越权操作 → 明确信任边界与最小权限</p>
<p>依赖投毒 / 供应链 → 锁定版本、审查新增依赖</p>
<p>数据泄露 → 脱敏、私有边界、密钥管理</p>
<p>维护债 / 隐藏复杂度 → 小 diff、文档、可读性审查</p>
<p>过度信任 → 人工签字与最终责任</p>
</div>

<hr>

> Vibe coding 不是“不看代码”，而是“把验证前移”。

---

## 六个常见反模式

<div style="display:grid;grid-template-columns:1fr 1fr;gap:18px;margin-top:24px;font-size:29px;">
<div style="padding:18px 22px;background:#fff7ed;">① 一口吃成胖子</div>
<div style="padding:18px 22px;background:#fff7ed;">② 只看 Demo，不看退出条件</div>
<div style="padding:18px 22px;background:#fff7ed;">③ 让 Agent 无限自动修复</div>
<div style="padding:18px 22px;background:#fff7ed;">④ 不加控制地产生大 diff</div>
<div style="padding:18px 22px;background:#fff7ed;">⑤ 没有版本控制就重构</div>
<div style="padding:18px 22px;background:#fff7ed;">⑥ 把“生成得快”当成“设计得好”</div>
</div>

<hr>

> 真正稀缺的不是代码，而是清晰的目标、可靠的判断和可维护的系统。

---

## 从 Vibe Coding 到 Vibe Engineering

| Vibe coding | Vibe engineering |
| :--: | :--: |
| 快、松、偏探索 | 快，但可追责 |
| 感觉对就继续 | 规格、测试、审查 |
| 适合一次性原型 | 适合长期维护的系统 |
| 依赖即时反馈 | 依赖工程证据 |

<hr>

> 业界也越来越用 **Agentic Engineering** 描述这种专业版本：AI 大量执行，人负责规格、验证与后果。

---

## 工程师应该继续加强什么？

<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:20px;margin-top:28px;">
<div style="padding:22px 18px;background:#faf5fb;text-align:center;">定义问题</div>
<div style="padding:22px 18px;background:#eff6ff;text-align:center;">设计边界</div>
<div style="padding:22px 18px;background:#f0fdfa;text-align:center;">读懂代码</div>
<div style="padding:22px 18px;background:#fefce8;text-align:center;">验证行为</div>
<div style="padding:22px 18px;background:#fff7ed;text-align:center;">管理风险</div>
<div style="padding:22px 18px;background:#f8fafc;text-align:center;">对结果负责</div>
</div>

<hr>

> 模型越强，越值钱的是那些能够判断输出质量的能力。

---

# 让 AI 加速探索，而不是替代判断

<div style="margin:34px auto 28px;max-width:960px;text-align:left;font-size:31px;line-height:1.7;">
<p>✅ Vibe coding 改变的是杠杆，不是责任</p>
<p>✅ 上下文与验收，是新的编程接口</p>
<p>✅ 自主性越强，验证越要硬</p>
<p>✅ 最好的团队用 AI 提高上限，而不是降低标准</p>
</div>

<hr>

> <red>少写代码，不等于少做工程。</red>
