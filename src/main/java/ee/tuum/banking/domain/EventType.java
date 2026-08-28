package ee.tuum.banking.domain;

public enum EventType {
    ACCOUNT_CREATED("account.created"),
    BALANCE_CREATED("balance.created"),
    BALANCE_UPDATED("balance.updated"),
    TRANSACTION_CREATED("transaction.created");

    private final String routingKey;

    EventType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }
}
