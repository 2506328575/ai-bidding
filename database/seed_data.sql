-- AI 招投标系统 — 种子数据

-- Prompt 模板
INSERT INTO prompt_template (`key`, version, content, description, status) VALUES
('intent.classify', 1,
 '判断用户问题意图，只返回以下之一：PRODUCT_QUERY | CASE_RETRIEVAL | POLICY_QUERY | DATA_QUERY | GREETING\n\n示例：\n"有没有制造业案例" → CASE_RETRIEVAL\n"系统支持信创吗" → PRODUCT_QUERY\n"广州社保基数" → POLICY_QUERY\n"我名下几个项目" → DATA_QUERY\n\n用户问题：{{question}}',
 '意图分类', 1),
('qa.rag.answer', 1,
 '基于以下资料回答问题。如果资料不足以回答，请说"当前案例库暂无相关信息"。\n\n参考资料：\n{{retrieved_docs}}\n\n问题：{{question}}\n\n要求：回答末尾标注引用来源。',
 'RAG问答', 1),
('proposal.outline', 1,
 '你是售前方案专家。根据以下客户需求生成方案大纲（6-8个章节），每章含一句话要点说明。\n\n客户需求:\n行业: {{industry}}\n规模: {{scale}}\n痛点: {{painPoints}}\n预算: {{budget}}\n\n输出格式（纯文本，每行一个章节）:\n1. 章节名: 要点',
 '方案大纲规划', 1),
('proposal.section', 1,
 '你是售前方案专家。请为方案中【{{sectionTitle}}】章节撰写专业内容。\n\n客户需求:\n行业: {{industry}}\n规模: {{scale}}\n痛点: {{painPoints}}\n\n本章要点: {{sectionKeyPoints}}\n前文摘要: {{previousSummary}}\n\n⚠️ 严禁事项:\n1. 不要写"好的"、"作为售前专家"、"根据前文"等元话语\n2. 不要复述本章要点，直接写正文\n3. 不要用"本章将..."开头\n4. 直接以章节内容的实质信息开始\n\n要求: 800-1200字、专业具体、结合实际行业特点、段落格式',
 '方案单节撰写', 1),
('bid.fill_section', 1,
 '你正在撰写一份商务标书的【{{sectionTitle}}】部分。\n\n客户需求背景:\n行业: {{industry}}\n项目概况: {{projectInfo}}\n\n本部分要求: {{sectionPurpose}}\n\n⚠️ 严禁事项:\n1. 不要写"好的"、"作为标书撰写专家"、"根据要求"等元话语\n2. 直接输出本部分的内容，不要用"本部分将..."开头\n3. 保持商务标书的正式、专业语气\n4. 涉及数据处保留适当灵活性',
 '标书章节填充', 1),
('policy.analyze', 1,
 '从以下政府公告中提取结构化政策信息。\n\n公告内容：{{content}}\n\n输出JSON格式：\n{"省": "", "市": "", "类别": "社保/公积金/...", "生效日期": "", "内容摘要": ""}',
 '政策抓取解析', 1);

-- 需求采集维度
INSERT INTO requirement_dimension (name, data_type, enum_values, sort_order, is_required, prompt_hint, is_fixed, status) VALUES
('行业', 'ENUM', '["制造业","金融","政府","医疗","教育","互联网","其他"]', 1, 1, '请问贵司主要属于哪个行业？', 1, 1),
('规模', 'ENUM', '["50人以下","50-200人","200-500人","500-2000人","2000人以上"]', 2, 1, '请问贵司的人员规模？', 1, 1),
('核心痛点', 'TEXTAREA', NULL, 3, 1, '请描述当前面临的核心痛点', 1, 1),
('预算范围', 'ENUM', '["50万以下","50-200万","200-500万","500万以上"]', 4, 1, '请问项目预算范围？', 1, 1),
('交付时限', 'DATE', NULL, 5, 1, '请问预期交付时间？', 1, 1),
('已有系统', 'TEXT', NULL, 6, 0, '请问现有IT系统情况？', 0, 1),
('技术栈偏好', 'TEXT', NULL, 7, 0, '对技术栈有特殊偏好吗？', 0, 1);

-- 标书格式校验规则
INSERT INTO bid_format_rule (rule_name, check_target, check_method, rule_config, severity) VALUES
('正文字体', 'FONT', 'PROPERTY_READ', '{"font":"宋体","size":12}', 'WARN'),
('标题字体', 'FONT', 'PROPERTY_READ', '{"font":"黑体","size":16}', 'WARN'),
('页边距', 'MARGIN', 'RANGE_CHECK', '{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17}', 'WARN'),
('页数限制', 'PAGE_NUM', 'RANGE_CHECK', '{"max":200}', 'ERROR'),
('目录完整性', 'TOC', 'EXISTENCE_CHECK', '{}', 'ERROR'),
('公章位置', 'SEAL', 'EXISTENCE_CHECK', '{"page":"last"}', 'ERROR');
