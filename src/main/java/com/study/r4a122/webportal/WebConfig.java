package com.study.r4a122.webportal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {

  // 出力ディレクトリのパス（ファイル名なし）
  public static final String OUTPUT_PATH = "/path/to/output/directory"; // 適宜変更してください

  // ファイル名だけ
  public static final String FILENAME_TASK_CSV = "output.csv";

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    // Java 8日時型（LocalDateTime等）をサポートするモジュールを登録
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }
}
