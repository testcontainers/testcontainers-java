package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfiguration.class)
public class DemoControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DemoRepository demoRepository;

    @Test
    public void getEntity() {
        DemoEntity demoEntity = demoRepository.save(DemoEntity.builder().name("demo").build());

        DemoEntity actual = restTemplate.getForEntity("/foo/" + demoEntity.getId(), DemoEntity.class)
            .getBody();

        assertThat(actual.getName()).isEqualTo("demo");
    }
}
