package cn.iocoder.yudao.module.aibidding.service.llm;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.data.Texts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Slf4j
@Service
public class WordTemplateService {

    public byte[] fillTemplate(String templatePath, Map<String, String> data) throws IOException {
        Path path = Path.of(templatePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("模板文件不存在: " + templatePath);
        }
        try (InputStream is = Files.newInputStream(path);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XWPFTemplate template = XWPFTemplate.compile(is).render(data);
            template.writeAndClose(bos);
            log.info("模板填充完成: {} ({} bytes)", templatePath, bos.size());
            return bos.toByteArray();
        }
    }

    public List<String> extractPlaceholders(String templatePath) throws IOException {
        Path path = Path.of(templatePath);
        if (!Files.exists(path)) throw new FileNotFoundException("模板不存在: " + templatePath);

        // 对于客户提供的原始 docx（非 POI-TL 模板），返回空列表
        // 后续系统会要求模板提供方按规范添加 {{placeholder}} 标签
        log.info("模板占位符提取（当前仅支持 POI-TL 标签格式）: {}", templatePath);
        return List.of();
    }
}
