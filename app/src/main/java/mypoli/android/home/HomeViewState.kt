package mypoli.android.home

import mypoli.android.common.AppState
import mypoli.android.common.BaseViewStateReducer
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 2/12/18.
 */
sealed class HomeAction : Action

object HomeReducer : BaseViewStateReducer<HomeViewState>() {

    override val stateKey = key<HomeViewState>()

    override fun reduce(state: AppState, subState: HomeViewState, action: Action) =
        state.dataState.player.let {
            subState.copy(
                showSignIn = if (it != null) !it.isLoggedIn() else true
            )
        }

    override fun defaultState() = HomeViewState(showSignIn = true)
}

data class HomeViewState(
    val showSignIn: Boolean
) : ViewState