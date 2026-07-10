package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class WeatherPeriodEmbeddable {

  @Column(length = 64)
  private String precipitation;

  @Column(length = 64)
  private String windStrength;

  @Column
  private Integer temperatureC;

  @Column
  private Integer temperatureF;

  public WeatherPeriodEmbeddable() {}

  public String getPrecipitation() {
    return precipitation;
  }

  public void setPrecipitation(String precipitation) {
    this.precipitation = precipitation;
  }

  public String getWindStrength() {
    return windStrength;
  }

  public void setWindStrength(String windStrength) {
    this.windStrength = windStrength;
  }

  public Integer getTemperatureC() {
    return temperatureC;
  }

  public void setTemperatureC(Integer temperatureC) {
    this.temperatureC = temperatureC;
  }

  public Integer getTemperatureF() {
    return temperatureF;
  }

  public void setTemperatureF(Integer temperatureF) {
    this.temperatureF = temperatureF;
  }
}
