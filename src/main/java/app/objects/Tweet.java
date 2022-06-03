package app.objects;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class Tweet {
    @JsonProperty("data")
    private Data data;
    @JsonProperty("matching_rules")
    private Rule [] matching_rules;
}
