package com.example.expenseapi;

import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public Map<String, String> hello(
            @RequestParam(name = "name", defaultValue = "ban") String name,
            @RequestParam(name = "city", defaultValue = "Ha Noi") String city)

    {
        return Map.of("message", "Xin chao, " + name + "!", "city", city);
    }
}