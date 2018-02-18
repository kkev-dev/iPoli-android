package mypoli.android.player.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import mypoli.android.R
import mypoli.android.common.AppState
import mypoli.android.common.BaseViewStateReducer
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.android.ReduxViewController
import mypoli.android.common.view.CurrencyConverterDialogController

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/28/2018.
 */

data class GemInventoryViewState(val gems: Int) : ViewState

object GemInventoryReducer : BaseViewStateReducer<GemInventoryViewState>() {

    override val stateKey = key<GemInventoryViewState>()

    override fun reduce(
        state: AppState,
        subState: GemInventoryViewState,
        action: Action
    ) = GemInventoryViewState(gems = state.dataState.player?.gems ?: 0)

    override fun defaultState() = GemInventoryViewState(0)
}

class GemInventoryViewController :
    ReduxViewController<Action, GemInventoryViewState, GemInventoryReducer> {

    private var showCurrencyConverter: Boolean = true


    override val reducer = GemInventoryReducer

    constructor(args: Bundle? = null) : super(args)

    constructor(showCurrencyConverter: Boolean) : super() {
        this.showCurrencyConverter = showCurrencyConverter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.controller_gem_inventory, container, false)
        if (showCurrencyConverter) {
            view.setOnClickListener {
                CurrencyConverterDialogController().showDialog(
                    parentController!!.router,
                    "currency-converter"
                )
            }
        } else {
            view.setOnClickListener(null)
        }
        return view
    }

    override fun render(state: GemInventoryViewState, view: View) {
        (view as TextView).text = state.gems.toString()
    }
}