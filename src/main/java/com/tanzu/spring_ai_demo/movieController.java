package com.tanzu.spring_ai_demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.parser.BeanOutputParser;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import com.tanzu.spring_ai_demo.Movie;

public class movieController {

  public static void main(String[] args) {
    SpringApplication.run(movieController.class, args);
  }

  @RestController
  @RequestMapping("/api")
  public static class ApiController {
    private final Logger logger = LoggerFactory.getLogger(ApiController.class);
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private List<Movie> movies;
    private final SimpleVectorStore movieVectorStore;

    @Value("classpath:static/imdb.json")
    private Resource movieResource;

    public ApiController(
        ResourceLoader resourceLoader,
        ObjectMapper objectMapper ,
        SimpleVectorStore movieVectorStore) {
      this.resourceLoader = resourceLoader;
      this.objectMapper = objectMapper;
      this.movies = loadMoviesFromJson(); // Load movies from JSON file
      this.movieVectorStore = movieVectorStore;

    }

    // Load movies from JSON file
    private List<Movie> loadMoviesFromJson() {
      try {
        Resource resource = resourceLoader.getResource("classpath:static/imdb.json");
        Movie[] movies = objectMapper.readValue(resource.getInputStream(), Movie[].class);
        return List.of(movies);
      } catch (IOException e) {
        e.printStackTrace();
        return new ArrayList<>();
      }
    }

    @GetMapping("/movies")
    public ResponseEntity<List<Movie>> getAllMovies() {
      return new ResponseEntity<>(movies, HttpStatus.OK);
    }

    @GetMapping("/movies/search")
    public ResponseEntity<List<Movie>> getmatchingMovies(
        @RequestParam(value = "context", defaultValue = "All the movies") String context)
        throws JsonProcessingException {
      SearchRequest query = SearchRequest.query(context).withTopK(6);
      //SearchRequest query = SearchRequest.query(context).withSimilarityThreshold(0.2).withTopK(8);
      List<Document> similarMovies = movieVectorStore.similaritySearch(query);
      List<Movie> movList = new ArrayList<Movie>();
      List<String> matchingmovies =
          similarMovies.stream().map(Document::getContent).collect(Collectors.toList());
      for (String currentmovie : matchingmovies) {
        Movie curMovie = mapToMovie(currentmovie);
        movList.add(curMovie);
      }
      return new ResponseEntity<>(movList, HttpStatus.OK);
    }

    @GetMapping("/movies/{imdbID}")
    public Optional<Movie> getMovieByImdbId(@PathVariable String imdbID) {
      return movies.stream().filter(movie -> movie.getImdbID().equals(imdbID)).findFirst();
    }

    @GetMapping("/movies/{imdbID}/title")
    public String getMovieTitleImdbId(@PathVariable String imdbID) {
      return movies.stream()
          .filter(movie -> movie.getImdbID().equals(imdbID))
          .findFirst()
          .get()
          .getTitle();
    }

    @GetMapping("/movies/{imdbID}/plot")
    public String getMoviePlotImdbId(@PathVariable String imdbID) {
      return movies.stream()
          .filter(movie -> movie.getImdbID().equals(imdbID))
          .findFirst()
          .get()
          .getPlot();
    }
  }


  public static Movie mapToMovie(String movieString) {
    Map<String, String> movieMap = new HashMap<>();
    String[] pairs = movieString.substring(1, movieString.length() - 1).split(", ");

    for (String pair : pairs) {
        String[] keyValue = pair.split("=", 2); // Specify limit to avoid exception
        movieMap.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : ""); // Handle empty value if there's no "=" in the pair
    }

    Movie movie = new Movie();
    movie.setImdbID(movieMap.get("imdbID"));
    movie.setTitle(movieMap.get("title"));
    movie.setPlot(movieMap.get("plot"));
    movie.setPoster(movieMap.get("poster"));
    // Set other attributes as needed

    return movie;
}
}
