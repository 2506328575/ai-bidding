package cn.iocoder.yudao.module.aibidding.controller;

import cn.iocoder.yudao.module.aibidding.service.llm.TemplateService;
import cn.iocoder.yudao.module.aibidding.service.llm.TemplateService.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/template")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/list")
    public Collection<TemplateRecord> list() {
        return templateService.listTemplates();
    }

    /** 获取原始模板文件（供编辑器标记占位符使用） */
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getTemplateFile(@PathVariable String id) {
        var record = templateService.getTemplate(id);
        if (record == null) return ResponseEntity.notFound().build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(record.getFileName()).build());
        return new ResponseEntity<>(record.getFileData(), headers, HttpStatus.OK);
    }

    @PostMapping("/upload")
    public TemplateRecord upload(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("上传模板: {}", file.getOriginalFilename());
        return templateService.upload(file);
    }

    @PostMapping("/{id}/placeholders")
    public TemplateRecord updatePlaceholders(@PathVariable String id,
                                              @RequestBody List<Placeholder> placeholders) {
        return templateService.updatePlaceholders(id, placeholders);
    }

    @PostMapping("/{id}/fill")
    public FillResult fill(@PathVariable String id, @RequestBody Map<String, String> body) throws Exception {
        String context = body.getOrDefault("context", "");
        log.info("填充模板: id={}", id);
        return templateService.fill(id, context);
    }

    @GetMapping("/file/{fileId}")
    public ResponseEntity<byte[]> getFile(@PathVariable String fileId) {
        byte[] data = templateService.getFile(fileId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
