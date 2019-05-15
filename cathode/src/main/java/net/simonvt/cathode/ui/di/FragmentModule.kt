package net.simonvt.cathode.ui.di

import dagger.Module
import net.simonvt.cathode.work.di.WorkerAssistedModule

@Module(includes = [FragmentModuleBinds::class, WorkerAssistedModule::class])
class FragmentModule
