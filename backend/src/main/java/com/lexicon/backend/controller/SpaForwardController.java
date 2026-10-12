package com.lexicon.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    // Internally forwards requests to index.html when users open or refresh URLs directly,
    // since SPA routes have no corresponding backend endpoints.
    @GetMapping({"/dictionaries", "/dictionaries/**"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}