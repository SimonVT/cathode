package net.simonvt.cathode.api;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import java.lang.reflect.Type;
import javax.inject.Singleton;
import net.simonvt.cathode.api.entity.IsoTime;
import net.simonvt.cathode.api.enumeration.GrantType;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.enumeration.Scope;
import net.simonvt.cathode.api.enumeration.TokenType;
import net.simonvt.cathode.api.service.AuthorizationService;
import net.simonvt.cathode.api.service.CheckinService;
import net.simonvt.cathode.api.service.EpisodeService;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.api.service.RecommendationsService;
import net.simonvt.cathode.api.service.SearchService;
import net.simonvt.cathode.api.service.SeasonService;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.api.util.TimeUtils;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

@Module(library = true, complete = false)
public class TraktModule {

  private static final String API_URL = "http://api.v2.trakt.tv";

  @Provides @Singleton @Trakt RestAdapter provideRestAdapter(@Trakt Gson gson,
      TraktInterceptor interceptor, ErrorHandler errorHandler) {
    return new RestAdapter.Builder() //
        .setEndpoint(API_URL)
        .setConverter(new GsonConverter(gson))
        .setRequestInterceptor(interceptor)
        .setErrorHandler(errorHandler)
        .setLogLevel(RestAdapter.LogLevel.FULL)
        .build();
  }

  @Provides @Singleton AuthorizationService provideAuthorizationService(
      @Trakt RestAdapter adapter) {
    return adapter.create(AuthorizationService.class);
  }

  @Provides @Singleton CheckinService provideCheckinService(@Trakt RestAdapter adapter) {
    return adapter.create(CheckinService.class);
  }

  @Provides @Singleton EpisodeService provideEpisodeService(@Trakt RestAdapter adapter) {
    return adapter.create(EpisodeService.class);
  }

  @Provides @Singleton MoviesService provideMoviesService(@Trakt RestAdapter adapter) {
    return adapter.create(MoviesService.class);
  }

  @Provides @Singleton RecommendationsService provideRecommendationsService(
      @Trakt RestAdapter adapter) {
    return adapter.create(RecommendationsService.class);
  }

  @Provides @Singleton SearchService provideSearchService(@Trakt RestAdapter adapter) {
    return adapter.create(SearchService.class);
  }

  @Provides @Singleton SeasonService provideSeasonService(@Trakt RestAdapter adapter) {
    return adapter.create(SeasonService.class);
  }

  @Provides @Singleton ShowsService provideShowsService(@Trakt RestAdapter adapter) {
    return adapter.create(ShowsService.class);
  }

  @Provides @Singleton SyncService provideSyncService(@Trakt RestAdapter adapter) {
    return adapter.create(SyncService.class);
  }

  @Provides @Singleton UsersService provideUsersService(@Trakt RestAdapter adapter) {
    return adapter.create(UsersService.class);
  }

  @Provides @Singleton @Trakt Gson provideGson() {
    GsonBuilder builder = new GsonBuilder();
    builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

    builder.registerTypeAdapter(int.class, new IntTypeAdapter());
    builder.registerTypeAdapter(Integer.class, new IntTypeAdapter());

    builder.registerTypeAdapter(IsoTime.class, new JsonDeserializer<IsoTime>() {
      @Override public IsoTime deserialize(JsonElement json, Type typeOfT,
          JsonDeserializationContext context) throws JsonParseException {
        String iso = json.getAsString();
        if (iso == null) {
          return null;
        }

        long timeInMillis = TimeUtils.getMillis(iso);
        return new IsoTime(iso, timeInMillis);
      }
    });

    builder.registerTypeAdapter(GrantType.class, new JsonDeserializer<GrantType>() {
      @Override public GrantType deserialize(JsonElement jsonElement, Type type,
          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return GrantType.fromValue(jsonElement.getAsString());
      }
    });

    builder.registerTypeAdapter(TokenType.class, new JsonDeserializer<TokenType>() {
      @Override public TokenType deserialize(JsonElement jsonElement, Type type,
          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return TokenType.fromValue(jsonElement.getAsString());
      }
    });

    builder.registerTypeAdapter(Scope.class, new JsonDeserializer<Scope>() {
      @Override public Scope deserialize(JsonElement jsonElement, Type type,
          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return Scope.fromValue(jsonElement.getAsString());
      }
    });

    builder.registerTypeAdapter(ItemType.class, new JsonDeserializer<ItemType>() {
      @Override public ItemType deserialize(JsonElement jsonElement, Type type,
          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return ItemType.fromValue(jsonElement.getAsString());
      }
    });

    return builder.create();
  }

  public static class IntTypeAdapter extends TypeAdapter<Number> {

    @Override public void write(JsonWriter out, Number value) throws IOException {
      out.value(value);
    }

    @Override public Number read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      try {
        String result = in.nextString();
        if ("".equals(result)) {
          return null;
        }
        return Integer.parseInt(result);
      } catch (NumberFormatException e) {
        throw new JsonSyntaxException(e);
      }
    }
  }
}
