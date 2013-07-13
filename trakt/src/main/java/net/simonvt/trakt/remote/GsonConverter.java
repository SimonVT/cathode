package net.simonvt.trakt.remote;

import com.google.gson.Gson;
import com.squareup.tape.FileObjectQueue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class GsonConverter<T> implements FileObjectQueue.Converter<T> {

    private final Gson mGson;

    private final Class<T> mType;

    public GsonConverter(Gson gson, Class<T> type) {
        this.mGson = gson;
        this.mType = type;
    }

    @Override
    public T from(byte[] bytes) {
        Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
        return mGson.fromJson(reader, mType);
    }

    @Override
    public void toStream(T object, OutputStream bytes) throws IOException {
        Writer writer = new OutputStreamWriter(bytes);
        mGson.toJson(object, mType, writer);
        writer.flush();
        writer.close();
    }
}
