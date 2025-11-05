package com.lake_team.fistserios.controller.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
    // Simple health-check endpoint
    @GetMapping("/ping")
    public String ping() { return "pong"; }
}
