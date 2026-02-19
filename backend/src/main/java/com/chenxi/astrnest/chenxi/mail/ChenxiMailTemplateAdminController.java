package com.chenxi.astrnest.chenxi.mail;

import com.chenxi.astrnest.chenxi.mail.dto.ChenxiMailTemplateResponse;
import com.chenxi.astrnest.chenxi.mail.dto.TestMailTemplateRequest;
import com.chenxi.astrnest.chenxi.mail.dto.UpsertChenxiMailTemplateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/chenxi/mail-templates")
@RequiredArgsConstructor
public class ChenxiMailTemplateAdminController {

  private final ChenxiMailTemplateService templateService;

  @GetMapping
  public List<ChenxiMailTemplateResponse> list() {
    return templateService.listAll();
  }

  @PostMapping
  public ChenxiMailTemplateResponse create(
      @Valid @RequestBody UpsertChenxiMailTemplateRequest request,
      Authentication authentication
  ) {
    return templateService.create(request, resolveOperator(authentication));
  }

  @PutMapping("/{id}")
  public ChenxiMailTemplateResponse update(
      @PathVariable Long id,
      @Valid @RequestBody UpsertChenxiMailTemplateRequest request,
      Authentication authentication
  ) {
    return templateService.update(id, request, resolveOperator(authentication));
  }

  @DeleteMapping("/{id}")
  public Map<String, String> delete(@PathVariable Long id) {
    templateService.delete(id);
    return Map.of("message", "模板已删除");
  }

  @PostMapping("/{id}/test")
  public Map<String, String> test(
      @PathVariable Long id,
      @Valid @RequestBody TestMailTemplateRequest request
  ) {
    templateService.sendTest(id, request.targetEmail(), request.params());
    return Map.of("message", "测试邮件已发送");
  }

  private String resolveOperator(Authentication authentication) {
    return authentication != null ? authentication.getName() : "system";
  }
}
