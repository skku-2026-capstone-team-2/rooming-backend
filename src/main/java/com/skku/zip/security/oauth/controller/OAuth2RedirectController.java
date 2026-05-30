package com.skku.zip.security.oauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OAuth2RedirectController {
    @GetMapping("/oauth2/redirect")
    public String oauth2Redirect(
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) Boolean profileComplete,
            Model model) {

        model.addAttribute("accountType", accountType);
        model.addAttribute("profileComplete", profileComplete);

        return "oauth2-redirect";
    }
}
