package mypoli.android.pet.store

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import kotlinx.android.synthetic.main.controller_pet_store.view.*
import kotlinx.android.synthetic.main.item_pet_store.view.*
import kotlinx.android.synthetic.main.view_inventory_toolbar.view.*
import mypoli.android.R
import mypoli.android.common.ViewUtils
import mypoli.android.common.redux.android.ReduxViewController
import mypoli.android.common.view.CurrencyConverterDialogController
import mypoli.android.common.view.setToolbar
import mypoli.android.common.view.showBackButton
import mypoli.android.common.view.visible
import mypoli.android.pet.PetAvatar
import mypoli.android.pet.store.PetStoreAction.*
import mypoli.android.pet.store.PetStoreViewState.StateType.*
import mypoli.android.store.GemStoreViewController

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 12/4/17.
 */
class PetStoreViewController(args: Bundle? = null) :
    ReduxViewController<PetStoreAction, PetStoreViewState, PetStoreReducer>(
        args
    ) {

    override val reducer = PetStoreReducer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.controller_pet_store, container, false)

        setToolbar(view.toolbar)

        view.toolbarTitle.setText(R.string.pet_store)

        view.petPager.clipToPadding = false
        view.petPager.pageMargin = ViewUtils.dpToPx(16f, view.context).toInt()

        view.petPager.adapter = PetPagerAdapter()

        return view
    }

    override fun onAttach(view: View) {
        showBackButton()
        super.onAttach(view)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            router.popCurrentController()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun render(state: PetStoreViewState, view: View) {
        when (state.type) {

            DATA_CHANGED -> {
                (view.petPager.adapter as PetPagerAdapter).updateAll(state.pets.map { it.toAndroidPetModel() })
            }

            PET_TOO_EXPENSIVE -> {
                showCurrencyConverter()
                Toast.makeText(view.context, "Pet too expensive", Toast.LENGTH_SHORT).show()
            }

            SHOW_GEM_STORE -> {
                showGemStore()
            }

            else -> {
            }
        }
    }

    private fun showCurrencyConverter() {
        CurrencyConverterDialogController().showDialog(router, "currency-converter")
    }

    private fun showGemStore() {
        val handler = FadeChangeHandler()
        router.pushController(
            RouterTransaction.with(GemStoreViewController())
                .pushChangeHandler(handler)
                .popChangeHandler(handler)
        )
    }

    inner class PetPagerAdapter(private var viewModels: List<PetViewModel> = listOf()) :
        PagerAdapter() {

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(container.context)
            val view = inflater.inflate(R.layout.item_pet_store, container, false)
            val vm = viewModels[position]
            val avatar = vm.avatar
            view.petName.setText(vm.name)
            view.pet.setImageResource(vm.image)
            view.petPrice.text = PetAvatar.valueOf(avatar.name).gemPrice.toString()
            view.petDescription.setText(vm.description)
            val action = view.petAction
            val current = view.currentPet

            action.visible = vm.showAction
            current.visible = vm.showIsCurrent
            view.petState.setImageResource(vm.moodImage)

            vm.actionText?.let {
                action.setText(it)
            }

            action.setOnClickListener(null)

            when (vm.action) {
                PetViewModel.Action.CHANGE -> {
                    action.dispatchOnClick(ChangePet(vm.avatar))
                }

                PetViewModel.Action.UNLOCK -> {
                    action.dispatchOnClick(UnlockPet(vm.avatar))
                }

                PetViewModel.Action.BUY -> {
                    action.dispatchOnClick(BuyPet(vm.avatar))
                }
            }

            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
            container.removeView(view as View)
        }

        override fun isViewFromObject(view: View, `object`: Any) = view == `object`

        override fun getCount() = viewModels.size

        override fun getItemPosition(`object`: Any) = PagerAdapter.POSITION_NONE

        fun updateAll(viewModels: List<PetViewModel>) {
            this.viewModels = viewModels
            notifyDataSetChanged()
        }

    }

    data class PetViewModel(
        val avatar: PetAvatar,
        @StringRes val name: Int,
        @DrawableRes val image: Int,
        val price: String,
        @StringRes val description: Int,
        @StringRes val actionText: Int?,
        @DrawableRes val moodImage: Int,
        val showAction: Boolean,
        val showIsCurrent: Boolean,
        val action: Action?
    ) {
        enum class Action {
            CHANGE, UNLOCK, BUY
        }
    }
}