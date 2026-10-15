package org.dsqrwym.langora;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Text {
    @GetMapping("/hello")
    public static String hello() {
        return "Hello";
    }
}
