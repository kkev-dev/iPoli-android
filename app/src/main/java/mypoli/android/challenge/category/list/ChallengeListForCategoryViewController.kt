package mypoli.android.challenge.category.list

import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import kotlinx.android.synthetic.main.controller_challenge_list_for_category.view.*
import kotlinx.android.synthetic.main.item_buy_challenge.view.*
import kotlinx.android.synthetic.main.view_inventory_toolbar.view.*
import mypoli.android.R
import mypoli.android.challenge.PersonalizeChallengeViewController
import mypoli.android.challenge.category.list.ChallengeListForCategoryViewState.StateType.*
import mypoli.android.challenge.data.AndroidPredefinedChallenge
import mypoli.android.challenge.data.Challenge
import mypoli.android.challenge.data.PredefinedChallenge
import mypoli.android.common.redux.android.ReduxViewController
import mypoli.android.common.view.*
import mypoli.android.player.inventory.GemInventoryViewController

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/30/17.
 */
class ChallengeListForCategoryViewController :
    ReduxViewController<ChallengeListForCategoryAction, ChallengeListForCategoryViewState, ChallengeListForCategoryReducer> {

    override val reducer = ChallengeListForCategoryReducer

    private lateinit var challengeCategory: Challenge.Category

    constructor(challengeCategory: Challenge.Category) : this() {
        this.challengeCategory = challengeCategory
    }

    constructor(args: Bundle? = null) : super(args)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val view =
            inflater.inflate(R.layout.controller_challenge_list_for_category, container, false)
        setToolbar(view.toolbar)

        val androidChallengeCategory =
            AndroidPredefinedChallenge.Category.valueOf(challengeCategory.name)

        view.toolbarTitle.setText(androidChallengeCategory.title)
        view.challengeList.layoutManager =
            LinearLayoutManager(container.context, LinearLayoutManager.VERTICAL, false)
        view.challengeList.adapter = ChallengeAdapter()

        setChildController(view.playerGems, GemInventoryViewController())

        return view
    }

    override fun onCreateLoadAction() =
        ChallengeListForCategoryAction.LoadData(challengeCategory)

    private fun showCurrencyConverter() {
        CurrencyConverterDialogController().showDialog(router, "currency-converter")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            router.popCurrentController()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(view: View) {
        showBackButton()
        super.onAttach(view)
    }

    override fun render(state: ChallengeListForCategoryViewState, view: View) {
        when (state.type) {
            PLAYER_CHANGED -> {
                (view.challengeList.adapter as ChallengeAdapter).updateAll(state.challenges.map { it.toAndroidChallenge() })
            }

            CHALLENGE_TOO_EXPENSIVE -> {
                showCurrencyConverter()
                Toast.makeText(
                    view.context,
                    stringRes(R.string.challenge_too_expensive),
                    Toast.LENGTH_SHORT
                ).show()
            }

            CHALLENGE_BOUGHT -> {
                Toast.makeText(
                    view.context,
                    stringRes(R.string.challenge_unlocked),
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
            }
        }
    }

    private fun showChallenge(challenge: PredefinedChallenge) {
        val handler = FadeChangeHandler()
        router.pushController(
            RouterTransaction.with(
                PersonalizeChallengeViewController(
                    challenge
                )
            )
                .pushChangeHandler(handler)
                .popChangeHandler(handler)
        )
    }

    data class ChallengeViewModel(
        @StringRes val name: Int,
        @StringRes val description: Int,
        @ColorRes val backgroundColor: Int,
        @DrawableRes val image: Int,
        val gemPrice: Int,
        val isBought: Boolean,
        val challenge: PredefinedChallenge
    )

    inner class ChallengeAdapter(private var viewModels: List<ChallengeViewModel> = listOf()) :
        RecyclerView.Adapter<ChallengeAdapter.ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val vm = viewModels[position]
            val itemView = holder.itemView

            val priceIndicator = itemView.challengePriceIndicator
            if (!vm.isBought) {

                if (vm.gemPrice == 0) {
                    priceIndicator.text = stringRes(R.string.free).toUpperCase()
                    priceIndicator.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                } else {
                    priceIndicator.text = "${vm.gemPrice}"
                    priceIndicator.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_gem_24dp),
                        null,
                        null,
                        null
                    )
                }

                itemView.dispatchOnClick(
                    ChallengeListForCategoryAction.BuyChallenge(
                        vm.challenge
                    )
                )
            } else {
                itemView.setOnClickListener {
                    showChallenge(vm.challenge)
                }
                priceIndicator.text = stringRes(R.string.unlocked).toUpperCase()
                priceIndicator.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }

            itemView.challengeContainer.setCardBackgroundColor(colorRes(vm.backgroundColor))
            itemView.challengeName.setText(vm.name)
            itemView.challengeDescription.setText(vm.description)
            itemView.challengeBackgroundImage.setImageResource(vm.image)
        }

        override fun getItemCount() = viewModels.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_buy_challenge,
                    parent,
                    false
                )
            )

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

        fun updateAll(viewModels: List<ChallengeViewModel>) {
            this.viewModels = viewModels
            notifyDataSetChanged()
        }

    }

}