package live.stream.theq.theqkit.di

import live.stream.theq.theqkit.TheQKit
import live.stream.theq.theqkit.repository.LiveGameRepository
import live.stream.theq.theqkit.ui.game.GameViewModel
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

internal val repositoryModule = module {
  single { LiveGameRepository(get(), get()) }
}

internal val theQKitModule = module {
  single { TheQKit.getInstance().restClient }
  single { TheQKit.getInstance().prefsHelper }
  single { TheQKit.getInstance().restClient.apiService }
}

internal val viewModelModule = module {
  viewModel { GameViewModel(get()) }
}
