package net.simonvt.trakt.sync;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.simonvt.trakt.sync.task.TraktTask;

import java.lang.reflect.Type;

public class TraktTaskSerializer implements JsonSerializer<TraktTask>, JsonDeserializer<TraktTask> {

    @Override
    public JsonElement serialize(TraktTask src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject member = new JsonObject();
        member.addProperty("type", src.getClass().getName());
        member.add("data", context.serialize(src, src.getClass()));
        return member;
    }

    @Override
    public TraktTask deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject member = (JsonObject) json;
        final JsonElement typeString = get(member, "type");
        final JsonElement data = get(member, "data");
        final Type actualType = typeForName(typeString);
        return context.deserialize(data, actualType);
    }

    private Type typeForName(final JsonElement typeElem) {
        try {
            return Class.forName(typeElem.getAsString());
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }

    private JsonElement get(final JsonObject wrapper, final String memberName) {
        final JsonElement elem = wrapper.get(memberName);
        if (elem == null) {
            throw new JsonParseException(
                    "no '" + memberName + "' member found in json file.");
        }
        return elem;
    }
}
