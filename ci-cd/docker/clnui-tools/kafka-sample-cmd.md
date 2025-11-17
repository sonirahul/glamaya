#### Kafka Important Commands

KAFKA_CONSUMER_GROUP=""
KAFKA_TOPIC=""
KAFKA_TOPICS=""

1. Reset Offset
   1. To Latest: $KAFKA_HOME/bin/kafka-consumer-groups.sh --bootstrap-server $KAFKA_BROKERCONNECT --reset-offsets --group $KAFKA_CONSUMER_GROUP --topic $KAFKA_TOPIC --to-latest --execute --command-config $KAFKA_CONFIG_HOME/$REGION-$ENV/$USER/client.config
   2. To Earliest: $KAFKA_HOME/bin/kafka-consumer-groups.sh --bootstrap-server $KAFKA_BROKERCONNECT --reset-offsets --group $KAFKA_CONSUMER_GROUP --topic $KAFKA_TOPIC --to-earliest --execute --command-config $KAFKA_CONFIG_HOME/$REGION-$ENV/$USER/client.config

2. Retention Time
   1. $KAFKA_HOME/bin/kafka-configs.sh --bootstrap-server $KAFKA_BROKERCONNECT --alter --topic $KAFKA_TOPIC --command-config $KAFKA_CONFIG_HOME/$REGION-$ENV/$USER/client.config --add-config retention.ms=604800000
   2. for i in $KAFKA_TOPICS; do $KAFKA_HOME/bin/kafka-configs.sh --bootstrap-server $KAFKA_BROKERCONNECT --alter --topic $i --command-config $KAFKA_CONFIG_HOME/$REGION-$ENV/$USER/client.config --add-config retention.ms=604800000; done

3. Describe Topic
   1. $KAFKA_HOME/bin/kafka-configs.sh --bootstrap-server $KAFKA_BROKERCONNECT --entity-type topics --entity-name $KAFKA_TOPIC --command-config $KAFKA_CONFIG_HOME/$REGION-$ENV/$USER/client.config --describe

4. Identify Lag
   1. $KAFKA_HOME/bin/kafka-consumer-groups.sh --bootstrap-server $KAFKA_BROKERCONNECT --group $KAFKA_CONSUMER_GROUP --command-config $KAFKA_CONFIG_HOME/$REGION-$ENV/$USER/client.config --describe | awk -F" " '{if ($6 > 0) print $0}'

5. List Topics
   1. $KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server $KAFKA_BROKERCONNECT --list --command-config $KAFKA_CONFIG_HOME/$REGION-$ENV/$USER/client.config

6. Console Consumer
   1. $KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server $KAFKA_BROKERCONNECT --topic $KAFKA_TOPIC --from-beginning --consumer.config $KAFKA_CONFIG_HOME/$REGION-$ENV/$USER/client.config --group group-console-consumer | jq '{supplierNum: .supplierDetails.supplierNumber, invoiceStatus: .invoiceDetails.invoiceStatus, holdCode: .invoiceDetails.customExtension.invoiceHoldCode, invoiceNum: .invoiceDetails.invoiceNumber, voucherNum: .invoiceDetails.voucherNumber, netAmount: .netAmount, grossAmount: .grossAmount } | select(.netAmount != .grossAmount)'

7. Some useful queries
   1. $KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server $KAFKA_BROKERCONNECT --topic $KAFKA_TOPIC --from-beginning --consumer.config $KAFKA_CONFIG_HOME/$REGION-$ENV/$USER/client.config --group group-console-consumer --formatter kafka.tools.LoggingMessageFormatter --property print.partition=true --property print.offset=true --property print.value=true --max-messages 578601 | sed -e 's|Partition|{"Partition"|g' | sed -e 's|Offset|,"Offset"|g' | sed -e 's|{"event"|,"event"|g' | jq '{partition: .Partition, offset: .Offset, supplierNum: .supplierDetails.supplierNumber, invoiceStatus: .invoiceDetails.invoiceStatus, holdCode: .invoiceDetails.customExtension.invoiceHoldCode, invoiceNum: .invoiceDetails.invoiceNumber, voucherNum: .invoiceDetails.voucherNumber, netAmount: .netAmount, grossAmount: .grossAmount, transactionCurrency: .transactionCurrency, sourceChangeDate: .payload.sourceChangeDate} | select(.voucherNum == "RXLS76" and .netAmount != .grossAmount and .invoiceStatus == "P" and .sourceChangeDate == "2023-11-08T00:00:00.000Z")' | jq -s '. | length' > temp.json
   2. $KAFKA_HOME/bin/kafka-console-producer.sh --bootstrap-server $KAFKA_BROKERCONNECT --topic $KAFKA_TOPIC --producer.config $KAFKA_CONFIG_HOME/$REGION-$ENV/$USER/client.config < $KAFKA_TOPIC.07.11.22.json
