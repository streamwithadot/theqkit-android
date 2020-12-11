package live.stream.theq.theqkit.di

import live.stream.theq.theqkit.TheQKit
import live.stream.theq.theqkit.repository.LiveGameRepository
import live.stream.theq.theqkit.ui.game.DefaultProfilePhotoLoader
import live.stream.theq.theqkit.ui.game.GameViewModel
import live.stream.theq.theqkit.ui.game.GameWinnerProfilePhotoLoader
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val repositoryModule = module {
  single { LiveGameRepository(get(), get()) }
}

internal val theQKitModule = module {
  single { TheQKit.getInstance().restClient }
  single { TheQKit.getInstance().prefsHelper }
  single { TheQKit.getInstance().restClient.apiService }
  single { DefaultProfilePhotoLoader() as GameWinnerProfilePhotoLoader } //cast is necessary
}

internal val viewModelModule = module {
  viewModel { GameViewModel(get()) }
}