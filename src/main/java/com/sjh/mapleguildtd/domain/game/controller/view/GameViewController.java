package com.sjh.mapleguildtd.domain.game.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/games")
public class GameViewController {

    @GetMapping
    public String gamePage() {
        return "game";
    }

    @GetMapping("/result")
    public String resultPage() {
        return "result";
    }
}
