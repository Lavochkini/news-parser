package com.lake_team.fistserios.controller.web;/*
  @author Bogdan
  @project fistserios
  @class AuthWebController
  @version 1.0.0
  @since 24.09.2025 - 16.47
*/

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthWebController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "registration";
    }

    @GetMapping("/main")
    public String mainPage() { return "main"; }

    @GetMapping("/dashboard")
    public String dashboardPage() { return "dashboard"; }

    @GetMapping("/my-dashboard")
    public String myDashboardPage() { return "my-dashboard"; }

    @GetMapping("/add-news")
    public String addNewsPage() { return "add-news"; }

    @GetMapping("/admin")
    public String adminPage() { return "admin"; }

    @GetMapping("/article/{id}")
    public String articlePage() { return "article"; }

    @GetMapping("/terms")
    public String termsPage() { return "terms"; }
}
