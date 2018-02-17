package mypoli.android.common.mvi

import android.content.Context
import android.os.Bundle
import android.support.annotation.MainThread
import android.view.View
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RestoreViewOnCreateController
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch
import mypoli.android.common.di.Module
import mypoli.android.common.redux.State
import mypoli.android.myPoliApp
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 9/8/17.
 */
interface ViewState : State

interface Intent

abstract class MviViewController<in VS : ViewState, in V : ViewStateRenderer<VS>, out P : MviPresenter<V, VS, I>, in I : Intent>
protected constructor(args: Bundle? = null) : RestoreViewOnCreateController(args),
    ViewStateRenderer<VS>, Injects<Module> {

    private lateinit var intentChannel: SendChannel<I>

    init {
        val lifecycleListener = object : LifecycleListener() {

            private var presenter: P? = null

            override fun postCreateView(controller: Controller, view: View) {

                val isRestoringViewState = presenter != null

                if (!isRestoringViewState) {
                    presenter = createPresenter()
                    intentChannel = presenter!!.intentChannel()
                } else {
                    setRestoringViewState(true)
                }

                try {
                    @Suppress("UNCHECKED_CAST")
                    presenter?.onAttachView(this@MviViewController as V)
                } catch (e: ClassCastException) {
                    throw RuntimeException("Your view " + this@MviViewController.javaClass.simpleName + " must implement the View interface ")
                }

                if (isRestoringViewState) {
                    setRestoringViewState(false)
                }
            }

            override fun preDestroyView(controller: Controller, view: View) {
                val shouldRetainInstance = (controller.activity!!.isChangingConfigurations
                    || !controller.activity!!.isFinishing) && !controller.isBeingDestroyed

                if (shouldRetainInstance) {
                    presenter?.onDetachView()
                } else {
                    presenter?.onDestroy()
                    presenter = null
                }
            }

            override fun postDestroy(controller: Controller) {
                presenter = null
            }
        }
        addLifecycleListener(lifecycleListener)
    }

    protected var isRestoring = false

    private fun setRestoringViewState(isRestoring: Boolean) {
        this.isRestoring = isRestoring
    }

    protected abstract fun createPresenter(): P

    protected fun send(intent: I) {
        launch {
            intentChannel.send(intent)
        }
    }

    override fun onContextAvailable(context: Context) {
        inject(myPoliApp.module(context))
    }

    @MainThread
    override fun render(state: VS) {
        render(state, view!!)
    }

    abstract fun render(state: VS, view: View)

    fun View.sendOnClick(intent: I) {
        sendOnClickAndExec(intent, {})
    }

    fun View.sendOnClickAndExec(intent: I, block: () -> Unit) {
        setOnClickListener {
            send(intent)
            block()
        }
    }
}