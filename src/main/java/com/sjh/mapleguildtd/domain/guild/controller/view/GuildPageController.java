package com.sjh.mapleguildtd.domain.guild.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/guild")
public class GuildPageController {

    @GetMapping
    public String indexPage() {
        return "index";
    }

    @GetMapping("/members")
    public String guildPage() {
        return "guild";
    }


}
