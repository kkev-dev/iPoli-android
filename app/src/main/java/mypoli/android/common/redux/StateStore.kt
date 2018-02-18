package mypoli.android.common.redux

import mypoli.android.common.UIAction
import mypoli.android.common.redux.MiddleWare.Result.Continue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/20/2018.
 */

interface Action

interface State

interface SideEffect<S : State> {

    suspend fun execute(action: Action, state: S, dispatcher: Dispatcher)

    fun canHandle(action: Action): Boolean
}

abstract class CombinedState<T>(
    private val stateData: Map<Class<*>, State>
) : State where T : CombinedState<T> {

    fun <S> stateFor(key: Class<S>): S {

        require(stateData.containsKey(key))

        val data = stateData[key]

        return data as S
    }

    fun update(stateKey: Class<*>, newState: State) =
        createWithData(stateData.plus(Pair(stateKey, newState)))

    fun update(stateData: Map<Class<*>, State>) =
        createWithData(stateData.plus(stateData))

    val keys = stateData.keys

    protected abstract fun createWithData(stateData: Map<Class<*>, State>): T
}

interface Reducer<S : State> {

    fun reduce(state: S, action: Action): S

    fun defaultState(): S

    val key: Class<S>
}

interface Dispatcher {
    fun <A : Action> dispatch(action: A)
}

class StateStore<S : CombinedState<S>>(
    private val reducers: List<Reducer<S>>,
    defaultState: S,
    middleware: List<MiddleWare<S>> = listOf()
) : Dispatcher {

    interface StateChangeSubscriber<in S> {

        fun onStateChanged(newState: S)
    }

    private var stateChangeSubscribers: CopyOnWriteArrayList<StateChangeSubscriber<S>> =
        CopyOnWriteArrayList()
    private var state = defaultState
    private val middleWare = CompositeMiddleware<S>(middleware)
    private val reducer = CombinedReducer<S>()

    override fun <A : Action> dispatch(action: A) {
        val res = middleWare.execute(state, this, action)
        if (res == Continue) changeState(action)
    }

    private fun changeState(action: Action) {
        val newState = reducer.reduce(state, action, reducers)
        state = newState
        notifyStateChanged(newState)
    }

    private fun notifyStateChanged(newState: S) {
        stateChangeSubscribers.forEach {
            it.onStateChanged(newState)
        }
    }

    fun subscribe(subscriber: StateChangeSubscriber<S>) {
        stateChangeSubscribers.add(subscriber)
        subscriber.onStateChanged(state)
    }

    fun unsubscribe(subscriber: StateChangeSubscriber<S>) {
        stateChangeSubscribers.remove(subscriber)
    }


    class CombinedReducer<S : CombinedState<S>> {

        fun reduce(state: S, action: Action, reducers: List<Reducer<S>>): S {
            if (action is UIAction.Attach) {
                val key = action.reducer.key
                val s = action.reducer.defaultState()
                return state.update(key, s)
            }

            val newState = reducers.map {
                val subState = it.reduce(state, action)
                it.key as Class<*> to subState
            }.toMap()

            return state.update(newState)

//            return newState

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
}