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
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import javax.inject.Singleton;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.enumeration.CommentType;
import net.simonvt.cathode.api.enumeration.DayOfWeek;
import net.simonvt.cathode.api.enumeration.Gender;
import net.simonvt.cathode.api.enumeration.ListItemType;
import net.simonvt.cathode.api.enumeration.ListPrivacy;
import net.simonvt.cathode.api.enumeration.Rating;
import net.simonvt.cathode.api.enumeration.RatingMode;
import net.simonvt.cathode.api.enumeration.ShowStatus;
import net.simonvt.cathode.api.enumeration.Status;
import net.simonvt.cathode.api.service.AccountService;
import net.simonvt.cathode.api.service.ActivityService;
import net.simonvt.cathode.api.service.CalendarService;
import net.simonvt.cathode.api.service.CommentService;
import net.simonvt.cathode.api.service.GenresService;
import net.simonvt.cathode.api.service.ListsService;
import net.simonvt.cathode.api.service.MovieService;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.api.service.NetworkService;
import net.simonvt.cathode.api.service.RateService;
import net.simonvt.cathode.api.service.RecommendationsService;
import net.simonvt.cathode.api.service.SearchService;
import net.simonvt.cathode.api.service.ServerService;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.api.service.UserService;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

@Module(library = true, complete = false)
public class TraktModule {

  private static final String API_URL = "http://api.trakt.tv";

  @Provides @Singleton @Trakt
  RestAdapter provideRestAdapter(@Trakt Gson gson, TraktInterceptor interceptor,
      ErrorHandler errorHandler) {
    return new RestAdapter.Builder().setServer(API_URL)
        .setConverter(new GsonConverter(gson))
        .setRequestInterceptor(interceptor)
        .setErrorHandler(errorHandler)
        .build();
  }

  @Provides @Singleton AccountService provideAccountService(@Trakt RestAdapter adapter) {
    return adapter.create(AccountService.class);
  }

  @Provides @Singleton ActivityService provideActivityService(@Trakt RestAdapter adapter) {
    return adapter.create(ActivityService.class);
  }

  @Provides @Singleton CalendarService provideCalendarService(@Trakt RestAdapter adapter) {
    return adapter.create(CalendarService.class);
  }

  @Provides @Singleton CommentService provideCommentService(@Trakt RestAdapter adapter) {
    return adapter.create(CommentService.class);
  }

  @Provides @Singleton GenresService provideGenresService(@Trakt RestAdapter adapter) {
    return adapter.create(GenresService.class);
  }

  @Provides @Singleton ListsService provideListsService(@Trakt RestAdapter adapter) {
    return adapter.create(ListsService.class);
  }

  @Provides @Singleton MovieService provideMovieService(@Trakt RestAdapter adapter) {
    return adapter.create(MovieService.class);
  }

  @Provides @Singleton MoviesService provideMoviesService(@Trakt RestAdapter adapter) {
    return adapter.create(MoviesService.class);
  }

  @Provides @Singleton NetworkService provideNetworkService(@Trakt RestAdapter adapter) {
    return adapter.create(NetworkService.class);
  }

  @Provides @Singleton RateService provideRateService(@Trakt RestAdapter adapter) {
    return adapter.create(RateService.class);
  }

  @Provides @Singleton
  RecommendationsService provideRecommendationService(@Trakt RestAdapter adapter) {
    return adapter.create(RecommendationsService.class);
  }

  @Provides @Singleton SearchService provideSearchService(@Trakt RestAdapter adapter) {
    return adapter.create(SearchService.class);
  }

  @Provides @Singleton ServerService provideServerService(@Trakt RestAdapter adapter) {
    return adapter.create(ServerService.class);
  }

  @Provides @Singleton ShowService provideShowService(@Trakt RestAdapter adapter) {
    return adapter.create(ShowService.class);
  }

  @Provides @Singleton ShowsService provideShowsService(@Trakt RestAdapter adapter) {
    return adapter.create(ShowsService.class);
  }

  @Provides @Singleton UserService provideUserService(@Trakt RestAdapter adapter) {
    return adapter.create(UserService.class);
  }

