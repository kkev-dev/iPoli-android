package mypoli.android.common.redux

import mypoli.android.common.UIAction
import mypoli.android.common.redux.MiddleWare.Result.Continue
import timber.log.Timber
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

interface Dispatcher {
    fun <A : Action> dispatch(action: A)
}

class StateStore<S : CombinedState<S>>(
    private val reducers: List<Reducer<S, *>>,
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

        fun reduce(state: S, action: Action, reducers: List<Reducer<S, *>>): S {

            val keyToReducer = reducers.map { it.key to it }.toMap()

            if (action is UIAction.Attach<*>) {
                Timber.d("Attaching ${action.stateKey}")
                require(keyToReducer.contains(action.stateKey))
                val reducer = keyToReducer[action.stateKey]!!
                return state.update(action.stateKey, reducer.defaultState())
            }

            if (action is UIAction.Detach<*>) {
                require(keyToReducer.contains(action.stateKey))
                return state.remove(action.stateKey)
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