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
import mypoli.android.common.UIReducer
import mypoli.android.common.di.Module
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.StateStore
import mypoli.android.myPoliApp
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 1/18/18.
 */
abstract class ReduxViewController<in A : Action, VS : ViewState, out R : UIReducer<AppState, VS>> protected constructor(
    args: Bundle? = null
) :
    RestoreViewOnCreateController(args), Injects<Module>,
    StateStore.StateChangeSubscriber<AppState> {

    private val stateStore by required { stateStore }

    protected abstract val reducer: R

    private var currentState: VS? = null

    override fun onContextAvailable(context: Context) {
        inject(myPoliApp.module(context))
    }

    init {
        val lifecycleListener = object : LifecycleListener() {

            override fun postAttach(controller: Controller, view: View) {
                stateStore.dispatch(UIAction.Attach(reducer.key))
                stateStore.subscribe(this@ReduxViewController)
            }

            override fun preDetach(controller: Controller, view: View) {
                stateStore.unsubscribe(this@ReduxViewController)
                stateStore.dispatch(UIAction.Detach(reducer.key))
                currentState = null
            }
        }
        addLifecycleListener(lifecycleListener)
    }

    fun dispatch(action: A) {
        stateStore.dispatch(action)
    }

    override fun onStateChanged(newState: AppState) {
        val newState = newState.stateFor(reducer.key)
        if(newState != currentState) {
            currentState = newState

            launch(UI) {

                render(newState, view!!)
            }
        }


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