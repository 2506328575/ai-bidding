package cn.iocoder.yudao.module.aibidding.service.llm;

import cn.iocoder.yudao.module.aibidding.service.prompt.PromptTemplateService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Slf4j
@Service
public class ProposalService {

    private final LlmService llmService;
    private final PromptTemplateService promptService;

    public ProposalService(LlmService llmService, PromptTemplateService promptService) {
        this.llmService = llmService;
        this.promptService = promptService;
    }

    public ProposalResult generate(ProposalRequest req) {
        long start = System.currentTimeMillis();

        // Step 1: 生成大纲
        String outlinePrompt = promptService.render("proposal.outline", Map.of(
            "industry", req.getIndustry(),
            "scale", req.getScale(),
            "painPoints", req.getPainPoints(),
            "budget", req.getBudget()
        ));
        String outline = llmService.call(LlmRequest.builder()
            .systemPrompt("你是专业的售前方案架构师。")
            .userMessage(outlinePrompt).maxTokens(500).temperature(0.5).build()
        ).getContent();

        List<Section> sections = parseOutline(outline);

        // Step 2: 逐节生成内容
        String previousSummary = "";
        for (Section sec : sections) {
            String sectionPrompt = promptService.render("proposal.section", Map.of(
                "sectionTitle", sec.getTitle(),
                "sectionKeyPoints", sec.getKeyPoint(),
                "industry", req.getIndustry(),
                "scale", req.getScale(),
                "painPoints", req.getPainPoints(),
                "previousSummary", previousSummary
            ));
            try {
                LlmResponse resp = llmService.call(LlmRequest.builder()
                    .systemPrompt("你是专业的售前方案架构师。")
                    .userMessage(sectionPrompt).maxTokens(3000).temperature(0.7).build()
                );
                String cleaned = stripAiPreamble(resp.getContent());
                sec.setContent(cleaned);
                int endIdx = (int) Math.min(200, cleaned.length());
                previousSummary = resp.getContent().substring(0, endIdx);
                sec.setTokensUsed(resp.getTokensUsed());
            } catch (Exception e) {
                sec.setContent("(本节内容生成失败: " + e.getMessage() + ")");
            }
        }

        // Step 3: 生成 Word 文件
        byte[] docxBytes = generateDocx(req, sections);

        long totalTokens = sections.stream().mapToInt(Section::getTokensUsed).sum();

        return ProposalResult.builder()
            .title(req.getIndustry() + "售前技术方案")
            .outline(outline)
            .sections(sections)
            .totalTokensUsed(totalTokens)
            .totalLatencyMs(System.currentTimeMillis() - start)
            .docxBase64(Base64.getEncoder().encodeToString(docxBytes))
            .build();
    }

    private byte[] generateDocx(ProposalRequest req, List<Section> sections) {
        try (var bos = new ByteArrayOutputStream()) {
            var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();

            // 封面标题
            var titlePara = doc.createParagraph();
            titlePara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            var titleRun = titlePara.createRun();
            titleRun.setText(req.getIndustry() + " 售前技术方案");
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            // 基本信息
            doc.createParagraph();
            var info = doc.createParagraph().createRun();
            info.setText("客户痛点: " + req.getPainPoints() + "    预算: " + req.getBudget());
            info.setFontSize(10);
            info.setColor("666666");
            doc.createParagraph();

            // 各章节 — 解析 Markdown 格式
            for (Section sec : sections) {
                // 章节大标题
                var heading = doc.createParagraph().createRun();
                heading.setText(sec.getTitle());
                heading.setBold(true);
                heading.setFontSize(16);
                heading.setColor("1e6fff");
                doc.createParagraph();

                // 解析 Markdown 正文
                renderMarkdownContent(doc, sec.getContent());
                doc.createParagraph(); // 节后空行
            }

            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.warn("Word生成失败，降级为纯文本: {}", e.getMessage());
            var sb = new StringBuilder();
            sb.append(req.getIndustry()).append(" 售前技术方案\n\n");
            for (var s : sections) sb.append(s.getTitle()).append("\n\n").append(s.getContent()).append("\n\n");
            return sb.toString().getBytes();
        }
    }

