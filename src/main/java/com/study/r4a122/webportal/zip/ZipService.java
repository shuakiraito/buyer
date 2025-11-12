package com.study.r4a122.webportal.zip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class ZipService {

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  /**
   * 郵便番号から住所を検索して ZipData に変換する
   */
  public ZipData execute(String zipcode) throws RestClientException, JsonProcessingException {
    // エンドポイントURL（クエリのスペースを削除）
    String URL = "https://zipcloud.ibsnet.co.jp/api/search?zipcode={zipcode}";

    // APIを呼び出して結果を取得
    String jsonResponse = restTemplate.getForObject(URL, String.class, zipcode);

    // JSONレスポンスをデータオブジェクトに変換
    return objectMapper.readValue(jsonResponse, ZipData.class);
  }
}
