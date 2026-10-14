package protocol;

public record TaskPayload(String jobId, String command) implements Payload {
}
