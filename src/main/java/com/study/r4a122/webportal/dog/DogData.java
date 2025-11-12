package com.study.r4a122.webportal.dog;

public record DogData(
    String message,
    String status,
    String errormessage) {
  public DogData withErrorMessaage(String newErrorMessage) {
    return new DogData(this.message, this.status, newErrorMessage);
  }
}
