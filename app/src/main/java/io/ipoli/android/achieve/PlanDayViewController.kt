package io.ipoli.android.achieve

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.ipoli.android.R
import io.ipoli.android.common.mvi.Intent
import io.ipoli.android.common.mvi.MviViewController
import io.ipoli.android.common.mvi.ViewState
import io.ipoli.android.common.mvi.ViewStateRenderer
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 11/20/17.
 */

sealed class PlanDayIntent : Intent

object LoadDataIntent : PlanDayIntent()

data class PlanDayViewState(
    val type: StateType = StateType.LOADING,
    val backgroundImageUrl: String
) : ViewState {
    enum class StateType {
        LOADING, DATA_LOADED
    }
}

class PlanDayViewController(args: Bundle? = null) :
    MviViewController<PlanDayViewState, PlanDayViewController, PlanDayPresenter, PlanDayIntent>(args),
    ViewStateRenderer<PlanDayViewState> {

    private val presenter by required { planDayPresenter }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View =
        inflater.inflate(R.layout.controller_plan_day, container, false)

    override fun onAttach(view: View) {
        super.onAttach(view)
        send(LoadDataIntent)
    }

    override fun createPresenter() = presenter

    override fun render(state: PlanDayViewState, view: View) {
        if (state.type == PlanDayViewState.StateType.DATA_LOADED) {

            val options = RequestOptions().override(1080, 1920)

            Glide.with(view.context)
                .load(state.backgroundImageUrl)
                .apply(options)
                .into(object : SimpleTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable?, transition: Transition<in Drawable>?) {
                        view.background = resource
                    }
                })
        }
    }

}