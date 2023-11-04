# dispatch application


## run

```
mvn clean install
mvn spring-boot:run
```

## Kafka

```
cd $HOME/.local/kafka_2.12-3.6.0
rm -rf /tmp/kraft-combined-logs/
KAFKA_CLUSTER_ID=$(bin/kafka-storage.sh random-uuid)
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties
bin/kafka-server-start.sh config/kraft/server.properties
```

## Console

```
export KAFKA_BROKERS=localhost:9092
redpanda-console
```

## Produce

```
cd $HOME/.local/kafka_2.12-3.6.0
bin/kafka-console-producer.sh --topic order.created --bootstrap-server localhost:9092
```

`{"orderId": "5f33d22e-586d-4bcf-b8c9-02e9d0aea300", "item": "item-1"}`

- e0fa7290-92d9-4fda-a00a-b35c5fdb5867
- cb7c7bee-a7fe-463a-9402-64c176aa61b1
- adec6441-f9ab-426d-917e-455f13b0a389