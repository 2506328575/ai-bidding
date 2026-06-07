package cn.iocoder.yudao.module.aibidding.service.llm;

import cn.iocoder.yudao.module.aibidding.service.prompt.PromptTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

@Slf4j
@Service
public class TemplateService {

    private final LlmService llmService;
    private final PromptTemplateService promptService;

    public TemplateService(LlmService llmService, PromptTemplateService promptService) {
        this.llmService = llmService;
        this.promptService = promptService;
    }

    private static final Map<String, TemplateRecord> store = new LinkedHashMap<>();

    /** 上传模板 + 提取占位符 */
    public TemplateRecord upload(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String id = UUID.randomUUID().toString().substring(0, 8);

        // 提取占位符
        List<Placeholder> placeholders = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        try (var is = new ByteArrayInputStream(bytes)) {
            var doc = new XWPFDocument(is);
            for (XWPFParagraph para : doc.getParagraphs()) {
                extractFromText(para.getText(), seen, placeholders);
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        extractFromText(cell.getText(), seen, placeholders);
                    }
                }
            }
        }
        // AI 自动推断每个占位符的含义
        for (Placeholder p : placeholders) {
            p.setDescription(guessDescription(p.getName()));
        }

        var record = new TemplateRecord();
        record.setId(id);
        record.setFileName(file.getOriginalFilename());
        record.setFileData(bytes);
        record.setPlaceholders(placeholders);
        store.put(id, record);
        log.info("模板上传成功: {} ({} bytes, {} 占位符)", id, bytes.length, placeholders.size());
        return record;
    }

    private void extractFromText(String text, Set<String> seen, List<Placeholder> out) {
        Matcher m = Pattern.compile("\\{\\{(.+?)}}").matcher(text);
        while (m.find()) {
            String name = m.group(1).trim();
            if (!name.startsWith("_") && seen.add(name)) {
                out.add(new Placeholder(name, "", ""));
            }
        }
    }

    private String guessDescription(String name) {
        // 根据占位符名称推断含义
        return switch (name) {
            case "项目名称", "projectName" -> "项目的完整名称";
            case "客户名称", "客户", "clientName" -> "客户公司名称";
            case "报价总价", "总价", "totalPrice" -> "项目总报价金额";
            case "报价明细", "报价", "priceDetail" -> "分项报价明细";
            case "技术方案", "技术", "techSolution" -> "技术方案描述";
            case "实施计划", "计划", "timeline" -> "项目实施时间计划";
            case "日期", "date" -> "填写日期";
            case "公司名称", "公司", "供应商" -> "供应商公司全称";
            case "联系人", "签字人" -> "负责人姓名";
            default -> name.length() > 2 ? "请填写 " + name : "待定义";
        };
    }

    /** 更新占位符定义 */
    public TemplateRecord updatePlaceholders(String templateId, List<Placeholder> placeholders) {
        var record = store.get(templateId);
        if (record == null) throw new IllegalArgumentException("模板不存在: " + templateId);
        record.setPlaceholders(placeholders);
        return record;
    }

    /** AI 填充模板 */
    public FillResult fill(String templateId, String context) throws Exception {
        var record = store.get(templateId);
        if (record == null) throw new IllegalArgumentException("模板不存在: " + templateId);

        // 为每个占位符生成 AI 内容
        Map<String, String> fillMap = new LinkedHashMap<>();
        int totalTokens = 0;

        for (Placeholder p : record.getPlaceholders()) {
            String prompt = String.format(
                "你正在填写一份文档的【%s】字段。\n字段含义: %s\n项目背景: %s\n\n" +
                "请直接输出要填入的内容，不要加任何解释、引号或元话语。",
                p.getName(), p.getDescription(), context
            );
            try {
                LlmResponse resp = llmService.call(LlmRequest.builder()
                    .systemPrompt("你是专业的文档撰写助手，输出简洁、准确、专业。")
                    .userMessage(prompt).maxTokens(500).temperature(0.3).build()
                );
                fillMap.put(p.getName(), resp.getContent().strip());
                totalTokens += resp.getTokensUsed();
            } catch (Exception e) {
                fillMap.put(p.getName(), "[生成失败: " + e.getMessage() + "]");
            }
        }

        // 用 Java POI 做占位符替换；若模板无占位符则在末尾追加填充内容
        byte[] filledBytes;
        boolean hasMarkers = record.getFileData() != null &&
            new String(record.getFileData(), 0, Math.min(500, record.getFileData().length)).contains("{{");
        if (hasMarkers) {
            filledBytes = fillDocxInJava(record.getFileData(), fillMap);
        } else {
            filledBytes = appendFillContent(record.getFileData(), fillMap, context);
        }

        String fileId = UUID.randomUUID().toString().substring(0, 8);
        BidTemplateService.tempFiles.put(fileId, filledBytes);

        FillResult result = new FillResult();
        result.setFileId(fileId);
        result.setFileName(record.getFileName().replace(".docx", "_填充.docx"));
        result.setFilledFields(fillMap);
        result.setTotalTokens(totalTokens);
        result.setDocxBase64(Base64.getEncoder().encodeToString(filledBytes));
        return result;
    }

    /** Java POI 直接替换 {{placeholder}}，保留所有格式 */
    private byte[] fillDocxInJava(byte[] templateBytes, Map<String, String> fillMap) throws IOException {
        try (var is = new ByteArrayInputStream(templateBytes);
             var doc = new XWPFDocument(is)) {

            // 替换段落中的占位符
            for (XWPFParagraph para : doc.getParagraphs()) {
                replaceInParagraph(para, fillMap);
            }
            // 替换表格中的占位符
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph para : cell.getParagraphs()) {
                            replaceInParagraph(para, fillMap);
                        }
                    }
                }
            }

            var bos = new ByteArrayOutputStream();
            doc.write(bos);
            return bos.toByteArray();
        }
    }

    /** 模板无占位符时 — 在文档末尾追加 AI 填充内容 */
    private byte[] appendFillContent(byte[] templateBytes, Map<String, String> fillMap, String context) throws IOException {
        try (var is = new ByteArrayInputStream(templateBytes);
             var doc = new XWPFDocument(is)) {

            doc.createParagraph(); // 空行
            var heading = doc.createParagraph().createRun();
            heading.setText("=== AI 填充内容（基于需求上下文） ===");
            heading.setBold(true); heading.setFontSize(14);

            var ctx = doc.createParagraph().createRun();
            ctx.setText("需求背景: " + context);
            ctx.setFontSize(10); ctx.setColor("666666");
            doc.createParagraph();

            for (var entry : fillMap.entrySet()) {
                var title = doc.createParagraph().createRun();
                title.setText("【" + entry.getKey() + "】");
                title.setBold(true); title.setFontSize(12);

                var body = doc.createParagraph().createRun();
                body.setText(entry.getValue());
                body.setFontSize(11);
                doc.createParagraph();
            }

            var bos = new ByteArrayOutputStream();
            doc.write(bos);
            return bos.toByteArray();
        }
    }

    private void replaceInParagraph(XWPFParagraph para, Map<String, String> fillMap) {
        for (var entry : fillMap.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            // 检查段落文本中是否含占位符
            if (!para.getText().contains(placeholder)) continue;

            // 逐个 run 替换
            List<XWPFRun> runs = para.getRuns();
            if (runs == null || runs.isEmpty()) continue;

            // 先合并所有 run 文本
            StringBuilder full = new StringBuilder();
            for (XWPFRun r : runs) full.append(r.text());
            String replaced = full.toString().replace(placeholder, value);

            // 清空所有 run，写回第一个
            for (int i = 0; i < runs.size(); i++) {
                runs.get(i).setText(i == 0 ? replaced : "", 0);
            }
        }
    }

    public byte[] getFile(String fileId) {
        byte[] data = BidTemplateService.tempFiles.get(fileId);
        if (data == null) throw new IllegalArgumentException("文件不存在: " + fileId);
        return data;
    }

    public TemplateRecord getTemplate(String id) {
        return store.get(id);
    }

    public Collection<TemplateRecord> listTemplates() {
        return store.values();
    }

    @Data
    public static class TemplateRecord {
        private String id;
        private String fileName;
        private byte[] fileData;
        private List<Placeholder> placeholders;
    }

    @Data @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class Placeholder {
        private String name;
        private String description;
        private String filledValue;
    }

    @Data
    public static class FillResult {
        private String fileId;
        private String fileName;
        private Map<String, String> filledFields;
        private int totalTokens;
        private String docxBase64;
    }
}
