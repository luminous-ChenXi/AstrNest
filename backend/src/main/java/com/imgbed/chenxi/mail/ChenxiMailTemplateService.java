package com.imgbed.chenxi.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imgbed.chenxi.mail.dto.ChenxiMailTemplateResponse;
import com.imgbed.chenxi.mail.dto.UpsertChenxiMailTemplateRequest;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    ChenxiMailTemplate template = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
    apply(request, operator, template);
    ChenxiMailTemplate saved = repository.save(template);
    return toResponse(saved);
  }

  @Transactional
  public void delete(Long id) {
    if (!repository.existsById(id)) {
      return;
    }
    repository.deleteById(id);
  }

  public void sendTest(Long id, String targetEmail, Map<String, String> params) {
    ChenxiMailTemplate template = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
    mailService.sendTemplateMail(targetEmail, template, params == null ? Collections.emptyMap() : params);
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
