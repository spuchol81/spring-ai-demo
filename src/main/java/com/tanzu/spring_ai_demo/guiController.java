package com.tanzu.spring_ai_demo;

import com.tanzu.spring_ai_demo.Movie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Controller
public class guiController {

  private static final String MOVIE_API_URL = "http://localhost:8080/api/movies";
  private static final String GEN_API_URL = "http://localhost:8080/api/poster/make";

  @GetMapping("/")
  public String index(Model model) {
    RestTemplate restTemplate = new RestTemplate();
    Movie[] movies = restTemplate.getForObject(MOVIE_API_URL, Movie[].class);
    model.addAttribute("movies", movies);
    return "index";
  }

  @GetMapping("/movie/{imdbID}")
  public String movieDetails(Model model, @PathVariable String imdbID) {
    RestTemplate restTemplate = new RestTemplate();
    Movie movie = restTemplate.getForObject(MOVIE_API_URL + "/" + imdbID, Movie.class);
    model.addAttribute("movie", movie);
    return "movie-details";
  }

  @GetMapping("/movie/generate")
  public String generatedmovieDetails(
      Model model,
      @RequestParam(value = "movie1", defaultValue = "tt0050613") String movie1,
      @RequestParam(value = "movie2", defaultValue = "tt0111161") String movie2) {
    RestTemplate restTemplate = new RestTemplate();
    Movie movie =
        restTemplate.getForObject(
            GEN_API_URL + "?movie1=" + movie1 + "&movie2=" + movie2, Movie.class);
    model.addAttribute("movie", movie);
    return "movie-details";
  }
}
