package app.consumers;

import app.config.KafkaConsumerConfig;
import app.objects.Tweet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;

public class TwitterConsumer {
    private static KafkaConsumer<String, Tweet> consumer = new KafkaConsumer<String, Tweet>(
            KafkaConsumerConfig.getProperties());
    static Logger logger = LoggerFactory.getLogger(TwitterConsumer.class);
    static ObjectMapper mapper = new ObjectMapper();
    public static void main(String[] args) throws JsonProcessingException {
        String topic = "twitter";
        consumer.subscribe(Collections.singleton(topic));
        while(true){
            ConsumerRecords<String, Tweet> records = consumer.poll(Duration.ofMillis(100));
            for(ConsumerRecord<String, Tweet> record: records){
                logger.info("Key: " + record.key() + "\n" +
                        "Value: " + mapper.writeValueAsString(record.value()) + "\n" +
                        "Partition: " + record.partition() + "\n" +
                        "Offset" + record.offset()
                );
            }
        }
    }
}
