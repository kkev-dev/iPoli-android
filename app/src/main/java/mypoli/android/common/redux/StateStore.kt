package mypoli.android.common.redux

import kotlinx.coroutines.experimental.launch
import mypoli.android.common.UIAction
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.MiddleWare.Result.Continue
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/20/2018.
 */

interface Action

interface State

interface SideEffect<in S : State> {

    suspend fun execute(action: Action, state: S, dispatcher: Dispatcher)

    fun canHandle(action: Action): Boolean
}

interface SideEffectExecutor<S : CombinedState<S>> {
    fun execute(sideEffect: SideEffect<S>, action: Action, state: S, dispatcher: Dispatcher)
}

class CoroutineSideEffectExecutor<S : CombinedState<S>>(
    private val coroutineContext: CoroutineContext
) : SideEffectExecutor<S> {
    override fun execute(
        sideEffect: SideEffect<S>,
        action: Action,
        state: S,
        dispatcher: Dispatcher
    ) {
        launch(coroutineContext) {
            sideEffect.execute(action, state, dispatcher)
        }
    }
}

abstract class CombinedState<T>(
    private val stateData: Map<Class<*>, State>
) : State where T : CombinedState<T> {

    fun <S> stateFor(key: Class<S>): S {

        require(stateData.containsKey(key))

        val data = stateData[key]

        @Suppress("unchecked_cast")
        return data as S
    }

    fun update(stateKey: Class<*>, newState: State) =
        createWithData(stateData.plus(Pair(stateKey, newState)))

    fun update(stateData: Map<Class<*>, State>) =
        createWithData(stateData.plus(stateData))

    fun remove(stateKey: Class<*>) =
        createWithData(stateData.minus(stateKey))

    val keys = stateData.keys

    protected abstract fun createWithData(stateData: Map<Class<*>, State>): T
}

interface Reducer<AS : CombinedState<AS>, S : State> {

    fun reduce(state: AS, action: Action) =
        reduce(state, state.stateFor(key), action)

    fun reduce(state: AS, subState: S, action: Action): S

    fun defaultState(): S

    val key: Class<S>
}

interface ViewStateReducer<S : CombinedState<S>, VS : ViewState> : Reducer<S, VS>

interface Dispatcher {
    fun <A : Action> dispatch(action: A)
}

class StateStore<S : CombinedState<S>>(
    initialState: S,
    reducers: Set<Reducer<S, *>>,
    sideEffects: Set<SideEffect<S>> = setOf(),
    private val sideEffectExecutor: SideEffectExecutor<S>,
    middleware: List<MiddleWare<S>> = listOf()
) : Dispatcher {

    interface StateChangeSubscriber<in S> {
        fun onStateChanged(newState: S)
    }

    private val storeEffects: CopyOnWriteArraySet<SideEffect<S>> =
        CopyOnWriteArraySet(sideEffects)

    private var stateChangeSubscribers: CopyOnWriteArraySet<StateChangeSubscriber<S>> =
        CopyOnWriteArraySet()

    private val storeReducers: CopyOnWriteArraySet<Reducer<S, *>> = CopyOnWriteArraySet(reducers)

    private var state = initialState
    private val middleWare = CompositeMiddleware(middleware)
    private val reducer = CombinedReducer<S>()

    override fun <A : Action> dispatch(action: A) {
        val res = middleWare.execute(state, this, action)
        if (res == Continue) changeState(action)
    }

    private fun changeState(action: Action) {
        val newState = reducer.reduce(state, action, storeReducers)
        state = newState
        notifyStateChanged(newState)
        executeSideEffects(action)
    }

    private fun executeSideEffects(action: Action) {
        storeEffects
            .filter { it.canHandle(action) }
            .forEach {
                sideEffectExecutor.execute(it, action, state, this)
            }
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

        fun reduce(state: S, action: Action, reducers: Set<Reducer<S, *>>): S {

            val keyToReducer = reducers.map { it.key to it }.toMap()

            if (action is UIAction.Attach<*>) {
                val stateKey = action.reducer.key
                require(keyToReducer.contains(stateKey))
                val reducer = keyToReducer[stateKey]!!
                return state.update(stateKey, reducer.defaultState())
            }

            if (action is UIAction.Detach<*>) {
                val stateKey = action.reducer.key
                require(keyToReducer.contains(stateKey))
                return state.remove(stateKey)
            }

            val newState = state.keys.map {
                val reducer = keyToReducer[it]!!
                val subState = reducer.reduce(state, action)
                it to subState
            }.toMap()

            return state.update(newState)
        }
    }
}