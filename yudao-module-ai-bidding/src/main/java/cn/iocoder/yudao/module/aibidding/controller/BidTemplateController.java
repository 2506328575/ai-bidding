package cn.iocoder.yudao.module.aibidding.controller;

import cn.iocoder.yudao.module.aibidding.service.llm.BidTemplateService;
import cn.iocoder.yudao.module.aibidding.service.llm.BidTemplateService.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/bid")
public class BidTemplateController {

    private final BidTemplateService bidTemplateService;

    public BidTemplateController(BidTemplateService bidTemplateService) {
        this.bidTemplateService = bidTemplateService;
    }

    /** 列出可用模板 */
    @GetMapping("/templates")
    public List<Map<String, String>> listTemplates() {
        return List.of(
            Map.of("name", "商务标——模板(1).docx", "label", "商务标模板", "size", "55KB"),
            Map.of("name", "技术标——模板(1).docx", "label", "技术标模板", "size", "947KB")
        );
    }

    /** 基于模板生成标书内容 */
    @PostMapping("/fill")
    public FillResult fill(@RequestBody FillRequest request) {
        String templateName = request.getTemplateName() != null
            ? request.getTemplateName() : "商务标——模板(1).docx";
        String templatePath = "../Ai招投标模板/" + templateName;
        log.info("标书填充: template={}", templateName);
        return bidTemplateService.fillTemplate(templatePath, request);
    }

    /** 获取生成的文件（供在线编辑器加载） */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<byte[]> getFile(@PathVariable String fileId) {
        byte[] data = bidTemplateService.getFile(fileId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.inline().filename("bid.docx").build());
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    /** 下载填充后的 docx */
    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@RequestBody Map<String, String> body) {
        String base64 = body.get("docxBase64");
        if (base64 == null) return ResponseEntity.badRequest().build();
        byte[] data = Base64.getDecoder().decode(base64);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("bid_filled.docx").build());
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
