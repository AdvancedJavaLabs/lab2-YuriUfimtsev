package util;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

public final class SerializationUtils {
    public static <T extends Serializable> byte[] serialize(T object) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

            objectOutputStream.writeObject(object);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Serialization error", e);
        }
    }

    public static <T> T deserialize(byte[] bytes) {
        try (ObjectInputStream objectInputStream =
                     new ObjectInputStream(new ByteArrayInputStream(bytes))) {

            return (T) objectInputStream.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException("Deserialization IO error", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Deserialization class not found", e);
        }
    }
}
