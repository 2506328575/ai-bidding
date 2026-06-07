package cn.iocoder.yudao.module.aibidding.service.rag;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InMemoryKnowledgeService implements KnowledgeService {

    private final Map<String, List<DocumentChunk>> collections = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        index("case_library", List.of(
                chunk("C01", "XX制造集团ERP实施案例", "制造业", "案例",
                        "为XX制造集团实施了完整的ERP系统，涵盖生产管理/供应链/财务模块，交付周期6个月。"),
                chunk("C02", "XX银行数据中台项目", "金融", "案例",
                        "搭建银行统一数据中台，实现实时风控和客户画像分析，日均处理数据10TB。"),
                chunk("C03", "XX市政府信创云平台", "政府", "案例",
                        "基于国产化技术栈为XX市建设政务云平台，完成100+政务系统迁移，通过等保三级。"),
                chunk("C04", "XX医院HIS系统升级", "医疗", "案例",
                        "医疗行业三甲医院核心HIS系统升级项目，实现电子病历/预约挂号/医保对接，日门诊量5000+。"),
                chunk("C05", "XX大学智慧校园项目", "教育", "案例",
                        "建设智慧校园一卡通/教务管理/在线教学平台，服务3万师生。")
        ));

        index("product_docs", List.of(
                chunk("P01", "产品A技术白皮书（信创适配）", null, "白皮书",
                        "产品A已完成与麒麟OS/达梦数据库/东方通中间件的全面国产化适配，支持国产化信创部署，通过信创目录认证。"),
                chunk("P02", "产品B功能列表v3.2", null, "产品文档",
                        "支持高并发/分布式部署/多租户/数据权限控制，单节点TPS超5000。"),
                chunk("P03", "项目部署硬件要求", null, "产品文档",
                        "最低配置: 4C8G/100G SSD；推荐配置: 8C16G/500G SSD，支持K8s容器化部署。")
        ));

        index("bid_material_library", List.of(
                chunk("B01", "XX制造集团ERP二期合同（中标）", "制造业", "标书",
                        "中标金额580万，含3年运维服务。技术方案优势：模块化设计+快速定制开发。"),
                chunk("B02", "XX市政府信创云平台验收报告", "政府", "标书",
                        "项目验收评分98/100，被评为年度优秀政务信息化项目。")
        ));

        log.info("知识库初始化完成: {} 个集合, {} 篇文档",
                collections.size(),
                collections.values().stream().mapToInt(List::size).sum());
    }

    @Override
    public List<DocumentChunk> search(String query, String collection, int topK) {
        if (query == null || query.isBlank()) return List.of();

        List<DocumentChunk> pool;
        if (collection != null && !collection.isBlank()) {
            pool = collections.getOrDefault(collection, List.of());
        } else {
            pool = collections.values().stream()
                    .flatMap(List::stream).collect(Collectors.toList());
        }

        if (pool.isEmpty()) return List.of();

        // 使用 TF-IDF 关键词匹配模拟语义检索
        List<String> queryWords = tokenize(query);
        return pool.stream()
                .map(doc -> new AbstractMap.SimpleEntry<>(doc, score(queryWords, doc)))
                .filter(e -> e.getValue() >= 0.35)  // 最低阈值
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public void index(String collection, List<DocumentChunk> documents) {
        collections.computeIfAbsent(collection, k -> new ArrayList<>()).addAll(documents);
    }

    private double score(List<String> queryWords, DocumentChunk doc) {
        String metaStr = "";
        if (doc.getMetadata() != null) {
            metaStr = doc.getMetadata().values().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
        }
        String text = (doc.getTitle() + " " + doc.getContent() + " " + metaStr).toLowerCase();

        double totalWeight = 0;
        double hits = 0;
        for (String word : queryWords) {
            double weight = word.length() >= 2 ? 2.0 : 0.5;  // 长词权重高
            totalWeight += weight;
            if (text.contains(word)) {
                hits += weight;
            }
        }
        return totalWeight > 0 ? hits / totalWeight : 0;
    }

    private List<String> tokenize(String text) {
        // 混合分词: 按标点切分的长词 + 字级 token
        List<String> tokens = new ArrayList<>();
        // 长词（按中文标点+空格切分）
        String[] words = text.split("[，。、；：？！\\s]+");
        for (String w : words) {
            String trimmed = w.trim();
            if (trimmed.length() >= 2) tokens.add(trimmed);
        }
        // 单字 token（中文）
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isIdeographic(c)) {
                tokens.add(String.valueOf(c));
            }
        }
        return tokens.stream().filter(s -> s.length() > 0).distinct().collect(Collectors.toList());
    }

    private DocumentChunk chunk(String id, String title, String industry, String type, String content) {
        Map<String, String> meta = new LinkedHashMap<>();
        if (industry != null) meta.put("industry", industry);
        meta.put("type", type);
        return DocumentChunk.builder()
                .id(id).title(title).content(content)
                .source(title).metadata(meta).build();
    }
}
