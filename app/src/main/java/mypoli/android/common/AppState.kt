package mypoli.android.common

import mypoli.android.common.redux.Action
import mypoli.android.common.redux.CombinedState
import mypoli.android.common.redux.State

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 01/23/2018.
 */

sealed class LoadDataAction : Action {
    object All : LoadDataAction()
    data class ChangePlayer(val oldPlayerId: String) : LoadDataAction()
}

sealed class UIAction : Action {
    data class Attach<S : State>(val stateKey: Class<S>) : UIAction()

    data class Detach<S : State>(val stateKey: Class<S>) : UIAction()
}

class AppState(
    data: Map<Class<*>, State> = mapOf(
        Pair(
            AppDataState::class.java,
            AppDataReducer.defaultState()
        )
    )
) : CombinedState<AppState>(data) {

    val dataState: AppDataState = stateFor(AppDataState::class.java)

    override fun createWithData(stateData: Map<Class<*>, State>) = AppState(stateData)
}