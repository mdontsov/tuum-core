package ee.tuum.banking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.tuum.banking.outbox.mapper.OutboxMapper;
import ee.tuum.banking.outbox.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "banking.outbox.fixed-delay-ms=3600000")
@AutoConfigureMockMvc
@Import(BankingIntegrationTest.RabbitTestConfiguration.class)
class BankingIntegrationTest {
    private static final String TEST_QUEUE = "banking.integration.events";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("banking")
            .withUsername("banking")
            .withPassword("banking");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired OutboxPublisher outboxPublisher;
    @Autowired OutboxMapper outboxMapper;

    @BeforeEach
    void cleanDatabaseAndQueue() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM account_transactions");
        jdbcTemplate.update("DELETE FROM balances");
        jdbcTemplate.update("DELETE FROM accounts");
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(TEST_QUEUE);
            return null;
        });
    }

    @Test
    void createsAndGetsAccountWithRequestedZeroBalances() throws Exception {
        UUID accountId = createAccount("customer-1", "ee", "EUR", "USD");

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.balances.length()").value(2))
                .andExpect(jsonPath("$.balances[0].availableAmount").value(0))
                .andExpect(jsonPath("$.balances[0].currency").value("EUR"))
                .andExpect(jsonPath("$.balances[1].currency").value("USD"));
        assertThat(outboxMapper.countUnpublished()).isEqualTo(3);
    }

    @Test
    void rejectsInvalidAndDuplicateCurrencies() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c1\",\"country\":\"EE\",\"currencies\":[\"CAD\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENCY"));

        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c1\",\"country\":\"EE\",\"currencies\":[\"EUR\",\"EUR\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CURRENCY"));
    }

    @Test
    void validatesAccountRequestsAndMissingAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\",\"country\":\"EST\",\"currencies\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void incomingAndOutgoingTransactionsChangeBalanceAndAppearInHistory() throws Exception {
        UUID accountId = createAccount("customer-2", "SE", "EUR");

        createTransaction(accountId, "100.2500", "EUR", "IN", "Salary", 201)
                .andExpect(jsonPath("$.balanceAfter").value(100.25));
        createTransaction(accountId, "25.25", "EUR", "OUT", "Groceries", 201)
                .andExpect(jsonPath("$.balanceAfter").value(75));

        mockMvc.perform(get("/api/v1/transactions").param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].direction").value("IN"))
                .andExpect(jsonPath("$[0].description").value("Salary"))
                .andExpect(jsonPath("$[1].direction").value("OUT"));
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
                .andExpect(jsonPath("$.balances[0].availableAmount").value(75));
        assertThat(outboxMapper.countUnpublished()).isEqualTo(6);
    }

    @Test
    void rejectsTransactionBusinessErrorsWithoutChangingBalance() throws Exception {
        UUID accountId = createAccount("customer-3", "GB", "GBP");

        createTransaction(accountId, "-1", "GBP", "IN", "Bad amount", 400)
                .andExpect(jsonPath("$.code").value("INVALID_AMOUNT"));
        createTransaction(accountId, "1.12345", "GBP", "IN", "Too precise", 400)
                .andExpect(jsonPath("$.code").value("INVALID_AMOUNT"));
        createTransaction(accountId, "1", "CAD", "IN", "Bad currency", 400)
                .andExpect(jsonPath("$.code").value("INVALID_CURRENCY"));
        createTransaction(accountId, "1", "GBP", "SIDEWAYS", "Bad direction", 400)
                .andExpect(jsonPath("$.code").value("INVALID_DIRECTION"));
        createTransaction(accountId, "1", "GBP", "IN", "   ", 400)
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        createTransaction(accountId, "1", "GBP", "OUT", "Overdraft", 409)
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
                .andExpect(jsonPath("$.balances[0].availableAmount").value(0));
    }

    @Test
    void rejectsMissingAccountAndUnavailableAccountCurrency() throws Exception {
        UUID missing = UUID.randomUUID();
        createTransaction(missing, "1", "EUR", "IN", "Deposit", 404)
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/transactions").param("accountId", missing.toString()))
                .andExpect(status().isNotFound());

        UUID accountId = createAccount("customer-4", "US", "USD");
        createTransaction(accountId, "1", "EUR", "IN", "Wrong balance", 400)
                .andExpect(jsonPath("$.code").value("BALANCE_NOT_FOUND"));
    }

    @Test
    void concurrentWithdrawalsCannotOverdrawBalance() throws Exception {
        UUID accountId = createAccount("customer-5", "EE", "EUR");
        createTransaction(accountId, "100", "EUR", "IN", "Initial funds", 201);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> transactionStatus(accountId, "80"));
            var second = executor.submit(() -> transactionStatus(accountId, "80"));
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
                .andExpect(jsonPath("$.balances[0].availableAmount").value(20));
    }

    @Test
    void outboxPublishesJsonMessagesAndMarksEventsPublished() throws Exception {
        UUID accountId = createAccount("customer-6", "EE", "EUR");
        createTransaction(accountId, "10", "EUR", "IN", "Deposit", 201);

        outboxPublisher.publishBatch();

        assertThat(outboxMapper.countUnpublished()).isZero();
        var messages = new java.util.ArrayList<org.springframework.amqp.core.Message>();
        for (int i = 0; i < 4; i++) {
            messages.add(rabbitTemplate.receive(TEST_QUEUE, 2_000));
        }
        assertThat(messages).doesNotContainNull();
        assertThat(messages).allSatisfy(message -> {
            assertThat(message.getMessageProperties().getMessageId()).isNotBlank();
            assertThat(message.getMessageProperties().getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
            assertThat(new String(message.getBody())).contains("accountId");
        });
    }

    @Test
    void malformedJsonReturnsConsistentBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private UUID createAccount(String customerId, String country, String... currencies) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "customerId", customerId, "country", country, "currencies", currencies));
        String response = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return UUID.fromString(json.get("accountId").asText());
    }

    private org.springframework.test.web.servlet.ResultActions createTransaction(
            UUID accountId, String amount, String currency, String direction, String description, int status) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "accountId", accountId, "amount", new java.math.BigDecimal(amount), "currency", currency,
                "direction", direction, "description", description));
        return mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(status));
    }

    private int transactionStatus(UUID accountId, String amount) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "accountId", accountId, "amount", new java.math.BigDecimal(amount), "currency", "EUR",
                "direction", "OUT", "description", "Concurrent withdrawal"));
        return mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getStatus();
    }

    @TestConfiguration
    static class RabbitTestConfiguration {
        @Bean
        Queue testQueue() {
            return new Queue(TEST_QUEUE, true, false, false);
        }

        @Bean
        Binding testBinding(Queue testQueue, TopicExchange accountEventsExchange) {
            return BindingBuilder.bind(testQueue).to(accountEventsExchange).with("#");
        }
    }
}
