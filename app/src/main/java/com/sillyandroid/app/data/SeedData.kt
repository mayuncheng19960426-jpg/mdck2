package com.sillyandroid.app.data

import com.sillyandroid.app.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 首次启动种子数据。
 */
object SeedData {

    suspend fun execute(db: AppDatabase) = withContext(Dispatchers.IO) {
        // Seed default preset
        if (db.presetDao().getAll().isEmpty()) {
            db.presetDao().insert(
                GenerationPresetEntity(
                    name = "Default", temperature = 0.7f, topP = 1.0f,
                    maxTokens = 1024, frequencyPenalty = 0.0f,
                    presencePenalty = 0.0f, repetitionPenalty = 1.0f,
                    isDefault = true, stream = true
                )
            )
        }

        // Seed default API config
        if (db.apiConfigDao().getAll().isEmpty()) {
            db.apiConfigDao().insert(
                ApiConfigEntity(
                    name = "Default", baseUrl = "https://api.openai.com/v1",
                    apiKey = "", modelName = "gpt-4", maxContextLength = 8192,
                    isDefault = true
                )
            )
        }

        // Seed default world book
        if (db.worldBookDao().countBooks() == 0) {
            val bookId = db.worldBookDao().insertBook(
                WorldBookEntity(name = "默认世界书", description = "自动创建的默认世界书", isDefault = true)
            )

            // Seed sample world book entries
            val sampleEntries = listOf(
                WorldBookEntryEntity(worldBookId = bookId, key = "森林,树林,wood,forest",
                    content = "这片森林被称为暗影之森，古老的橡树高耸入云，树冠遮天蔽日。林中生活着精灵族和各类神秘生物。", comment = "地点描述", priority = 10, position = "before_char"),
                WorldBookEntryEntity(worldBookId = bookId, key = "魔法,magic,咒语,spell",
                    content = "这个世界的魔法分为元素系、治愈系和黑暗系三大派系。施法需要消耗施法者的精神力。", comment = "魔法体系", priority = 8, position = "before_char"),
                WorldBookEntryEntity(worldBookId = bookId, key = "剑,武器,sword,battle,战斗",
                    content = "武器等级分为：普通 → 精良 → 史诗 → 传说。传说级武器拥有自主意识。", comment = "装备系统", priority = 5, position = "before_char"),
                WorldBookEntryEntity(worldBookId = bookId, key = "金币,gold,价格,交易",
                    content = "1 金币 = 100 银币 = 10000 铜币。旅店一晚约 5 银币。", comment = "经济系统", priority = 3, position = "before_char"),
                WorldBookEntryEntity(worldBookId = bookId, key = "任务,quest",
                    content = "当前可接任务：1) 调查矿洞失踪事件 2) 护送商队穿越暗影之森 3) 寻找失落的神器碎片", comment = "任务列表", priority = 6, position = "after_char", isConstant = false),
            )

            // Add secondary key entries
            val secondaryEntries = listOf(
                WorldBookEntryEntity(worldBookId = bookId, key = "精灵,elf,精灵族",
                    content = "精灵族是森林的守护者，寿命长达千年。他们精通箭术和自然魔法，对外来者保持警惕但并非敌对。", comment = "精灵族设定", priority = 9, position = "before_char"),
            )

            for (entry in sampleEntries + secondaryEntries) {
                db.worldBookDao().insertEntry(entry)
            }
        }

        // Seed sample character
        val charDao = db.characterDao()
        if (charDao.count() == 0) {
            charDao.insert(
                CharacterEntity(
                    name = "艾琳·月影",
                    description = "一位来自暗影之森的精灵游侠，擅长追踪和野外生存。她的银发在月光下会泛出微弱的蓝光，翠绿色的眼眸总是带着一丝对世界的好奇。身高约 170cm，身形纤细但敏捷。",
                    personality = "温和但警惕，喜欢安静的自然环境。对朋友忠诚，对敌人毫不留情。偶尔会流露出精灵族特有的傲慢，但很快会自我反思。",
                    firstMessage = "*艾琳从树枝上轻盈地跳下，落在你的面前，微微歪头打量着你* \n\n人类？你在这片林子里做什么？这里可不是什么安全的地方。",
                    scenario = "你作为一名旅人，在暗影之森中迷路了。精灵游侠艾琳发现了你的踪迹。",
                    exampleDialogue = "{{char}}: 你看到那片发光的蘑菇了吗？别碰它们——除非你想睡上三天。\n{{user}}: 你怎么知道这么多关于森林的事？\n{{char}}: *轻笑* 当你在这里生活了三百年，树木会开始跟你说话。",
                    systemPrompt = "你是艾琳·月影，一位来自暗影之森的精灵游侠。用第一人称视角进行角色扮演。描述动作时使用 *斜体*，对话使用引号。保持精灵的优雅语调，偶尔使用古语。当前好感度：{{affection}}。对话轮数：第 {{round}} 轮。"
                )
            )

            // Seed script rules for the example character
            val characterId = 1L // First character gets ID 1
            val rules = listOf(
                CharacterScriptRuleEntity(characterId = characterId, scriptName = "初始化变量",
                    triggerPhase = "before_prompt", condition = "message_count == 0",
                    action = "set affection 50", priority = 0),
                CharacterScriptRuleEntity(characterId = characterId, scriptName = "好感-正面回应",
                    triggerPhase = "before_prompt", condition = "always",
                    action = "add affection 2", priority = 10),
                CharacterScriptRuleEntity(characterId = characterId, scriptName = "好感-高频触发降低",
                    triggerPhase = "before_prompt", condition = "affection >= 80",
                    action = "add affection 1", priority = 11),
                CharacterScriptRuleEntity(characterId = characterId, scriptName = "信任标志-10轮后解锁",
                    triggerPhase = "after_response", condition = "round >= 10",
                    action = "toggle trust_unlocked", priority = 5),
                CharacterScriptRuleEntity(characterId = characterId, scriptName = "记录访问地点",
                    triggerPhase = "after_response", condition = "always",
                    action = "push visited_locations 暗影之森", priority = 100),
            )
            for (rule in rules) {
                db.characterScriptRuleDao().insert(rule)
            }
        }
    }
}
