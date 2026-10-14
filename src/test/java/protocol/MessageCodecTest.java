package protocol;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageCodecTest {
    private final MessageCodec codec = new MessageCodec();

    @Test
    void roundTripsRegisterMessage() throws Exception {
        Message message = message(MessageType.REGISTER, new RegisterPayload("worker-1", 8, 16384));

        assertEquals(message, roundTrip(message));
    }

    @Test
    void roundTripsErrorMessageWithTaskPayload() throws Exception {
        Message message = message(MessageType.ERROR, new TaskPayload("job-7", "echo hello"));

        assertEquals(message, roundTrip(message));
    }

    @Test
    void roundTripsResultPayload() throws Exception {
        Message message = message(MessageType.ERROR, new ResultPayload("job-7", 0, "hello\n", ""));

        assertEquals(message, roundTrip(message));
    }

    @Test
    void readsTwoMessagesBackToBack() throws Exception {
        Message first = message(MessageType.REGISTER, new RegisterPayload("worker-1", 4, 8192));
        Message second = message(MessageType.ERROR, new ResultPayload("job-1", 1, "", "failed"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (DataOutputStream output = new DataOutputStream(bytes)) {
            codec.write(output, first);
            codec.write(output, second);
        }

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(first, codec.read(input));
            assertEquals(second, codec.read(input));
        }
    }

    @Test
    void rejectsTruncatedFrame() throws Exception {
        Message message = message(MessageType.REGISTER, new RegisterPayload("worker-1", 4, 8192));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            codec.write(output, message);
        }
        byte[] truncated = bytes.toByteArray();
        byte[] shortened = java.util.Arrays.copyOf(truncated, truncated.length - 1);

        assertThrows(java.io.EOFException.class, () -> codec.read(
                new DataInputStream(new ByteArrayInputStream(shortened))));
    }

    private Message roundTrip(Message message) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            codec.write(output, message);
        }
        return codec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }

    private Message message(MessageType type, Payload payload) {
        return new Message(
                type,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "coordinator",
                Instant.parse("2026-10-15T00:00:00Z"),
                payload
        );
    }
}