    /** 将 Markdown 格式文本渲染为 Word 段落（支持 # ## ### ** - 等） */
    private void renderMarkdownContent(
            org.apache.poi.xwpf.usermodel.XWPFDocument doc, String text) {
        for (String line : text.split("\n")) {
            line = line.strip();
            if (line.isEmpty()) {
                doc.createParagraph(); // 空行
                continue;
            }
            var para = doc.createParagraph();
            para.setSpacingAfter(80);

            // # 系列标题（支持 # ~ ######）
            if (line.startsWith("# ")) {
                var run = para.createRun();
                run.setText(line.replaceFirst("^#+\\s", "").strip());
                run.setBold(true);
                run.setFontSize(16);
                run.setColor("1e6fff");
                continue;
            }
            if (line.startsWith("## ")) {
                var run = para.createRun();
                run.setText(line.replaceFirst("^##+\\s", "").strip());
                run.setBold(true);
                run.setFontSize(14);
                continue;
            }
            if (line.startsWith("### ")) {
                var run = para.createRun();
                run.setText(line.replaceFirst("^###+\\s", "").strip());
                run.setBold(true);
                run.setFontSize(13);
                continue;
            }
            if (line.startsWith("#### ")) {
                var run = para.createRun();
                run.setText(line.replaceFirst("^####+\\s", "").strip());
                run.setBold(true);
                run.setFontSize(12);
                continue;
            }
            if (line.startsWith("##### ")) {
                var run = para.createRun();
                run.setText(line.replaceFirst("^#####+\\s", "").strip());
                run.setBold(true);
                run.setFontSize(11);
                continue;
            }
            // - 或 * 无序列表
            if (line.matches("^[-*]\\s.*")) {
                para.setIndentationLeft(400);
                renderMixedFormat(para, line.replaceFirst("^[-*]\\s", "• "));
                continue;
            }
            // 1. 有序列表
            if (line.matches("^\\d+[\\.\\)]\\s.*")) {
                para.setIndentationLeft(400);
                renderMixedFormat(para, line);
                continue;
            }
            // 普通段落（可能含 **bold** 内联）
            renderMixedFormat(para, line);
        }
    }

    /** 处理行内 **粗体** 格式 */
    private void renderMixedFormat(
            org.apache.poi.xwpf.usermodel.XWPFParagraph para, String line) {
        var parts = line.split("(\\*\\*)");
        boolean bold = false;
        for (String part : parts) {
            var run = para.createRun();
            run.setText(part);
            run.setFontSize(11);
            if (bold) run.setBold(true);
            bold = !bold;
        }
    }

    private List<Section> parseOutline(String outline) {
        List<Section> sections = new ArrayList<>();
        for (String line : outline.split("\n")) {
            line = line.trim();
            if (line.matches("^\\d+[\\.\\、].*")) {
                String[] parts = line.split("[:：]", 2);
                String title = parts[0].replaceFirst("^\\d+[\\.\\、]\\s*", "");
                String keyPoint = parts.length > 1 ? parts[1].trim() : "";
                sections.add(new Section(title, keyPoint));
            }
        }
        if (sections.isEmpty()) {
            sections.add(new Section("项目概述", "项目背景与建设目标"));
            sections.add(new Section("需求分析", "客户痛点与需求解读"));
            sections.add(new Section("总体方案", "技术架构与实施路线"));
            sections.add(new Section("产品选型", "核心产品配置说明"));
            sections.add(new Section("实施计划", "项目排期与资源规划"));
            sections.add(new Section("服务保障", "运维与培训支持"));
        }
        return sections;
    }

    private String stripAiPreamble(String text) {
        // 去掉 AI 常见的元话语开头
        if (text == null) return "";
        return text
            .replaceFirst("(?i)^好的[，,]\\s*作为.*?[,，]\\s*我将.*?撰写.*?章节[。\\s]*", "")
            .replaceFirst("(?i)^好的[，,]\\s*我是.*?[,，]\\s*.*?\\s*", "")
            .replaceFirst("(?i)^作为(一名)?售前.*?[,，]\\s*我将.*?[。\\s]*", "")
            .replaceFirst("(?i)^根据(前文|您).*?[,，]\\s*(本章|本节|现在).*?[。\\s]*", "")
            .replaceFirst("(?i)^好的[，,].*?(?:以下|如下).*?[:：]?\\s*", "")
            .strip();
    }

    @Data
    public static class ProposalRequest {
        private String industry;
        private String scale;
        private String painPoints;
        private String budget;
    }

    @Data
    public static class Section {
        private String title;
        private String keyPoint;
        private String content;
        private int tokensUsed;

        public Section(String title, String keyPoint) {
            this.title = title;
            this.keyPoint = keyPoint;
            this.content = "";
        }
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProposalResult {
        private String title;
        private String outline;
        private List<Section> sections;
        private long totalTokensUsed;
        private long totalLatencyMs;
        private String docxBase64;
    }
}
