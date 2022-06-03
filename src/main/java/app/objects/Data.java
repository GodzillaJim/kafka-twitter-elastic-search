package app.objects;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class Data {
    @JsonProperty("id")
    private String id;
    @JsonProperty("text")
    private String text;
}
