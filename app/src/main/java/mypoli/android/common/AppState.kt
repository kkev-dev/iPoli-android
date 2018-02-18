package mypoli.android.common

import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.CompositeState
import mypoli.android.common.redux.State
import mypoli.android.common.redux.ViewStateReducer

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 01/23/2018.
 */

sealed class LoadDataAction : Action {
    object All : LoadDataAction()
    data class ChangePlayer(val oldPlayerId: String) : LoadDataAction()
}

sealed class UIAction : Action {
    data class Attach<S : CompositeState<S>>(val reducer: ViewStateReducer<S, *>) : UIAction()

    data class Detach<S : CompositeState<S>>(val reducer: ViewStateReducer<S, *>) : UIAction()
}

abstract class BaseViewStateReducer<VS : ViewState> : ViewStateReducer<AppState, VS> {
    inline fun <reified VS> key() = VS::class.java
}

class AppState(
    data: Map<Class<*>, State>
) : CompositeState<AppState>(data) {

    val dataState: AppDataState = stateFor(AppDataState::class.java)

    override fun createWithData(stateData: Map<Class<*>, State>) = AppState(stateData)
}