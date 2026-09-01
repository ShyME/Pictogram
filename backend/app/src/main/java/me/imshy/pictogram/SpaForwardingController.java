package me.imshy.pictogram;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SpaForwardingController {

    private static final String RESERVED_PREFIXES = "api|actuator|oauth2|v3";

    @GetMapping({
        "/{route:(?!(?:" + RESERVED_PREFIXES + ")$)[^.]+}",
        "/u/{username:[^.]+}",
        "/u/{username:[^.]+}/{list:followers|following}"
    })
    String forwardToSpaShell() {
        return "forward:/index.html";
    }
}
