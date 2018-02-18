package mypoli.android.home

import mypoli.android.common.AppState
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.ViewStateReducer

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 2/12/18.
 */
sealed class HomeAction : Action

object HomeReducer : ViewStateReducer<AppState, HomeViewState> {
    override fun reduce(state: AppState, subState: HomeViewState, action: Action): HomeViewState {
        val player = state.dataState.player
        return subState.copy(
            showSignIn = if (player != null) !player.isLoggedIn() else true
        )
    }

    override fun defaultState() = HomeViewState(showSignIn = true)

    override val key = HomeViewState::class.java
}

data class HomeViewState(
    val showSignIn: Boolean
) : ViewState