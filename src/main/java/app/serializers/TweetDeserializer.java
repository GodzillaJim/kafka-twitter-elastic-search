package app.serializers;

import app.objects.Tweet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TweetDeserializer implements Deserializer {
    ObjectMapper mapper = new ObjectMapper();
    Logger logger = LoggerFactory.getLogger(TweetDeserializer.class);
    @Override
    public Object deserialize(String topic, byte[] data) {
        Tweet tweet = null;
        try{
            tweet = mapper.readValue(data, Tweet.class);
        }catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        return tweet;
    }
}
