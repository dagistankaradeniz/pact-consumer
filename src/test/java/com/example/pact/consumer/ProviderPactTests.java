package com.example.pact.consumer;

import au.com.dius.pact.consumer.*;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.RequestResponsePact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ProviderPactTests {

    @Test
    public void createPerson() throws JsonProcessingException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);

        ObjectMapper objectMapper = new ObjectMapper();
        Person person = Person.builder().name("Roger Antonsen").ssn("71039012345").build();

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer("pact-consumer")
                .hasPactWith("pact-provider")
                .uponReceiving("Create new person")
                    .path("/person")
                    .method("POST")
                    .headers(headers)
                    .body(objectMapper.writeValueAsString(person))
                .willRespondWith()
                    .headers(headers)
                    .status(HttpStatus.CREATED.value())
                    .body(new PactDslJsonBody()
                        .stringValue("name", "Roger Antonsen") // Strict value
                        .stringValue("ssn", "71039012345") // Strict value
                        .integerType("id", 0) // Value not important, but strict type
                    )
                .toPact();

        MockProviderConfig config = MockProviderConfig.createDefault();

        PactVerificationResult pactVerificationResult = runConsumerTest(pact, config, mockProvider -> {
            Person response = new ProviderClient(new ProviderConfig(mockProvider.getUrl())).createPerson(person);

            assertEquals(response.getName(), person.getName());
            assertEquals(response.getSsn(), person.getSsn());
            assertTrue(response.getId() != null);
        });

        assertEquals(PactVerificationResult.Ok.INSTANCE, pactVerificationResult);
    }

    @Test
    public void getPersonAndValidateFields() throws JsonProcessingException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);

        ObjectMapper objectMapper = new ObjectMapper();
        Person person = Person.builder().name("Roger Antonsen").ssn("71039012345").build();

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer("pact-consumer").hasPactWith("pact-provider")
                .given("Person with SSN(71039012345) exists")
                .uponReceiving("Get person by SSN and validate fields")
                    .path("/person/"+person.getSsn())
                    .method("GET")
                .willRespondWith()
                    .headers(headers)
                    .status(HttpStatus.OK.value())
                    .body(new PactDslJsonBody()
                        .stringType("name", "Roger Antonsen") // Strict value
                        .stringValue("ssn", "71039012345") // Strict value
                        .integerType("id", 4) // Value not important, but strict type
                    )
                .toPact();

        MockProviderConfig config = MockProviderConfig.createDefault();

        PactVerificationResult pactVerificationResult = runConsumerTest(pact, config, mockProvider -> {
            Person response = new ProviderClient(new ProviderConfig(mockProvider.getUrl())).getPerson(person.getSsn());

            assertEquals(response.getSsn(), person.getSsn());
            assertTrue(response.getId() != null);
            assertTrue(response.getName() != null);
        });

        assertEquals(PactVerificationResult.Ok.INSTANCE, pactVerificationResult);
    }
}
