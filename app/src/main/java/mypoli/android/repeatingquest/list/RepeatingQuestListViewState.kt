package mypoli.android.repeatingquest.list

import mypoli.android.common.AppState
import mypoli.android.common.BaseViewStateReducer
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 2/14/18.
 */
sealed class RepeatingQuestListAction : Action


object RepeatingQuestListReducer : BaseViewStateReducer<RepeatingQuestListViewState>() {

    override val stateKey = key<RepeatingQuestListViewState>()

    override fun reduce(
        state: AppState,
        subState: RepeatingQuestListViewState,
        action: Action
    ): RepeatingQuestListViewState {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun defaultState() =
        RepeatingQuestListViewState(RepeatingQuestListViewState.StateType.LOADING)

}

data class RepeatingQuestListViewState(
    val type: RepeatingQuestListViewState.StateType
) : ViewState {
    enum class StateType {
        LOADING
    }
}