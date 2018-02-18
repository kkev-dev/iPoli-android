package mypoli.android.common

import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.CombinedState
import mypoli.android.common.redux.Reducer
import mypoli.android.common.redux.State

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 01/23/2018.
 */

//interface AppStateReducer<out S : State> : Reducer<AppState, S>

sealed class LoadDataAction : Action {
    object All : LoadDataAction()
    data class ChangePlayer(val oldPlayerId: String) : LoadDataAction()
}

sealed class UIAction : Action {
    data class Attach<S : State>(val stateKey: Class<S>) : UIAction()

    data class Detach<S : State>(val stateKey: Class<S>) : UIAction()
}

//interface UIState

interface UIReducer<S : CombinedState<S>, VS : ViewState> : Reducer<S, VS>

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