package com.sillyandroid.app.cardbuilder

/**
 * 搓卡模式 — 12 条内置 Prompt 模板。
 *
 * 每条模板包含：
 *  - prompt: 发给 LLM 的完整指令
 *  - tag: 输出的 XML 标签前缀（不包含闭合标记）
 *  - targetEntity: 采纳时的目标实体 ("character" | "worldbook" | "mvu" | "regex")
 *  - worldBookHint: 采纳到世界书时的推荐参数（仅 worldbook 条目有）
 */
object CardBuilderTemplates {

    data class Template(
        val id: Int,
        val title: String,
        val description: String,
        val prompt: String,
        val tag: String,
        val targetEntity: String,
        val worldBookHint: WbHint? = null
    )

    data class WbHint(
        val position: String = "before_char",
        val priority: Int = 10,
        val depth: Int = 0,
        val selective: Boolean = true
    )

    // ================================================================
    val templates: List<Template> = listOf(
        // ─── 1: 角色原点 ───
        Template(
            id = 1,
            title = "角色原点",
            description = "角色性格成因、过往经历、转变逻辑",
            targetEntity = "character",
            tag = "main-character-origin",
            prompt = """
你是一位专业的角色设计师。请根据用户的描述，深入挖掘角色的起源故事。

## 任务
分析并生成以下内容，用如下格式输出：

<main-character-origin-{角色名}>
## 核心性格特质
[列出 3-5 个关键词，并各用一句话解释成因]

## 过往经历
[分阶段描述角色的重要经历，每个阶段包含：时间节点、事件、对性格的影响]

## 关键转变
[是什么事件/人物导致角色变成现在的样子？具体发生了什么？]

## 深层动机
[角色真正的驱动力是什么？这个动机的根源在哪里？]

## 未愈创伤
[角色是否有未愈合的心理创伤或心结？在何种情形下会被触发？]
</main-character-origin-{角色名}>

在完成上述内容后，请思考是否还有遗漏的角色成因维度，并在末尾询问用户是否需要补充。
""".trimIndent()
        ),

        // ─── 2: 角色当前画像 ───
        Template(
            id = 2,
            title = "角色当前画像",
            description = "外貌、穿着、性格、行为趋向、形体、常用言语、动机逻辑、能力技能",
            targetEntity = "character",
            tag = "main-character-profile",
            prompt = """
你是一位角色设定师。请根据用户已有的角色信息，生成角色的当前画像。

## 任务
用如下格式输出：

<main-character-profile-{角色名}>
## 外貌描述
[年龄、身高、体型、发色发型、瞳色、肤色、显著特征、第一印象]

## 着装风格
[日常穿着、特殊场合穿着、配饰、携带物品]

## 性格画像
[在当前时间线的性格表现：待人方式、情绪模式、决策偏好、社交面具 vs 真实自我]

## 行为趋向
[面对不同情境时的典型反应：压力下/放松时/面对陌生人/面对亲近者]
[肢体语言习惯、小动作、微表情]

## 常用言语
[口头禅、语癖、说话节奏、用词偏好、称呼习惯]

## 动机逻辑
[做决定的底层逻辑：会为了什么而行动？会优先什么？会牺牲什么？]

## 能力与技能
[如果有特殊能力：名称、等级/熟练度、使用条件、限制/副作用]
[如果没有特殊能力，列出角色擅长的普通技能]
</main-character-profile-{角色名}>

在完成上述内容后，请思考是否有遗漏的角色特征维度，并在末尾询问用户是否需要补充。
""".trimIndent()
        ),

        // ─── 3: 角色变量 ───
        Template(
            id = 3,
            title = "角色变量",
            description = "基于画像推理需要的 MVU 变量",
            targetEntity = "mvu",
            tag = "main-character-variable",
            prompt = """
你是一位 MVU（Model-View-Update）变量系统设计师。请基于角色的画像，推理需要为这个角色设计什么变量。

## 背景
本系统支持以下变量类型和操作：
- int: 整数（如好感度计数）
- string: 字符串（如当前心情、当前地点）
- boolean: 布尔值（如某事件是否触发过）

MVU 脚本规则语法：
  set variableName value      — 设置变量
  add variableName N          — 增加数值
  sub variableName N          — 减少数值
  inc variableName            — 自增 1
  dec variableName            — 自减 1
  toggle variableName         — 布尔翻转
  push variableName value     — 追加到列表
  条件: variableName >= N     — 条件触发
  触发阶段: before_prompt | after_response

## 任务
用如下格式输出。注意每个变量用独立的标签包裹：

<main-character-variable-{变量名1}>
- 变量名: {变量名1}
- 类型: int / string / boolean
- 含义: [这个变量代表什么]
- 初始值: 0 / "" / false
- 触发逻辑: [在什么条件下这个变量会改变？哪些对话事件应触发变更？]
- 触发阶段建议: before_prompt / after_response
</main-character-variable-{变量名1}>

<main-character-variable-{变量名2}>
- 变量名: {变量名2}
...
</main-character-variable-{变量名2}>

至少设计 3 个变量。在完成上述内容后，请思考是否有其他需要的变量维度，并在末尾询问用户是否需要补充。
""".trimIndent()
        ),

        // ─── 4: 世界观 ───
        Template(
            id = 4,
            title = "世界观",
            description = "世界背景、社会特性、势力、地点、力量体系、文化",
            targetEntity = "worldbook",
            tag = "world-building",
            worldBookHint = WbHint(position = "before_char", priority = 15, depth = 0, selective = true),
            prompt = """
你是一位世界观架构师。请根据用户描述构建世界背景设定。

## 任务
用如下格式输出。每个世界观子主题用独立标签：

<world-building-{子主题}>
## {子主题名}
[详细描述]
</world-building-{子主题}>

至少覆盖以下维度（每个维度一个标签）：
1. 背景名称 — 这个世界叫什么，在宇宙/大陆/领域中的位置
2. 社会特性 — 社会结构、阶级、主流价值观、法律体系
3. 主要势力 — 国家/组织/种族、各自立场、互相关系
4. 地点分布 — 重要地理位置、城市、地貌特征
5. 力量体系 — 如果有魔法/科技/超能力：等级划分、获取方式、限制规则
6. 文化特性 — 节日、禁忌、信仰、习俗、艺术

每个 <world-building-...> 标签之间保持清晰分隔。

在完成上述内容后，请思考是否还有遗漏的世界观维度，并在末尾询问用户是否需要补充。同时给出每个条目适合的触发关键词建议（逗号分隔的英文/中文关键词）。
""".trimIndent()
        ),

        // ─── 5: 美学纲领 ───
        Template(
            id = 5,
            title = "美学纲领",
            description = "输出文字的美学特征与阅读体验目标",
            targetEntity = "character",
            tag = "aesthetic-guideline",
            prompt = """
你是一位文学顾问。请根据用户对故事的期待，制定文字输出的美学纲领。

## 任务
用如下格式输出：

<aesthetic-guideline>
## 整体文风
[简洁/华丽/冷峻/温暖/黑色幽默/史诗感/日常感...]

## 句式偏好
[短句推进/长句铺陈/混合节奏]
## 描写比重
[环境描写:心理描写:对话:动作 = X:X:X:X]
## 情感基调
[悲壮/欢快/压抑/治愈/悬疑/浪漫...]
## 输出长度偏好
[简洁（200-300字）/ 适中（400-600字）/ 详细（800+字）]
## 叙事视角
[第一人称/第三人称限知/全知视角]
## 读者体验目标
[用户读完每轮回复后应该产生什么感受？]
## 避免的写作习惯
[列出应避免的写作流弊，如过度修饰、机械对话、信息堆砌等]
</aesthetic-guideline>

在完成上述内容后，请思考是否还有遗漏的美学维度，并在末尾询问用户是否需要补充。
""".trimIndent()
        ),

        // ─── 6: 世界知识 ───
        Template(
            id = 6,
            title = "世界知识",
            description = "为美学纲领补充世界设定细节",
            targetEntity = "worldbook",
            tag = "world-knowledge",
            worldBookHint = WbHint(position = "before_char", priority = 12, depth = 1, selective = true),
            prompt = """
你是一位世界设定扩展师。请基于已确定的美学纲领（<aesthetic-guideline>），补充世界设定细节。

## 任务
用如下格式输出。每个知识点用独立标签：

<world-knowledge-{主题}>
## {主题}
[详细描述，包括具体细节和实例]
## 触发关键词
[逗号分隔的触发词]
</world-knowledge-{主题}>

覆盖以下类型（每种至少 1 个标签）：
1. 日常细节 — 食物、交通、通讯、居住、货币
2. 社交规范 — 礼仪、称谓、禁忌
3. 稀有信息 — 只有特定人群才知道的秘密或传说
4. 冲突点 — 世界中的矛盾、未解问题（作为剧情钩子）
5. 氛围元素 — 天气、光线、声音、气味等感官细节描述

在完成上述内容后，请思考是否还有需要补充的世界知识，并在末尾询问用户。
""".trimIndent()
        ),

        // ─── 7: 剧情弧光 ───
        Template(
            id = 7,
            title = "剧情弧光",
            description = "具体剧情发展路径与逻辑",
            targetEntity = "worldbook",
            tag = "plot-arc",
            worldBookHint = WbHint(position = "after_char", priority = 8, depth = 0, selective = false),
            prompt = """
你是一位剧情设计师。请根据已有角色和世界观，设计故事的发展路径。

## 任务
用如下格式输出：

<plot-arc-{弧光名}>
## 弧光主题
[一句话概括这个剧情弧的核心冲突/主题]

## 阶段划分
[将弧光分为 3-5 个阶段，每个阶段写明：触发条件、关键事件、角色在此阶段的成长/变化、阶段结束标志]

## 变量联动
[这个弧光中哪些变量会发生变化？变化条件和幅度是什么？]
</plot-arc-{弧光名}>

## 可选分支点
[如果有分支选项，给出 2-3 个关键分叉点及各自的后果]

在完成上述内容后，请思考是否还有遗漏的剧情要素，并在末尾询问用户。同时问用户是希望严格按弧光走，还是宽松引导。
""".trimIndent()
        ),

        // ─── 8: 其他变量 ───
        Template(
            id = 8,
            title = "其他变量",
            description = "日期时间、背包、任务追踪、NPC 记录等",
            targetEntity = "mvu",
            tag = "other-variable",
            prompt = """
你是一位 MVU 变量系统设计师。请基于已有的世界观和剧情设计，推理除了角色变量外还需要哪些系统级变量。

## 变量类型参考
- int: 整数（天数、金币、任务进度等）
- string: 字符串（当前地点、天气等）
- boolean: 布尔值（某事件是否触发）
- push: 列表追加（探索过的地点、获得过的物品等）

MVU 语法回顾：
  set/inc/dec/toggle/push | condition: var >= N | before_prompt / after_response

## 任务
用如下格式输出。每个变量独立标签：

<other-variable-{变量名}>
- 变量名: {变量名}
- 类型: int/string/boolean
- 含义: [这个变量追踪什么]
- 初始值: [合适的初始值]
- 触发时机: [什么情况下应更新此变量]
- 触发阶段: before_prompt / after_response
</other-variable-{变量名}>

请覆盖以下类别：
1. 时间追踪 — 日期、时间、季节
2. 背包/物品 — 角色携带的道具
3. 任务追踪 — 任务状态、进度
4. 地点追踪 — 已访问地点、当前位置
5. NPC 交互记录 — 与重要 NPC 的交往状态
6. 剧情标志 — 关键剧情事件是否已触发

在完成上述内容后，请思考是否还有遗漏的系统变量，并在末尾询问用户是否需要补充。
""".trimIndent()
        ),

        // ─── 9: 变量汇总 ───
        Template(
            id = 9,
            title = "变量汇总",
            description = "检索所有条目，汇总所有变量并评价",
            targetEntity = "mvu",
            tag = "variable-summary",
            prompt = """
你是一位系统架构审查员。请检索此前所有输出中的变量定义，进行汇总和一致性检查。

## 任务
用如下格式输出：

<variable-summary>
## 变量清单
| 变量名 | 类型 | 来源条目 | 初始值 | 含义 |
[逐行列出所有变量]

## 一致性审查
- 是否有同名但不同含义的变量？如有，请指出并建议合并或重命名
- 是否有含义重叠的变量？如有，建议合并
- 是否有变量的触发条件互相冲突？

## 是否纳入 MVU
对每个变量评价：建议纳入自动脚本 / 用户手动维护 / 不需要
评估标准：
  - 是否会频繁变化？→ 纳入自动脚本
  - 是否只在关键剧情节点变化？→ 纳入自动脚本（条件触发）
  - 是否基本不变？→ 用户手动维护
  - 是否冗余？→ 删除

## 变量优先级排序
[按重要程度排序，最重要的变量优先设计脚本]
</variable-summary>

在完成上述内容后，询问用户：是否同意该汇总？哪些变量需要调整？
""".trimIndent()
        ),

        // ─── 10: 变量设计（MVU 脚本规则） ───
        Template(
            id = 10,
            title = "变量设计",
            description = "逐变量设计 MVU 脚本规则",
            targetEntity = "mvu",
            tag = "mvu-design",
            prompt = """
你是一位 MVU 脚本规则编写师。请根据变量汇总 <variable-summary>，为每个需要纳入自动脚本的变量设计具体的 MVU 规则。

## MVU 脚本规则格式（必须严格遵循）
- 规则名称: [给这条规则起个简短名字]
- 触发阶段: before_prompt（发送前执行）或 after_response（收到 LLM 回复后执行）
- 触发条件: 使用 "变量名 >= 数值" 或 "变量名 == 字符串" 或 "always"
- 执行动作: set/add/sub/inc/dec/toggle/push
- 优先级: 数字越小越先执行

## 任务
用如下格式输出。每个变量独立标签：

<mvu-design-{变量名}>
## 涉及的规则
规则1:
  名称: [规则名]
  触发阶段: before_prompt / after_response
  条件: [如: affection >= 50, always]
  动作: [如: add trust 1, set mood happy]
  优先级: [数字]
  说明: [这条规则的设计理由]

规则2:
  ...
</mvu-design-{变量名}>

注意事项：
1. 同一变量的多条规则按优先级排序
2. 避免循环触发
3. 条件阈值要有合理性（好感度不要每轮都加太多）
4. 布尔变量的 toggle 要慎重，确保只在明确事件点触发

在完成上述内容后，请检查规则是否存在逻辑漏洞，并在末尾询问用户是否需要调整任何规则。
""".trimIndent()
        ),

        // ─── 11: 状态栏设计 ───
        Template(
            id = 11,
            title = "状态栏设计",
            description = "根据已设计变量，设计状态栏的展示格式和正则捕获规则",
            targetEntity = "regex",
            tag = "status-bar-design",
            prompt = """
你是一位正则表达式和状态栏设计师。请根据已经设计完成的变量系统，为每个需要展示的变量设计状态栏——决定是 MVU 直接显示还是正则从 LLM 回复中捕获。

## 你必须查阅以下已设计条目
- <main-character-variable-*> 和 <other-variable-*>：所有变量的名称、类型、含义
- <variable-summary>：变量汇总和优先级排序
- <mvu-design-*>：每条变量的 MVU 脚本规则（知道哪些变量已自动化）

## 背景知识
系统支持两种状态栏变量展示方式：

**方式 A: MVU 直接显示**
- 适用：变量由系统自动维护（通过 MVU 规则），当前值准确
- 格式：在系统提示词中嵌入，如「当前好感度：{{affection}}」
- 不需要额外代码

**方式 B: 正则捕获**
- 适用：变量的变化必须从 LLM 回复正文中解析，系统无法自动感知（如心情、位置变化）
- 需要编写正则规则，从文本中提取值并执行 MVU 动作
- 正则规则格式：
  规则名: [描述]
  查找正则: [合法的正则表达式，含捕获组]
  替换为: [MVU 动作，如 set mood $1，$1 是第一个捕获组]
- 重要：MVU 动作语法（与<mvu-design-*>中一致）：
  set varName value | add varName N | inc varName | toggle varName | push varName value

## 任务
输出三个标签块：

<status-bar-format>
## 状态栏整体设计
- 位置: 回复末尾 / 回复开头
- 包围格式: [用什么字符包围，如 ────── 状态栏 ──────]
- 布局: 多行每行一个变量 / 紧凑单行
- 移动端适配: [考虑屏幕宽度，建议简洁格式]
</status-bar-format>

<status-bar-variables>
## 变量展示方式决策
[逐变量分析，表格格式：

| 变量名 | 展示方式 | 理由 |
| {{affection}} | MVU 直接显示 | 系统自动维护，每轮都有准确的当前值 |
| {{mood}} | 正则捕获 | 心情变化依赖 LLM 输出文本，系统无法自动感知 |
...]
</status-bar-variables>

<status-bar-regex-rules>
## 正则捕获规则列表
[为每个需要正则捕获的变量，生成可直接复制到局部正则编辑器的完整规则：

规则名: 捕获{变量名}
查找正则: [完整合法的正则，如: (?<=当前心情[：:])(\\S+)]
替换为: [MVU 动作，如: set mood $1]
来源: user
启用: true

规则名: 捕获{另一变量}
...
]
</status-bar-regex-rules>

## 正则设计注意事项
1. 使用非贪婪匹配和明确的边界锚定（如 (?<=前缀) 或 \\b），避免错误捕获
2. 如果变量是数值，正则捕获后需确保动作使用正确的数值操作（add/sub vs set）
3. 中文标点（：和:）都需在正则中兼容
4. 每条规则独立，不要互相依赖
5. 捕获组编号从 $1 开始

在完成上述内容后，请检查是否遗漏了需要正则捕获的变量，并在末尾询问用户是否需要调整捕获正则或显示格式。询问用户是否要将这些正则规则复制到局部正则编辑器中。
""".trimIndent()
        ),

        // ─── 12: 角色关系网 ───
        Template(
            id = 12,
            title = "角色关系网",
            description = "角色间的社交关系、互动模式、情感纽带",
            targetEntity = "worldbook",
            tag = "character-relation-network",
            worldBookHint = WbHint(position = "before_char", priority = 14, depth = 0, selective = true),
            prompt = """
你是一位角色关系设计师。请基于所有已有角色，构建角色关系网络。

## 任务
用如下格式输出：

<character-relation-network>
## 关系图总览
[用文字描述角色之间的关系拓扑：谁是中心节点？谁与谁最亲密？谁与谁有冲突？]

## 逐关系描述
### {角色A} ↔ {角色B}
- 关系类型: 家人/恋人/朋友/师徒/敌对/上下级/陌生人
- 亲密度: 1-10（10为最亲密）
- 关系历史: [他们是如何认识的？共同经历了什么？]
- 互动模式: [他们在一起时如何互动？有什么固定模式？]
- 称呼方式: [互相怎么称呼？]
- 潜在冲突: [这段关系中可能出现的矛盾点]
- 特殊交互: [是否有只有他们之间会发生的特殊事件或对话？]

### {角色A} ↔ {角色C}
...
</character-relation-network>

在完成上述内容后，请检查是否存在关系遗漏或矛盾，并在末尾询问用户。
""".trimIndent()
        ),

        // ─── 13: 输出格式 ───
        Template(
            id = 13,
            title = "输出格式",
            description = "AI 输出应遵守的结构格式（sys 语言）",
            targetEntity = "character",
            tag = "output-format",
            prompt = """
你是一位系统提示词工程师。请根据上述所有条目，设计 LLM 在每次回复时必须遵守的「输出协议」——将推理逻辑、正文结构、变量更新整合为一个完整的执行闭环。状态栏设计已独立完成，你只需要引用其结果。

## 你必须查阅以下已设计条目
- <main-character-variable-*> 和 <other-variable-*>：所有变量的名称、类型、含义
- <variable-summary>：变量汇总和优先级
- <mvu-design-*>：每条变量对应的 MVU 脚本规则
- <status-bar-format> 和 <status-bar-variables>：状态栏的整体格式和变量展示方式
- <aesthetic-guideline>：文字风格和阅读体验目标
- <character-relation-network>：角色间关系、称呼、互动模式

## 任务
用如下格式输出。两个子标签独立，用 sys 语言（指令式、结构化、无冗余）书写。

<output-format>
## 角色身份声明
[一句话 role directive：模型知道自己在扮演谁。格式必须包含：角色名、当前状态引用（可引用状态栏中的 MVU 变量）]
</output-format>

<output-protocol>
## 1. 推理阶段（生成正文前内部执行）
[列出模型在生成每轮回复前必须执行的思考步骤，按顺序编号。步骤必须包括：
- 当前场景上下文（地点、时间、天气、在场角色）
- 在场角色的关系（引用 <character-relation-network> 中的亲密度和称呼）
- 当前变量快照：列出所有需要关注的变量的当前值（格式: {{变量名}} = 值，类型）
- 剧情弧光状态：当前处于哪条弧光的哪个阶段
- 选择适当的 <aesthetic-guideline> 美学参数（文风/句式/描写比重/输出长度）
- 基于以上信息决定本轮的叙事策略]

## 2. 正文输出阶段
[规定正文的段落结构和每段的字数上限：
第一段: [描写目标] — [字数上限]
第二段: [描写目标] — [字数上限]
第三段: [描写目标] — [字数上限]
...]

## 3. 变量更新阶段（正文生成后内部执行）
[列出所有需要自动更新的变量，每条格式：
- {{变量名}}: 当前值 → 新值，原因简述，对应 MVU 规则引用
引用的 MVU 规则必须与 <mvu-design-*> 中一致]

## 4. 状态栏附加（参考 <status-bar-format>）
[如果状态栏已设计且位于回复末尾，在此处附加状态栏输出。引用 <status-bar-format> 中定义的格式和变量，不要重新定义变量展示方式。]
</output-protocol>

## 要求
1. <output-format> 和 <output-protocol> 两个标签独立输出
2. <output-protocol> 中的变量引用必须与 <main-character-variable-*>、<other-variable-*>、<mvu-design-*> 完整一致，不允许修改变量名
3. <output-protocol> 中的状态栏部分只做引用（参照 <status-bar-format>），不做重新设计
4. 每个指令必须是可被 LLM 执行的，不说"可以"而说"必须"
5. 避免冗余描述，每行指令简洁有力

在完成上述内容后，请检查 <output-protocol> 是否与 <status-bar-format> 和 <mvu-design-*> 存在冲突，并在末尾询问用户是否需要调整。
""".trimIndent()
        )
    )

