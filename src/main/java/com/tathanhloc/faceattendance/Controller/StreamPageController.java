package com.tathanhloc.faceattendance.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller  // Chú ý: @Controller không phải @RestController
public class StreamPageController {

    @GetMapping("/stream")
    public String streamPage() {
        return "stream"; // Trả về stream.html
    }

}