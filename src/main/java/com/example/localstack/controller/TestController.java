package com.example.localstack.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Testing APIs")
@SecurityRequirements({@SecurityRequirement(name = "bearerAuth")})
public class TestController {

    @GetMapping("/test")
    @Operation(summary = "Test API")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Secured test API");
    }
}
