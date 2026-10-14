package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Codec<T> {
    void write(DataOutputStream output, T value) throws IOException;

    T read(DataInputStream input) throws IOException;
}
