package mypoli.android.common.redux

import mypoli.android.common.redux.MiddleWare.Result.Continue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/20/2018.
 */

interface Action

interface SideEffect<S : State<S>> {

    suspend fun execute(action: Action, state: S, dispatcher: Dispatcher)

    fun canHandle(action: Action): Boolean
}

abstract class State<T>(
    private val stateData: Map<Class<Any>, Any>
) where T : State<T> {

    fun <S> stateFor(key: Class<S>): S {

        require(stateData.containsKey(key as Class<Any>))

        val data = stateData[key]

        return data as S
    }

    fun <S> update(stateKey: Class<S>, newState: S) =
        createWithData(stateData.plus(Pair(stateKey as Class<Any>, newState as Any)))

    val keys = stateData.keys

    abstract fun createWithData(stateData: Map<Class<Any>, Any>): T
}

interface Reducer<S : State<S>> {

    fun reduce(state: S, action: Action): S

    fun defaultState(): S

    val key: Class<S>
}

interface Dispatcher {
    fun <A : Action> dispatch(action: A)
}

class StateStore<S : State<S>>(
    private val reducer: Reducer<S>,
    middleware: List<MiddleWare<S>> = listOf()
) : Dispatcher {

    interface StateChangeSubscriber<in S> {

        fun onStateChanged(newState: S)
    }

    private var stateChangeSubscribers: CopyOnWriteArrayList<StateChangeSubscriber<S>> =
        CopyOnWriteArrayList()
    private var state = reducer.defaultState()
    private val middleWare = CompositeMiddleware<S>(middleware)

    override fun <A : Action> dispatch(action: A) {
        val res = middleWare.execute(state, this, action)
        if (res == Continue) changeState(action)
    }

    private fun changeState(action: Action) {
        val newState = reducer.reduce(state, action)
        state = newState
        notifyStateChanged(newState)
    }

    private fun notifyStateChanged(newState: S) {
        stateChangeSubscribers.forEach {
            it.onStateChanged(newState)
        }
    }

    fun <T> subscribe(subscriber: StateChangeSubscriber<S>) {
        stateChangeSubscribers.add(subscriber)
        subscriber.onStateChanged(state)
    }

    fun <T> unsubscribe(subscriber: StateChangeSubscriber<S>) {
        stateChangeSubscribers.remove(subscriber)
    }
}