package protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, 
    include = JsonTypeInfo.As.PROPERTY, 
    property = "payloadType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegisterPayload.class, name = "REGISTER"),
        @JsonSubTypes.Type(value = TaskPayload.class, name = "TASK"),
        @JsonSubTypes.Type(value = ResultPayload.class, name = "RESULT")
})
public sealed interface Payload permits RegisterPayload, TaskPayload, ResultPayload {
}
