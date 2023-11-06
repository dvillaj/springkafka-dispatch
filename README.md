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

- {"orderId": "5f33d22e-586d-4bcf-b8c9-02e9d0aea300", "item": "item-1"}
- {"orderId": "e0fa7290-92d9-4fda-a00a-b35c5fdb5867", "item": "item-2"}
- {"orderId": "cb7c7bee-a7fe-463a-9402-64c176aa61b1", "item": "item-3"}
- {"orderId": "adec6441-f9ab-426d-917e-455f13b0a389", "item": "item-4"}


## Consume

```
cd $HOME/.local/kafka_2.12-3.6.0
bin/kafka-console-consumer.sh --topic order.dispatched --bootstrap-server localhost:9092
```
## Produce with Key

```
cd $HOME/.local/kafka_2.12-3.6.0
bin/kafka-console-producer.sh --topic order.created --bootstrap-server localhost:9092 --property parse.key=true --property key.separator=:
```

- 123:{"orderId": "5f33d22e-586d-4bcf-b8c9-02e9d0aea300", "item": "item-1"}
- 389:{"orderId": "e0fa7290-92d9-4fda-a00a-b35c5fdb5867", "item": "item-2"}
- 411:{"orderId": "cb7c7bee-a7fe-463a-9402-64c176aa61b1", "item": "item-3"}
- 
- 200:{"orderId": "cb7c7bee-a7fe-463a-9402-64c176aa61b1", "item": "item_200"}
- 400:{"orderId": "cb7c7bee-a7fe-463a-9402-64c176aa61b1", "item": "item_400"}
- 502:{"orderId": "cb7c7bee-a7fe-463a-9402-64c176aa61b1", "item": "item_502"}


## Consume with Key

```
cd $HOME/.local/kafka_2.12-3.6.0
bin/kafka-console-consumer.sh --topic order.dispatched --bootstrap-server localhost:9092 --property print.key=true --property key.separator=:
```

## Add Partitions to a topic

```
cd $HOME/.local/kafka_2.12-3.6.0
bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic order.created

bin/kafka-topics.sh --bootstrap-server localhost:9092 --alter --topic order.created --partitions 5
```


## Describe Consumer Groups

```
cd $HOME/.local/kafka_2.12-3.6.0
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group dispatch.order.created.consumer

```

## Wiremock

```bash
git clone https://github.com/lydtechconsulting/introduction-to-kafka-wiremock.git
cd introduction-to-kafka-wiremock/
curl https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.3.1/wiremock-standalone-3.3.1.jar --output wiremock-standalone-3.3.1.jar
java -jar wiremock-standalone-3.3.1.jar --port 9001
```