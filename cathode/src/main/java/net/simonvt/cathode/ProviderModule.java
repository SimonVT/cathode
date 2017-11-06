package net.simonvt.cathode;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module public abstract class ProviderModule {

  @ContributesAndroidInjector abstract CathodeInitProvider contributeInitProvider();
}
