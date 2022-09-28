package com.example;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoService {

    private final DemoRepository demoRepository;

    public DemoService(final DemoRepository demoRepository) {
        this.demoRepository = demoRepository;
    }

    @Transactional(readOnly = true)
    public DemoEntity getEntity(final Long id) {
        return demoRepository.findById(id)
            .orElseThrow();
    }
}
