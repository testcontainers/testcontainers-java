package com.example;

import org.springframework.stereotype.Service;

@Service
public class DemoService {
    private final DemoRepository demoRepository;

    public DemoService(DemoRepository demoRepository) {
        this.demoRepository = demoRepository;
    }


    public DemoEntity getDemoEntity(Long id) {
        return demoRepository.findById(id)
            .orElseThrow(() ->new RuntimeException("Entity not found"));
    }
}
