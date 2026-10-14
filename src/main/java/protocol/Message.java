package protocol;

import java.time.Instant;
import java.util.UUID;

public record Message(
        MessageType type,
        UUID messageId,
        UUID correlationId,
        String senderId,
        Instant timestamp,
        Payload payload
) {
}
