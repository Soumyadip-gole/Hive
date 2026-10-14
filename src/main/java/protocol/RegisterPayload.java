package protocol;

public record RegisterPayload(
    String hostname, 
    int cores, 
    long totalMemMb
) implements Payload {}
