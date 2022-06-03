package app.components;


import app.config.KafkaProducerConfig;
import app.config.TwitterConfig;
import app.objects.Tweet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.*;

public class Streams {
    static Logger logger = LoggerFactory.getLogger(Streams.class);
    static ObjectMapper mapper = new ObjectMapper();
    static KafkaProducer<String, Tweet> producer = new KafkaProducer<String, Tweet>(
            KafkaProducerConfig.getProperties());
    static String topic = "twitter";

    public static void main(String[] args) {
        try{
            Map<String, String> rules = preparingRules();
            setupRules(rules);
            connectStreams();
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    private static void produceMessage(Tweet tweet){
        try{
            ProducerRecord<String, Tweet> record = new ProducerRecord<>(topic, tweet);
            producer.send(record).get();
        }catch (Exception e){
            logger.error("Blocked producer function failed!");
        }
    }

    private static Map<String, String> preparingRules(){
        Map<String, String> rules = new HashMap<>();
        rules.put("cats has:images", "cat images");
        rules.put("dogs has:images", "dog images");
        rules.put("cute cats has image","cute cats and dogs");
        return rules;
    }

    private static ArrayList<NameValuePair> getQueryParams () {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("tweet.fields", "created_at"));
        return params;
    }

    private static void connectStreams()
            throws URISyntaxException, IOException {
        URIBuilder builder = new URIBuilder("https://api.twitter.com/2/tweets/search/stream");
//        ArrayList<NameValuePair> params = getQueryParams();
//        builder.addParameters(params);
        HttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD)
                        .build())
                .build();
        HttpGet get = new HttpGet(builder.build());
        get.setHeader("Authorization", String.format("Bearer %s", TwitterConfig.BEARER_TOKEN));

        HttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        if(entity != null){
            BufferedReader reader = new BufferedReader(new InputStreamReader((entity.getContent())));
            String line = reader.readLine();
            while(line != null){
                try{
                    Tweet tweet = mapper.readValue(line, Tweet.class);
                    logger.debug(String.valueOf(tweet));
                    produceMessage(tweet);
                }catch(Exception e){
                    logger.error("JSON " +
                            "Mapping error");
                    logger.error(e.getMessage());
                }
                line = reader.readLine();
            }
        }
    }

    private static void setupRules(Map<String, String> rules){
         List<String> existingRules = getRules();
         if(existingRules != null && existingRules.size() > 0){
             deleteRules(existingRules);
         }
         createRules(rules);
    }

    private static void createRules(Map<String, String> rules){
        try{
            createRequestObject(getFormattedString(rules));
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    private static void createRequestObject(String formattedString)
            throws URISyntaxException, IOException {
        HttpClient client = getHttpClient();
        URIBuilder builder = new URIBuilder("https://api.twitter.com/2/tweets/search/stream/rules");
        HttpPost post = new HttpPost(builder.build());
        post.setHeader("Authorization", TwitterConfig.BEARED_TOKEN);
        post.setHeader("content-type", "application/json");
        StringEntity body = new StringEntity(Objects.requireNonNull(
                formattedString));
        post.setEntity(body);
        HttpResponse response = client.execute(post);
        HttpEntity entity = response.getEntity();
        if(entity != null){
            System.out.print(EntityUtils.toString(entity, "UTF-8"));
        }
    }

    private static List<String> getRules() {
        try {
            List<String> rules = new ArrayList<>();
            HttpClient client = getHttpClient();
            URIBuilder builder = new URIBuilder("https://api.twitter.com/2/tweets/search/stream/rules");
            HttpGet get = new HttpGet(builder.build());
            get.setHeader("Authorization", TwitterConfig.BEARED_TOKEN);
            get.setHeader("content-type","application/json");
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            if(entity != null){
                JSONObject json = new JSONObject();
                if(json.length() > 1){
                    JSONArray array = (JSONArray) json.get("data");
                    for (int i=0; i < array.length(); i++){
                        JSONObject object = (JSONObject) array.get(i);
                        rules.add(object.getString("id"));
                    }
                }
            }
            return rules;
        } catch (URISyntaxException syntaxException){
            logger.error("URI builder produced a syntax error:-> " + syntaxException.getMessage());
        } catch (IOException | JSONException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private static HttpClient getHttpClient(){
        return HttpClients
                .custom()
                .setDefaultRequestConfig(
                        RequestConfig
                                .custom()
                                .setCookieSpec(CookieSpecs.STANDARD)
                                .build()
                )
                .build();
    }

    private static void deleteRules(List<String> existingRules){
        try{
            createRequestObject(getFormattedString(
                    existingRules));
        } catch (URISyntaxException | IOException e) {
            logger.error(e.getMessage());
        }
    }

    private static String getFormattedString(List<String> ids){
        try{
            StringBuilder builder = new StringBuilder();
            if(ids.size() == 1){
                return String.format(
                        "{ \"delete\": { \"ids\": [%s]}}", "\"" + ids.get(0) + "\"");
            } else {
                for (String id: ids){
                    builder.append("\"").append(id).append("\"").append(",");
                }
                String result = builder.toString();
                return String.format("{ \"delete\": { \"ids\": [%s]}}", result.substring(0, result.length() - 1));
            }

        }catch (Exception e){
            logger.error(e.getMessage());
        }
        return null;
    }

    private static String getFormattedString(Map<String, String> rules){
        try{
            StringBuilder builder = new StringBuilder();
            if(rules.size() == 1){
                String key = rules.keySet().iterator().next();
                return String.format("{\"add\": [%s]}", "{\"value\": \"" + key + "\", \"tag\": \"" + rules.get(key) + "\"}");
            } else {
                for (Map.Entry<String, String> entry: rules.entrySet()){
                    String value = entry.getKey();
                    String tag = entry.getValue();
                    builder.append("{\"value\": \"").append(value)
                            .append("\", \"tag\": \"").append(tag).append("\"}")
                            .append(",");
                }
                String result = builder.toString();
                return String.format(
                        "{\"add\": [%s]}", result.substring(0, result.length() - 1));
            }
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        return null;
    }
}
