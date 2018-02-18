package mypoli.android.common

import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.CombinedState
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
    data class Attach(val reducer: UIReducer<*, *>) : UIAction()

    data class Detach(val reducer: UIReducer<*, *>) : UIAction()
}

//interface UIState

interface UIReducer<S : CombinedState<S>, VS : ViewState> {
    fun reduce(state: S, action: Action) =
        reduce(state, state.stateFor(key), action)

    fun reduce(state: S, uiState: VS, action: Action): VS

    fun defaultState(): VS

    val key: Class<VS>
}

class AppState(
    data: Map<Class<*>, State> = mapOf(
        Pair(
            AppDataState::class.java,
            AppDataReducer.defaultState()
        )
    )
)
//    val appDataState: AppDataState,
////    val scheduleState: ScheduleState,
//    val calendarState: CalendarState,
//    val agendaState: AgendaState,
//    val petStoreState: PetStoreState,
//    val challengeListForCategoryState: ChallengeListForCategoryState,
//    val authState: AuthState,
//    val repeatingQuestListState: RepeatingQuestListState,
//    val uiState: Map<String, ViewState>
    : CombinedState<AppState>(data) {

    val dataState: AppDataState = stateFor(AppDataState::class.java)

    override fun createWithData(stateData: Map<Class<*>, State>) = AppState(stateData)
}