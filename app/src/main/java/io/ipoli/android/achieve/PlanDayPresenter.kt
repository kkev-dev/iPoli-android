package io.ipoli.android.achieve

import io.ipoli.android.common.mvi.BaseMviPresenter
import io.ipoli.android.common.mvi.ViewStateRenderer
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 11/20/17.
 */
class PlanDayPresenter(
    coroutineContext: CoroutineContext
) : BaseMviPresenter<ViewStateRenderer<PlanDayViewState>, PlanDayViewState, PlanDayIntent>(
    PlanDayViewState(
        type = PlanDayViewState.StateType.LOADING,
        backgroundImageUrl = ""
    ),
    coroutineContext
) {
    override fun reduceState(intent: PlanDayIntent, state: PlanDayViewState) =
        when (intent) {
            is LoadDataIntent -> {
                state.copy(
                    type = PlanDayViewState.StateType.DATA_LOADED,
                    backgroundImageUrl = "https://images.unsplash.com/photo-1500817487388-039e623edc21?auto=format&fit=crop&q=100"
                )
            }
        }

}
