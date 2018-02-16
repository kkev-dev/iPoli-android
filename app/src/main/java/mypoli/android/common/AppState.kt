package mypoli.android.common

import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
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
    data class Attach(val reducer: UIReducer<*>) : UIAction()

    data class Detach(val reducer: UIReducer<*>) : UIAction()
}

//interface UIState

interface UIReducer<VS : ViewState> {
    fun reduce(state: AppState, action: Action): VS

    fun defaultState(): VS

    val key: Class<VS>
}

class AppState(
    data: Map<Class<Any>, Any> = mapOf(
        Pair(
            AppDataState::class.java as Class<Any>,
            AppDataReducer.defaultState() as Any
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
    : State<AppState>(data) {

    val dataState: AppDataState = stateFor(AppDataState::class.java)

    override fun createWithData(stateData: Map<Class<Any>, Any>) = AppState(stateData)
}

object CombinedReducer {

    fun reduce(state: AppState, action: Action, reducers: List<Reducer<AppState>>): AppState {
        if (action is UIAction.Attach) {
            val key = action.reducer.key
            val s = action.reducer.defaultState()
            return state.update(key as Class<Any>, s as Any)

//            return state.copy(
//                uiState = state.uiState + Pair("state_key", action.reducer.defaultState())
//            )
        }

        return AppState(reducers.map {
            //            val s = state.stateFor(it.key)
            val subState = it.reduce(state, action)
            it.key to subState
        }.toMap() as Map<Class<Any>, Any>)

//        return state.copy(
//            appDataState = AppDataReducer.reduce(state, action),
////        scheduleState = ScheduleReducer.reduce(state, action),
////            uiState = ScheduleReducer.reduce(state, action),
//            calendarState = CalendarReducer.reduce(state, action),
//            agendaState = AgendaReducer.reduce(state, action),
//            petStoreState = PetStoreReducer.reduce(state, action),
//            authState = AuthReducer.reduce(state, action),
//            repeatingQuestListState = RepeatingQuestListReducer.reduce(state, action)
//        )
    }

//    override fun defaultState() =
//        AppState(
//            appDataState = AppDataReducer.defaultState(),
////            scheduleState = ScheduleReducer.defaultState(),
//            uiState = mapOf(),
//            calendarState = CalendarReducer.defaultState(),
//            agendaState = AgendaReducer.defaultState(),
//            petStoreState = PetStoreReducer.defaultState(),
//            challengeListForCategoryState = ChallengeListForCategoryReducer.defaultState(),
//            authState = AuthReducer.defaultState(),
//            repeatingQuestListState = RepeatingQuestListReducer.defaultState()
//        )
}