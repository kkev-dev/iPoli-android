package io.ipoli.android.pet.shop

import io.ipoli.android.common.mvi.BaseMviPresenter
import io.ipoli.android.common.mvi.ViewStateRenderer
import io.ipoli.android.pet.shop.PetShopViewState.StateType.LOADING
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 12/4/17.
 */
class PetShopPresenter(
    coroutineContext: CoroutineContext
) : BaseMviPresenter<ViewStateRenderer<PetShopViewState>, PetShopViewState, PetShopIntent>(
    PetShopViewState(LOADING),
    coroutineContext
) {
    override fun reduceState(intent: PetShopIntent, state: PetShopViewState): PetShopViewState {
        return state
    }
}