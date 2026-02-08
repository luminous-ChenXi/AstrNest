package com.chenxi.astrnest.chenxi.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chenxi.astrnest.chenxi.mail.dto.ChenxiMailTemplateResponse;
import com.chenxi.astrnest.chenxi.mail.dto.UpsertChenxiMailTemplateRequest;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class ChenxiMailTemplateService {

  private final ChenxiMailTemplateRepository repository;
  private final ChenxiMailService mailService;
  private final ObjectMapper objectMapper;

  public List<ChenxiMailTemplateResponse> listAll() {
    return repository
        .findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))
        .stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public ChenxiMailTemplateResponse create(UpsertChenxiMailTemplateRequest request, String operator) {
    ChenxiMailTemplate template = new ChenxiMailTemplate();
    apply(request, operator, template);
    ChenxiMailTemplate saved = repository.save(template);
    return toResponse(saved);
  }

  @Transactional
  public ChenxiMailTemplateResponse update(Long id, UpsertChenxiMailTemplateRequest request, String operator) {
    Long templateId = Objects.requireNonNull(id, "模板 ID 不能为空");
    UpsertChenxiMailTemplateRequest payload = Objects.requireNonNull(request, "模板请求不能为空");
    ChenxiMailTemplate template = repository.findById(templateId)
        .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
    apply(payload, operator, template);
    ChenxiMailTemplate saved = repository.save(Objects.requireNonNull(template));
    return toResponse(saved);
  }

  @Transactional
  public void delete(Long id) {
    Long templateId = Objects.requireNonNull(id, "模板 ID 不能为空");
    if (!repository.existsById(templateId)) {
      return;
    }
    repository.deleteById(templateId);
  }

  public void sendTest(Long id, String targetEmail, Map<String, String> params) {
    Long templateId = Objects.requireNonNull(id, "模板 ID 不能为空");
    String recipient = Objects.requireNonNull(targetEmail, "测试邮箱不能为空");
    ChenxiMailTemplate template = repository.findById(templateId)
        .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
    mailService.sendTemplateMail(recipient, template, params == null ? Collections.emptyMap() : params);
  }

  private void apply(UpsertChenxiMailTemplateRequest request, String operator, ChenxiMailTemplate template) {
    template.setName(request.name());
    template.setType(request.type());
    template.setSubject(request.subject());
    template.setContent(request.content());
    template.setVariablesJson(encodeVariables(request.variables()));
    template.setUpdatedBy(operator);
  }

  private ChenxiMailTemplateResponse toResponse(ChenxiMailTemplate template) {
    return new ChenxiMailTemplateResponse(
        template.getId(),
        template.getName(),
        template.getType(),
        template.getSubject(),
        template.getContent(),
        decodeVariables(template.getVariablesJson()),
        template.getCreatedAt(),
        template.getUpdatedAt()
    );
  }

  private String encodeVariables(List<String> variables) {
    List<String> source = CollectionUtils.isEmpty(variables) ? List.of() : variables;
    try {
      return objectMapper.writeValueAsString(source);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("无法序列化模板变量", ex);
    }
  }

  private List<String> decodeVariables(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("无法解析模板变量", ex);
    }
  }
}
