package com.study.r4a122.webportal.dog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;

@Controller
public class DogController {
  @Autowired
  private DogService dogService;

  @GetMapping("dog")
  public String getDog(Model model) {
    try {
      DogData data = dogService.execute();
      model.addAttribute("data", data);
      return "dog/result";
    } catch (RestClientException | JsonProcessingException e) {
      model.addAttribute("errorMessage", "検索に失敗しました。");

      return "index";
    }
  }
}
