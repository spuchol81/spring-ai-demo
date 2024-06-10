package com.tanzu.spring_ai_demo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/api/tts")
public class TextToSpeechController {
  private final Logger logger = LoggerFactory.getLogger(TextToSpeechController.class);
  private final OpenAiAudioSpeechModel ttsModel;
  private final ChatClient chatClient;
  @Autowired
  private Environment env;

  @Value("classpath:/movie-pitch-translation.st")
    private Resource moviePitch;


  public TextToSpeechController(OpenAiAudioSpeechModel ttsModel, @Qualifier("myChatClientProvider") ChatClient.Builder chatClientBuilder) {
    this.ttsModel = ttsModel;
    this.chatClient = chatClientBuilder.build();
  }

  @GetMapping("read")
  public ResponseEntity<byte[]> readPlot(@RequestParam(value = "plot", defaultValue = "Please enter a movie description!") String plot,
                                            @RequestParam(value = "lang", defaultValue = "") String lang) {
    OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
    .withVoice(OpenAiAudioApi.SpeechRequest.Voice.NOVA)
    .build();
    SpeechPrompt speechPrompt;
    if (lang == null || lang.isEmpty()) {
        speechPrompt = new SpeechPrompt(plot, speechOptions);
    }
    else{
        String translatedPlot = chatClient.prompt()
                              .user(p -> p.text(moviePitch).param("lang", lang)
                                                           .param("moviepitch", plot)
                                   )
                              .call()
                              .content()
                              .toString();
        speechPrompt = new SpeechPrompt(translatedPlot, speechOptions);  
    }
    SpeechResponse response = ttsModel.call(speechPrompt);
    byte[] responseAsBytes = response.getResult().getOutput();
    // Set the response headers
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_TYPE, "audio/mp3");  // Change this to "audio/mp3" if the audio is in MP3 format
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=audio.mp3");
    return new ResponseEntity<>(responseAsBytes, headers, HttpStatus.OK);
    }
    
}
