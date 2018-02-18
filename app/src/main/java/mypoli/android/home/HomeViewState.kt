package mypoli.android.home

import mypoli.android.common.AppState
import mypoli.android.common.UIReducer
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 2/12/18.
 */
sealed class HomeAction : Action

object HomeReducer : UIReducer<AppState, HomeViewState> {
    override fun reduce(state: AppState, uiState: HomeViewState, action: Action): HomeViewState {
        val player = state.dataState.player
        return uiState.copy(
            showSignIn = if (player != null) !player.isLoggedIn() else true
        )
    }

    override fun defaultState() = HomeViewState(showSignIn = true)

    override val key = HomeViewState::class.java
}

data class HomeViewState(
    val showSignIn: Boolean
) : ViewState