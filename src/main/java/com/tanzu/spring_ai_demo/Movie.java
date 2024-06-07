package com.tanzu.spring_ai_demo;

import org.springframework.stereotype.Component;

@Component
public class Movie {
  private String imdbID;
  private String title;
  private String plot;
  private String poster;

  public Movie() {}

  public String getImdbID() {
    return imdbID;
  }

  public String getTitle() {
    return title;
  }

  public String getPlot() {
    return plot;
  }

  public String getPoster() {
    return poster;
  }

  public void setPoster(String poster) {
    this.poster = poster;
  }
  public void setImdbID(String imdbID) {
    this.imdbID = imdbID;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setPlot(String plot) {
    this.plot = plot;
  }
}
