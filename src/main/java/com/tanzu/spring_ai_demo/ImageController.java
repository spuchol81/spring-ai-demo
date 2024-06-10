package com.tanzu.spring_ai_demo;

import com.tanzu.spring_ai_demo.Movie;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;


@RestController
@RequestMapping("/api/poster")
public class ImageController {
  private final Logger logger = LoggerFactory.getLogger(ImageController.class);
  private final ImageModel imageModel;
  private final ChatClient chatClient;
  private static final String MOVIE_API_URL = "http://localhost:8080/api/movies";
  @Autowired
  private Environment env;

  @Value("classpath:/movie-pitch-prompt-multi-modal.st")
    private Resource moviePromptRes;
  
    @Value("classpath:/movie-pitch.st")
    private Resource moviePitch;

  public ImageController(ImageModel imageModel, @Qualifier("myChatClientProvider") ChatClient.Builder chatClientBuilder) {
    this.imageModel = imageModel;
    this.chatClient = chatClientBuilder.build();
  }

  @GetMapping("make")
  public ResponseEntity<Movie> transcribe(
      @RequestParam(value = "movie1", defaultValue = "tt0050613") String movie1,
      @RequestParam(value = "movie2", defaultValue = "tt0111161") String movie2) {

    RestTemplate restTemplate = new RestTemplate();
    Movie movie1obj = restTemplate.getForObject(MOVIE_API_URL + "/" + movie1, Movie.class);
    Movie movie2obj = restTemplate.getForObject(MOVIE_API_URL + "/" + movie2, Movie.class);

    record MovieDescription(
            String movie1PosterDescription,
            String movie2PosterDescription) {
    }
   //logger.info("chatmodel version: {}",env.getProperty("spring.ai.openai.chat.options.model"));
   // call the AI model to generate a new movie plot an title based on the parent movies plot and title.
   Movie genMovie = chatClient.prompt()
                              .user(p -> p.text(moviePitch).param("plot1", movie1obj.getPlot())
                                                           .param("plot2", movie2obj.getPlot())
                                                           .param("title1", movie1obj.getTitle())
                                                           .param("title2", movie2obj.getTitle())
                                   )
                              .call()
                              .entity(Movie.class);

   // call the AI model to describe the parent movies posters  
   MovieDescription moviedesc = chatClient.prompt()
                                          .user(p -> {
                                            try {
                                              p.text("""
                                                                Describe what you see on these images
                                                                Return description, nothing else.
                                                                """)
                                                         .media(MimeTypeUtils.IMAGE_JPEG, new URI(movie1obj.getPoster()).toURL())
                                                         .media(MimeTypeUtils.IMAGE_JPEG, new URI(movie2obj.getPoster()).toURL());
                                            } catch (MalformedURLException e) {
                                              // TODO Auto-generated catch block
                                              e.printStackTrace();
                                            } catch (URISyntaxException e) {
                                              // TODO Auto-generated catch block
                                              e.printStackTrace();
                                            }
                                          })
                                          .call()
                                          .entity(MovieDescription.class);
                                          logger.info("Movie 1 desc: {}",moviedesc.movie1PosterDescription);


   PromptTemplate imagePromptTemplate =
          new PromptTemplate(
              """
              You are the illustrator of the poster for a new movie.
              New movie title and plot will be provided, which has been generated from a mix of two original movies titles and plots.
              the two original movie posters description will be provided.
              Using the title, plot and original poster descriptions as inspiration, generate the new movie poster.
              Take care of not generating any violence.

              ##### new movie information
              * title: {gentitle}
              * plot: {genplot}

              ##### Original posters descriptions
              * Original movie1 Poster Description: {Description1}
              * Original movie2 Poster Description: {Description2}
              This title has to appear clearly on the poster            
                """);
      imagePromptTemplate.add("gentitle", genMovie.getTitle());
      imagePromptTemplate.add("genplot", genMovie.getPlot());
      imagePromptTemplate.add("Description1", moviedesc.movie1PosterDescription);
      imagePromptTemplate.add("Description2", moviedesc.movie2PosterDescription);
      String imagePrompt = imagePromptTemplate.create().toString();
      logger.info("the following prompt will be used{}", imagePrompt);
      ImageResponse poster =
        imageModel.call(
              new ImagePrompt(
                  imagePrompt,
                  OpenAiImageOptions.builder()
                      .withQuality("standard")
                      .withN(1)
                      .withHeight(1792)
                      .withWidth(1024)
                      .build()));
      String imageUrl = poster.getResult().getOutput().getUrl();

      genMovie.setPoster(imageUrl);
      return new ResponseEntity<>(genMovie, HttpStatus.OK);

   
  }

  
}
