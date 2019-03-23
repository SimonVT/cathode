package net.simonvt.cathode.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Lazy
import dagger.Module
import dagger.Provides
import net.simonvt.cathode.api.entity.IsoTimeAdapter
import net.simonvt.cathode.api.enumeration.ActionAdapter
import net.simonvt.cathode.api.enumeration.CommentTypeAdapter
import net.simonvt.cathode.api.enumeration.DepartmentAdapter
import net.simonvt.cathode.api.enumeration.ExtendedAdapter
import net.simonvt.cathode.api.enumeration.GenderAdapter
import net.simonvt.cathode.api.enumeration.GrantTypeAdapter
import net.simonvt.cathode.api.enumeration.HiddenSectionAdapter
import net.simonvt.cathode.api.enumeration.ItemTypeAdapter
import net.simonvt.cathode.api.enumeration.ItemTypesAdapter
import net.simonvt.cathode.api.enumeration.PrivacyAdapter
import net.simonvt.cathode.api.enumeration.ScopeAdapter
import net.simonvt.cathode.api.enumeration.ShowStatusAdapter
import net.simonvt.cathode.api.enumeration.TokenTypeAdapter
import net.simonvt.cathode.api.service.AuthorizationService
import net.simonvt.cathode.api.service.CheckinService
import net.simonvt.cathode.api.service.CommentsService
import net.simonvt.cathode.api.service.EpisodeService
import net.simonvt.cathode.api.service.MoviesService
import net.simonvt.cathode.api.service.PeopleService
import net.simonvt.cathode.api.service.RecommendationsService
import net.simonvt.cathode.api.service.SearchService
import net.simonvt.cathode.api.service.SeasonService
import net.simonvt.cathode.api.service.ShowsService
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.api.service.UsersService
import net.simonvt.cathode.api.util.OkHttpUtils
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
class TraktModule {

  @Provides
  @Singleton
  @Named(NAMED_TRAKT)
  fun provideRestAdapter(@Named(NAMED_TRAKT) client: OkHttpClient, @Named(NAMED_TRAKT) moshi: Moshi): Retrofit {
    return Retrofit.Builder()
      .baseUrl(API_URL)
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
  }

  @Provides
  @Singleton
  @Named(NAMED_TRAKT)
  fun provideOkHttpClient(
    context: Context,
    settings: TraktSettings,
    @Named(NAMED_TRAKT) interceptors: List<@JvmSuppressWildcards Interceptor>,
    authService: Lazy<AuthorizationService>
  ): OkHttpClient {
    val builder = OkHttpClient.Builder()
    builder.connectTimeout(15, TimeUnit.SECONDS)
    builder.readTimeout(20, TimeUnit.SECONDS)

    val cacheDir = OkHttpUtils.getCacheDir(context)
    builder.cache(Cache(cacheDir, OkHttpUtils.getCacheSize(cacheDir)))

    builder.interceptors().addAll(interceptors)
    builder.interceptors().add(ApiInterceptor(settings))
    builder.interceptors().add(AuthInterceptor(settings))
    builder.authenticator(TraktAuthenticator(settings, authService))

    return builder.build()
  }

  @Provides
  @Singleton
  fun provideAuthorizationService(@Named(NAMED_TRAKT) adapter: Retrofit): AuthorizationService {
    return adapter.create(AuthorizationService::class.java)
  }

  @Provides
  @Singleton
  fun provideCheckinService(@Named(NAMED_TRAKT) adapter: Retrofit): CheckinService {
    return adapter.create(CheckinService::class.java)
  }

  @Provides
  @Singleton
  fun provideCommentsService(@Named(NAMED_TRAKT) adapter: Retrofit): CommentsService {
    return adapter.create(CommentsService::class.java)
  }

  @Provides
  @Singleton
  fun provideEpisodeService(@Named(NAMED_TRAKT) adapter: Retrofit): EpisodeService {
    return adapter.create(EpisodeService::class.java)
  }

  @Provides
  @Singleton
  fun provideMoviesService(@Named(NAMED_TRAKT) adapter: Retrofit): MoviesService {
    return adapter.create(MoviesService::class.java)
  }

  @Provides
  @Singleton
  fun providePeopleService(@Named(NAMED_TRAKT) adapter: Retrofit): PeopleService {
    return adapter.create(PeopleService::class.java)
  }

  @Provides
  @Singleton
  fun provideRecommendationsService(@Named(NAMED_TRAKT) adapter: Retrofit): RecommendationsService {
    return adapter.create(RecommendationsService::class.java)
  }

  @Provides
  @Singleton
  fun provideSearchService(@Named(NAMED_TRAKT) adapter: Retrofit): SearchService {
    return adapter.create(SearchService::class.java)
  }

  @Provides
  @Singleton
  fun provideSeasonService(@Named(NAMED_TRAKT) adapter: Retrofit): SeasonService {
    return adapter.create(SeasonService::class.java)
  }

  @Provides
  @Singleton
  fun provideShowsService(@Named(NAMED_TRAKT) adapter: Retrofit): ShowsService {
    return adapter.create(ShowsService::class.java)
  }

  @Provides
  @Singleton
  fun provideSyncService(@Named(NAMED_TRAKT) adapter: Retrofit): SyncService {
    return adapter.create(SyncService::class.java)
  }

  @Provides
  @Singleton
  fun provideUsersService(@Named(NAMED_TRAKT) adapter: Retrofit): UsersService {
    return adapter.create(UsersService::class.java)
  }

  @Provides
  @Singleton
  @Named(NAMED_TRAKT)
  fun provideMoshi(): Moshi {
    val builder = Moshi.Builder()
      .add(IsoTimeAdapter())
      .add(ActionAdapter())
      .add(CommentTypeAdapter())
      .add(DepartmentAdapter())
      .add(ExtendedAdapter())
      .add(GenderAdapter())
      .add(GrantTypeAdapter())
      .add(HiddenSectionAdapter())
      .add(ItemTypeAdapter())
      .add(ItemTypesAdapter())
      .add(PrivacyAdapter())
      .add(ScopeAdapter())
      .add(ShowStatusAdapter())
      .add(TokenTypeAdapter())
      .add(KotlinJsonAdapterFactory())
    return builder.build()
  }

  companion object {
    const val API_URL = "https://api.trakt.tv"
    const val NAMED_TRAKT = "Trakt"
  }
}
