package com.chenxi.astrnest.tag;

import com.chenxi.astrnest.tag.dto.ChenxiTagResponse;
import com.chenxi.astrnest.tag.dto.CreateChenxiTagRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','API_CLIENT','USER')")
public class ChenxiTagController {

  private final ChenxiTagService chenxiTagService;

  @GetMapping
  public List<ChenxiTagResponse> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer limit
  ) {
    return chenxiTagService.search(keyword, limit);
  }

  @PostMapping
  public ChenxiTagResponse create(@Valid @RequestBody CreateChenxiTagRequest request) {
    return chenxiTagService.create(request.name(), request.description());
  }
}
