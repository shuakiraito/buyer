package com.study.r4a122.webportal.dog;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * ワンちゃん画像取得の業務ロジッククラスを実現するクラスです。
 * 本機能は、ワンちゃん画像取得APIを内部で呼び出して結果を表示します。
 * 仕様については、下記のドキュメントを参照してください。
 * https://dog.ceo/dog-api/documentation/random
 *
 * @author田中来翔
 */
@Service
public class DogService {
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public DogService() {
    // RestTemplate and ObjectMapper configuration
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Prevents exceptions on unknown
                                                                                      // fields in JSON
  }

  public DogData execute() throws RestClientException, JsonProcessingException {
    // The URL to fetch the dog image
    final String URL = "https://dog.ceo/api/breeds/image/random";

    // API call to fetch the JSON response
    String jsonResponse = restTemplate.getForObject(URL, String.class);

    // Map the JSON response to the DogData object
    DogData dogData = objectMapper.readValue(jsonResponse, DogData.class);
    return dogData;
  }
}
