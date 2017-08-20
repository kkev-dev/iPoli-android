package io.ipoli.android.quest.overview

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.ipoli.android.quest.overview.ui.OverviewController
import io.ipoli.android.quest.overview.ui.OverviewInitialLoadingState
import io.ipoli.android.quest.overview.ui.OverviewStatePartialChange
import io.ipoli.android.quest.overview.ui.OverviewViewState
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 8/20/17.
 */
class OverviewPresenter @Inject constructor(private val displayOverviewQuestsUseCase: DisplayOverviewQuestsUseCase) : MviBasePresenter<OverviewController, OverviewViewState>() {
    override fun bindIntents() {

        val observables = listOf<Observable<OverviewStatePartialChange>>(
            intent { it.loadQuestsIntent() }.switchMap { parameters ->
                Timber.d("Loading")
                displayOverviewQuestsUseCase.execute(parameters)
            }
        )

        val allIntents: Observable<OverviewStatePartialChange> = Observable.merge(observables)
        val initialState: OverviewViewState = OverviewInitialLoadingState()
        val stateObservable = allIntents.scan(initialState, this::viewStateReducer)
            .observeOn(AndroidSchedulers.mainThread())

        subscribeViewState(stateObservable, OverviewController::render)
    }

    private fun viewStateReducer(prevState: OverviewViewState, statePartialChange: OverviewStatePartialChange): OverviewViewState {
        return statePartialChange.computeNewState(prevState)
    }

}