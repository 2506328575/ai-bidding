package cn.iocoder.yudao.module.aibidding.service.llm;

import cn.iocoder.yudao.module.aibidding.service.prompt.PromptTemplateService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Slf4j
@Service
public class BidTemplateService {

    private final LlmService llmService;
    private final PromptTemplateService promptService;

    public BidTemplateService(LlmService llmService, PromptTemplateService promptService) {
        this.llmService = llmService;
        this.promptService = promptService;
    }

    /** 解析模板结构 + AI填充 + 返回填充后的docx */
    public FillResult fillTemplate(String templatePath, FillRequest req) {
        long start = System.currentTimeMillis();
        try (var is = Files.newInputStream(Path.of(templatePath))) {
            var doc = new XWPFDocument(is);
            List<TemplateSection> sections = extractSections(doc);
            log.info("模板解析: {} 个部分", sections.size());

            List<FilledSection> filled = new ArrayList<>();
            int totalTokens = 0;

            for (TemplateSection sec : sections) {
                String prompt = promptService.render("bid.fill_section", Map.of(
                    "sectionTitle", sec.getTitle(),
                    "industry", req.getIndustry(),
                    "projectInfo", req.getProjectInfo(),
                    "sectionPurpose", sec.getPurpose()
                ));
                try {
                    LlmResponse resp = llmService.call(LlmRequest.builder()
                        .systemPrompt("你是专业的商务标书撰写专家。")
                        .userMessage(prompt).maxTokens(2000).temperature(0.5).build()
                    );
                    String cleaned = stripAiPreamble(resp.getContent());
                    filled.add(new FilledSection(sec.getTitle(), cleaned, resp.getTokensUsed()));
                    totalTokens += resp.getTokensUsed();
                } catch (Exception e) {
                    filled.add(new FilledSection(sec.getTitle(),
                        "(生成失败: " + e.getMessage() + ")", 0));
                }
            }

            // 生成填充后的 docx + 保存到临时文件
            byte[] docxBytes = generateFilledDocx(doc, filled, req);
            String fileId = saveTempFile(docxBytes);

            return FillResult.builder()
                .fileId(fileId)
                .fileName(templatePath.contains("商务") ? "商务标_填充.docx" : "技术标_填充.docx")
                .sections(filled)
                .totalTokensUsed(totalTokens)
                .totalLatencyMs(System.currentTimeMillis() - start)
                .docxBase64(Base64.getEncoder().encodeToString(docxBytes))
                .build();

        } catch (Exception e) {
            log.error("模板填充失败", e);
            throw new RuntimeException("模板填充失败: " + e.getMessage(), e);
        }
    }

    /** 提取模板的章节结构 */
    private List<TemplateSection> extractSections(XWPFDocument doc) {
        List<TemplateSection> sections = new ArrayList<>();
        String currentTitle = "文档头部";
        StringBuilder currentPurpose = new StringBuilder();
        boolean hasContent = false;

        for (IBodyElement elem : doc.getBodyElements()) {
            if (elem instanceof XWPFParagraph) {
                var para = (XWPFParagraph) elem;
                String style = para.getStyle();
                String text = para.getText().trim();

                // 检测标题:
                // 1. style含heading/标题
                // 2. 中文数字序号开头的加粗短文本(如"一、收费公式")
                // 3. 以"报价"或"技术"开头的加粗短文本(如"报价一览表")
                boolean isHeadingStyle = style != null &&
                    (style.toLowerCase().contains("heading") || style.contains("标题"));
                boolean numberedBold = !text.isEmpty() && text.length() < 30
                    && text.matches("^[一二三四五六七八九十]、.+")
                    && para.getRuns() != null && !para.getRuns().isEmpty()
                    && para.getRuns().get(0).isBold();
                boolean looksLikeSectionTitle = !text.isEmpty() && text.length() < 30
                    && (text.endsWith("表") || text.contains("方案") || text.contains("说明"))
                    && para.getRuns() != null && !para.getRuns().isEmpty()
                    && para.getRuns().get(0).isBold();
                if (isHeadingStyle || numberedBold || looksLikeSectionTitle) {
                    if (hasContent) {
                        sections.add(new TemplateSection(currentTitle, currentPurpose.toString().trim()));
                    }
                    currentTitle = text.isEmpty() ? style : text;
                    currentPurpose = new StringBuilder();
                    hasContent = false;
                } else if (!text.isEmpty()) {
                    if (currentPurpose.length() < 500) {
                        currentPurpose.append(text).append(" ");
                    }
                    hasContent = true;
                }
            } else if (elem instanceof XWPFTable) {
                var table = (XWPFTable) elem;
                // 提取表格标题行作为 section purpose
                List<String> headers = new ArrayList<>();
                for (XWPFTableCell cell : table.getRow(0).getTableCells()) {
                    headers.add(cell.getText().trim());
                }
                currentPurpose.append(" [表格: ").append(String.join(" | ", headers)).append("] ");
                hasContent = true;
            }
        }
        // 最后一节
        if (hasContent && !currentTitle.isEmpty()) {
            sections.add(new TemplateSection(currentTitle, currentPurpose.toString().trim()));
        }

        if (sections.isEmpty()) {
            sections.add(new TemplateSection("标书正文", "商务标书完整内容"));
        }
        return sections;
    }

