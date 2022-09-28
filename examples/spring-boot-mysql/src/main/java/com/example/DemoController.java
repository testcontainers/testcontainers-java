package com.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    private final DemoService demoService;

    public DemoController(final DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/foo/{id}")
    public ResponseEntity<DemoEntity> getEntity(@PathVariable final Long id) {
        return ResponseEntity.ok(demoService.getEntity(id));
    }
}
