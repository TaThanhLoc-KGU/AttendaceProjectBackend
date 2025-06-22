package com.tathanhloc.faceattendance.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping(value = {"/", "/index", "/index.html"})
    public String index() {
        return "index";
    }
}