package app.serializers;

import app.objects.Tweet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TweetSerializer implements Serializer {
    Logger logger = LoggerFactory.getLogger(TweetSerializer.class);
    @Override
    public void configure(Map configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        Tweet tweet = (Tweet) data;
        byte[] byteValue = null;
        ObjectMapper mapper = new ObjectMapper();
        try{
            byteValue = mapper.writeValueAsString(tweet).getBytes(
                    StandardCharsets.UTF_8);
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return byteValue;
    }

    @Override
    public void close() {
        Serializer.super.close();
    }
}
