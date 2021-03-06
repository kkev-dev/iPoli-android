package mypoli.android.common.redux.android

import android.content.Context
import android.os.Bundle
import android.view.View
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RestoreViewOnCreateController
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import mypoli.android.common.AppState
import mypoli.android.common.di.Module
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.StateStore
import mypoli.android.myPoliApp
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import timber.log.Timber

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 1/18/18.
 */
abstract class ReduxViewController<in A : Action, VS : ViewState, out P : AndroidStatePresenter<AppState, VS>> protected constructor(
    args: Bundle? = null
) :
    RestoreViewOnCreateController(args), Injects<Module>,
    StateStore.StateChangeSubscriber<AppState, VS> {

    private val stateStore by required { stateStore }

    protected abstract val presenter: P

    override fun onContextAvailable(context: Context) {
        inject(myPoliApp.module(context))
    }

    init {
        val lifecycleListener = object : LifecycleListener() {

            override fun postAttach(controller: Controller, view: View) {
                stateStore.subscribe(this@ReduxViewController)
            }

            override fun preDetach(controller: Controller, view: View) {
                stateStore.unsubscribe(this@ReduxViewController)
            }
        }
        addLifecycleListener(lifecycleListener)
    }

    override val transformer: StateStore.StateChangeSubscriber.StateTransformer<AppState, VS>
        get() = object : StateStore.StateChangeSubscriber.StateTransformer<AppState, VS> {

            override fun transformInitial(state: AppState): VS =
                presenter.presentInitial(presenter.present(state, activity!!))

            override fun transform(state: AppState): VS =
                presenter.present(state, activity!!)
        }

    fun dispatch(action: A) {
        stateStore.dispatch(action)
    }

    override fun onStateChanged(newState: VS) {
        launch(UI) {
            Timber.d("AAA render $this")
            render(newState, view!!)
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