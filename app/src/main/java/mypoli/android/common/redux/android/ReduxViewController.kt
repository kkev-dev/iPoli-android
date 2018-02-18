package mypoli.android.common.redux.android

import android.content.Context
import android.os.Bundle
import android.view.View
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RestoreViewOnCreateController
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import mypoli.android.common.AppState
import mypoli.android.common.UIAction
import mypoli.android.common.di.Module
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.StateStore
import mypoli.android.common.redux.ViewStateReducer
import mypoli.android.myPoliApp
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 1/18/18.
 */
abstract class ReduxViewController<A : Action, VS : ViewState, out R : ViewStateReducer<AppState, VS>> protected constructor(
    args: Bundle? = null
) : RestoreViewOnCreateController(args), Injects<Module>,
    StateStore.StateChangeSubscriber<AppState> {

    private val stateStore by required { stateStore }

    protected abstract val reducer: R

    @Volatile
    private var currentState: VS? = null

    override fun onContextAvailable(context: Context) {
        inject(myPoliApp.module(context))
    }

    init {
        val lifecycleListener = object : LifecycleListener() {

            override fun postAttach(controller: Controller, view: View) {
                stateStore.dispatch(UIAction.Attach(reducer))
                stateStore.subscribe(this@ReduxViewController)
                onCreateLoadAction()?.let {
                    stateStore.dispatch(it)
                }
            }

            override fun preDetach(controller: Controller, view: View) {
                stateStore.unsubscribe(this@ReduxViewController)
                stateStore.dispatch(UIAction.Detach(reducer))
                currentState = null
            }
        }
        addLifecycleListener(lifecycleListener)
    }

    fun dispatch(action: A) {
        stateStore.dispatch(action)
    }

    override fun onStateChanged(newState: AppState) {
        val viewState = newState.stateFor(reducer.stateKey)
        if (viewState != currentState) {
            currentState = viewState

            launch(UI) {
                render(viewState, view!!)
            }
        }
    }

    protected open fun onCreateLoadAction(): A? {
        return null
    }

    abstract fun render(state: VS, view: View)

    fun View.dispatchOnClick(intent: A) {
        dispatchOnClickAndExec(intent, {})
    }

    fun View.dispatchOnClickAndExec(intent: A, block: () -> Unit) {
        setOnClickListener {
            dispatch(intent)
            block()
        }
    }
}