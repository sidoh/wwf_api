package org.sidoh.wwf_api.util;

import com.google.common.io.ByteStreams;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TCompactProtocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ThriftSerializationHelper {
  private static ThriftSerializationHelper instance;

  private static final TSerializer COMPACT_SERIALIZER
    = new TSerializer(new TCompactProtocol.Factory());
  private static final TDeserializer COMPACT_DESERIALIZER
    = new TDeserializer(new TCompactProtocol.Factory());

  /**
   * Force singleton
   */
  private ThriftSerializationHelper() { }

  public static ThriftSerializationHelper getInstance() {
    if ( instance == null ) {
      instance = new ThriftSerializationHelper();
    }
    return instance;
  }

  /**
   * Write the provided object to the provided file
   *
   * @param object
   * @param file
   * @throws IOException
   * @throws TException
   */
  public void serialize(TBase<?, ?> object, File file) throws IOException, TException {
    FileOutputStream stream = new FileOutputStream(file);
    serialize(object, stream);
    stream.close();
  }

  /**
   * Serialize the provided byte array and write it the specified output stream
   * @param object
   * @param stream
   * @throws TException
   * @throws IOException
   */
  public void serialize(TBase<?, ?> object, OutputStream stream) throws TException, IOException {
    stream.write(COMPACT_SERIALIZER.serialize(object));
  }

  /**
   * Serialize the provided object and return it as a byte array
   *
   * @param object
   * @return
   * @throws TException
   */
  public byte[] serialize(TBase<?, ?> object) throws TException {
    return COMPACT_SERIALIZER.serialize(object);
  }

  /**
   * Deserialize the provided file
   *
   * @param file
   * @param prototype
   * @param <T>
   * @return
   * @throws IOException
   * @throws TException
   */
  public <T extends TBase<?, ?>> T deserialize(File file, T prototype) throws IOException, TException {
    FileInputStream stream = new FileInputStream(file);
    T object = deserialize(stream, prototype);
    stream.close();
    return object;
  }

  /**
   * Deserialize the provided type and return it
   *
   * @param reader
   * @param prototype
   * @param <T>
   * @return
   * @throws IOException
   * @throws TException
   */
  public <T extends TBase<?, ?>> T deserialize(InputStream reader, T prototype) throws IOException, TException {
    COMPACT_DESERIALIZER.deserialize(prototype, ByteStreams.toByteArray(reader));
    return prototype;
  }
}