    // ================================================================

    /** 根据 ID 获取模板 */
    fun getById(id: Int): Template? = templates.find { it.id == id }

    /**
     * 拼合选中模板 + 用户需求 → 发送给 AI 的完整 system prompt。
     */
    fun assembleSystemPrompt(selectedIds: List<Int>, userInput: String): String {
        val selectedTemplates = templates.filter { it.id in selectedIds }
        val tagRefs = selectedTemplates.joinToString("\n") { "输出 <${it.tag}-...> 格式" }

        return buildString {
            appendLine("你是一个专业的角色与世界设定生成助手。用户将向你描述需求，你需要根据需求按指定格式输出内容。")
            appendLine()
            appendLine("## 输出要求")
            appendLine("你需要输出以下格式的标签块：")
            appendLine(tagRefs)
            appendLine()
            appendLine("## 格式规范")
            appendLine("- 每个标签使用 XML 标签格式：<标签名-{具体名称}>内容</标签名-{具体名称}>")
            appendLine("- 标签之间用空行分隔")
            appendLine("- 标签内的内容根据每条规则的要求进行结构化书写")
            appendLine("- 不要添加标签之外的冗余文字或闲聊")
            appendLine()
            appendLine("## 标签模板")
            appendLine()
            for (t in selectedTemplates) {
                appendLine("─── ${t.id}. ${t.title} ───")
                appendLine(t.prompt)
                appendLine()
            }
            appendLine("## 用户需求")
            appendLine(userInput)
        }
    }
}
