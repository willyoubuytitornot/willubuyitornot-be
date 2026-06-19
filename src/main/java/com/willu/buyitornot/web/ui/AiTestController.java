package com.willu.buyitornot.web.ui;

import com.willu.buyitornot.service.GeminiService;
import com.willu.buyitornot.web.ui.common.WrapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final GeminiService geminiService;

    @GetMapping("/test")
    @WrapResponse
    public String testGemini(@RequestParam(defaultValue = "안녕하세요! 자기소개 해주세요.") String question) {
        return geminiService.chat(question);
    }
}
