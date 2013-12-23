/*
 * Copyright (C) 2013 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.remote;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
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
      throw new JsonParseException("no '" + memberName + "' member found in json file.");
    }
    return elem;
  }
}
