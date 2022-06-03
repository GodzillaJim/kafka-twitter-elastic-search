package app.objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Rule {
    @JsonProperty("id")
    private String id;
    @JsonProperty("tag")
    private String tag;
}