    /** 调用 Python fill_template.py 在原模板中注入 AI 内容（保留所有原始格式） */
    private byte[] generateFilledDocx(XWPFDocument template,
                                       List<FilledSection> filled,
                                       FillRequest req) throws IOException {
        // 构建 AI 内容映射 JSON
        var contentMap = new LinkedHashMap<String, String>();
        for (var fs : filled) contentMap.put(fs.getTitle(), fs.getContent());

        if (contentMap.isEmpty()) {
            var bos = new ByteArrayOutputStream();
            template.write(bos);
            return bos.toByteArray();
        }

        try {
            // 写 AI 内容到临时 JSON
            var tmpDir = Path.of(System.getProperty("java.io.tmpdir"), "aibidding");
            Files.createDirectories(tmpDir);
            var contentFile = tmpDir.resolve("bid_content.json");
            var outFile = tmpDir.resolve("bid_filled_" + System.currentTimeMillis() + ".docx");
            Files.writeString(contentFile,
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(contentMap));

            // 写原模板到临时文件
            var tmplFile = tmpDir.resolve("bid_template.docx");
            try (var bos = new ByteArrayOutputStream()) {
                template.write(bos);
                Files.write(tmplFile, bos.toByteArray());
            }

            // 调用 Python 脚本
            var script = getClass().getResourceAsStream("/fill_template.py");
            var scriptFile = tmpDir.resolve("fill_template.py");
            if (script != null) {
                Files.copy(script, scriptFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            var proc = new ProcessBuilder("python",
                scriptFile.toString(), "fill",
                tmplFile.toString(),
                contentFile.toString(),
                outFile.toString())
                .redirectErrorStream(true)
                .start();
            var output = new String(proc.getInputStream().readAllBytes());
            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                log.warn("Python填充失败(code={}): {}", exitCode, output);
                throw new IOException("Python填充失败: " + output);
            }

            byte[] result = Files.readAllBytes(outFile);
            log.info("Python填充完成: {} bytes", result.length);
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("填充被中断", e);
        }
    }

    private String stripAiPreamble(String text) {
        if (text == null) return "";
        return text
            .replaceFirst("(?i)^好的[，,].*?[。\\s]*", "")
            .replaceFirst("(?i)^作为.*?[,，].*?[。\\s]*", "")
            .strip();
    }

    // === Data classes ===
    @Data @lombok.AllArgsConstructor
    public static class TemplateSection {
        private String title;
        private String purpose;
    }
    @Data @lombok.AllArgsConstructor
    public static class FilledSection {
        private String title;
        private String content;
        private int tokensUsed;
    }
    @Data
    public static class FillRequest {
        private String industry;
        private String projectInfo;
        private String templateName;
    }
    // === 临时文件管理 ===
    public static final Map<String, byte[]> tempFiles = new LinkedHashMap<>();

    private String saveTempFile(byte[] data) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        tempFiles.put(id, data);
        return id;
    }

    public byte[] getFile(String fileId) {
        byte[] data = tempFiles.get(fileId);
        if (data == null) throw new IllegalArgumentException("文件不存在或已过期: " + fileId);
        return data;
    }

    @Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class FillResult {
        private String fileId;
        private String fileName;
        private List<FilledSection> sections;
        private long totalTokensUsed;
        private long totalLatencyMs;
        private String docxBase64;
    }
}
