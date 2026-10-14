package protocol;

public record ResultPayload(String jobId, int exitCode, String stdout, String stderr) implements Payload {
}
