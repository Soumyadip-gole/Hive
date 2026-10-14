package protocol;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class MessageCodec implements Codec<Message> {
    private static final int MAX_FRAME_SIZE = 16 * 1024 * 1024;

    private final ObjectMapper objectMapper;

    public MessageCodec() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public MessageCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(DataOutputStream output, Message message) throws IOException {
        byte[] json = objectMapper.writeValueAsBytes(message);
        if (json.length > MAX_FRAME_SIZE) {
            throw new IOException("Message exceeds maximum frame size: " + json.length);
        }
        output.writeInt(json.length);
        output.write(json);
    }

    @Override
    public Message read(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_FRAME_SIZE) {
            throw new IOException("Invalid frame length: " + length);
        }
        byte[] json = new byte[length];
        input.readFully(json);
        return objectMapper.readValue(json, Message.class);
    }
}
