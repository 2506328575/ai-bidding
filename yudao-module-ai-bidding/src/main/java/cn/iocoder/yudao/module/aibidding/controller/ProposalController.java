package cn.iocoder.yudao.module.aibidding.controller;

import cn.iocoder.yudao.module.aibidding.service.llm.ProposalService;
import cn.iocoder.yudao.module.aibidding.service.llm.ProposalService.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/proposal")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping("/generate")
    public ProposalResult generate(@RequestBody ProposalRequest request) {
        log.info("方案生成: industry={}", request.getIndustry());
        return proposalService.generate(request);
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@RequestBody Map<String, String> body) {
        String base64 = body.get("docxBase64");
        if (base64 == null) return ResponseEntity.badRequest().build();

        byte[] data = Base64.getDecoder().decode(base64);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("proposal.docx").build());
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