  @Provides @Singleton @Trakt Gson provideGson() {
    GsonBuilder builder = new GsonBuilder();
    builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

    builder.registerTypeAdapter(int.class, new IntTypeAdapter());
    builder.registerTypeAdapter(Integer.class, new IntTypeAdapter());

    builder.registerTypeAdapter(Gender.class, new JsonDeserializer<Gender>() {
      @Override
      public Gender deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
          throws JsonParseException {
        return Gender.fromValue(json.getAsString());
      }
    });

    builder.registerTypeAdapter(Rating.class, new JsonDeserializer<Rating>() {
      @Override
      public Rating deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
          throws JsonParseException {
        return Rating.fromValue(json.getAsString());
      }
    });

    builder.registerTypeAdapter(RatingMode.class, new JsonDeserializer<RatingMode>() {
      @Override
      public RatingMode deserialize(JsonElement json, Type typeOfT,
          JsonDeserializationContext context) throws JsonParseException {
        return RatingMode.fromValue(json.getAsString());
      }
    });

    builder.registerTypeAdapter(Status.class, new JsonDeserializer<Status>() {
      @Override
      public Status deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
          throws JsonParseException {
        return Status.fromValue(json.getAsString());
      }
    });

    builder.registerTypeAdapter(ShowStatus.class, new JsonDeserializer<ShowStatus>() {

      @Override
      public ShowStatus deserialize(JsonElement json, Type typeOfT,
          JsonDeserializationContext context) throws JsonParseException {
        return ShowStatus.fromValue(json.getAsString());
      }
    });

    builder.registerTypeAdapter(DayOfWeek.class, new JsonDeserializer<DayOfWeek>() {
      @Override
      public DayOfWeek deserialize(JsonElement json, Type type,
          JsonDeserializationContext context) throws JsonParseException {
        return DayOfWeek.fromValue(json.getAsString());
      }
    });

    builder.registerTypeAdapter(ListPrivacy.class, new JsonDeserializer<ListPrivacy>() {
      @Override public ListPrivacy deserialize(JsonElement json, Type type,
          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return ListPrivacy.fromValue(json.getAsString());
      }
    });

    builder.registerTypeAdapter(ListItemType.class, new JsonDeserializer<ListItemType>() {
      @Override public ListItemType deserialize(JsonElement json, Type type,
          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return ListItemType.fromValue(json.getAsString());
      }
    });

    builder.registerTypeAdapter(CommentType.class, new JsonDeserializer<CommentType>() {
      @Override public CommentType deserialize(JsonElement json, Type type,
          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return CommentType.fromValue(json.getAsString());
      }
    });

    builder.registerTypeAdapter(Season.Episodes.class, new JsonDeserializer<Season.Episodes>() {
      @Override
      public Season.Episodes deserialize(JsonElement json, Type typeOfT,
          JsonDeserializationContext context) throws JsonParseException {
        Season.Episodes episodes = new Season.Episodes();
        try {
          if (json.isJsonArray()) {
            if (json.getAsJsonArray().get(0).isJsonPrimitive()) {
              //Episode number list
              Field fieldNumbers = Season.Episodes.class.getDeclaredField("numbers");
              fieldNumbers.setAccessible(true);
              fieldNumbers.set(episodes, context.deserialize(json, (new TypeToken<List<Integer>>() {
              }).getType()));
            } else {
              //Episode object list
              Field fieldList = Season.Episodes.class.getDeclaredField("episodes");
              fieldList.setAccessible(true);
              fieldList.set(episodes, context.deserialize(json, (new TypeToken<List<Episode>>() {
              }).getType()));
            }
          } else {
            //Episode count
            Field fieldCount = Season.Episodes.class.getDeclaredField("count");
            fieldCount.setAccessible(true);
            fieldCount.set(episodes, new Integer(json.getAsInt()));
          }
        } catch (SecurityException e) {
          throw new JsonParseException(e);
        } catch (NoSuchFieldException e) {
          throw new JsonParseException(e);
        } catch (IllegalArgumentException e) {
          throw new JsonParseException(e);
        } catch (IllegalAccessException e) {
          throw new JsonParseException(e);
        }
        return episodes;
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
