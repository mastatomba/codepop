package nl.schoutens.codepop.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
  private final ChatClient chatClient;

  public ChatController(ChatClient.Builder builder) {
    this.chatClient = builder.defaultAdvisors(new SimpleLoggerAdvisor()).build();
  }

  @PostMapping("/ask")
  Output chat(@RequestBody @Valid Input input) {
    String response = chatClient.prompt(input.prompt()).call().content();
    return new Output(response);
  }

  record Input(@NotBlank String prompt) {}

  record Output(String response) {}
}
