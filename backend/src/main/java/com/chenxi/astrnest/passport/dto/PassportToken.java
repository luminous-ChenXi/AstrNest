package com.chenxi.astrnest.passport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 通行证 /oauth2/token 端点的标准响应（OAuth 2.1）。
 * 本站只消费 access_token（用完即弃，不缓存、不刷新），其余字段仅用于日志。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PassportToken(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Long expiresIn,
    String scope) {
}
