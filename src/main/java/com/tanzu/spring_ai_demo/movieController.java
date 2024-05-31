package com.tanzu.spring_ai_demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
      String[] propertiesToInclude = {"title", "imdbID", "plot", "poster"};
      var outputParser = new BeanOutputParser<>(Movie.class);
      SearchRequest query = SearchRequest.query(context).withTopK(8);
      //SearchRequest query = SearchRequest.query(context).withSimilarityThreshold(0.3).withTopK(8);
      List<Document> similarMovies = movieVectorStore.similaritySearch(query);
      List<Movie> movList = new ArrayList<Movie>();
      List<String> matchingmovies =
          similarMovies.stream().map(Document::getContent).collect(Collectors.toList());
      for (String currentmovie : matchingmovies) {
        HashMap<String, String> hashMap = parseStringToMap(currentmovie, propertiesToInclude);
        String curData = objectMapper.writeValueAsString(hashMap);
        movList.add(outputParser.parse(curData));
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

  private static HashMap<String, String> parseStringToMap(
      String dataString, String[] propertiesToInclude) {
    HashMap<String, String> map = new HashMap<>();

    // Define pattern to match key-value pairs
    Pattern pattern = Pattern.compile("\\{?([^,=]+)=([^,}]+)\\}?");
    Matcher matcher = pattern.matcher(dataString);

    // Iterate through matches and populate the map with specified properties
    while (matcher.find()) {
      String key = matcher.group(1).trim();
      String value = matcher.group(2).trim();
      for (String property : propertiesToInclude) {
        if (key.equals(property)) {
          map.put(key, value);
          break;
        }
      }
    }

    return map;
  }

  @Component
  public static class Movie {
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

    /*public void setMovieUrl(String movieUrl) {
      this.movieUrl = movieUrl;
    }*/
  }
}
