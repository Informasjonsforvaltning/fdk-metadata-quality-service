package no.fdk.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationStatusController {

    @GetMapping("/ping")
    private ResponseEntity<Void> ping() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ready")
    private ResponseEntity<Void> ready() {
        return ResponseEntity.ok().build();
    }

}
