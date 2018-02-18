package mypoli.android.pet.store

import mypoli.android.R
import mypoli.android.common.AppState
import mypoli.android.common.BaseViewStateReducer
import mypoli.android.common.DataLoadedAction
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.pet.AndroidPetAvatar
import mypoli.android.pet.PetAvatar
import mypoli.android.pet.PetMood
import mypoli.android.player.Player

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 12/4/17.
 */
sealed class PetStoreAction : Action {
    data class BuyPet(val pet: PetAvatar) : PetStoreAction()

    data class UnlockPet(val pet: PetAvatar) : PetStoreAction()

    data class ChangePet(val pet: PetAvatar) : PetStoreAction()

    object PetBought : PetStoreAction()

    object PetTooExpensive : PetStoreAction()
}

object PetStoreReducer : BaseViewStateReducer<PetStoreViewState>() {

    override val stateKey = key<PetStoreViewState>()

    override fun reduce(
        state: AppState,
        subState: PetStoreViewState,
        action: Action
    ): PetStoreViewState {
        val petStoreState = subState.copy(
            playerGems = state.dataState.player?.gems ?: 0
        )
        return when (action) {
            is DataLoadedAction.PlayerChanged -> {
                val player = action.player
                petStoreState.copy(
                    type = PetStoreViewState.StateType.DATA_CHANGED,
                    pets = createPetModels(player)
                )
            }

            is PetStoreAction.BuyPet -> {
                petStoreState.copy(
                    type = PetStoreViewState.StateType.LOADING
                )
            }

            PetStoreAction.PetBought -> {
                petStoreState.copy(
                    type = PetStoreViewState.StateType.PET_BOUGHT
                )
            }

            PetStoreAction.PetTooExpensive -> {
                petStoreState.copy(
                    type = PetStoreViewState.StateType.PET_TOO_EXPENSIVE
                )
            }

            is PetStoreAction.UnlockPet -> {
                petStoreState.copy(
                    type = PetStoreViewState.StateType.SHOW_GEM_STORE
                )
            }
            is PetStoreAction.ChangePet -> {
                petStoreState.copy(
                    type = PetStoreViewState.StateType.LOADING
                )
            }
            else -> petStoreState
        }
    }

    private fun createPetModels(player: Player) =
        PetAvatar.values().map {
            PetStoreViewState.PetModel(
                avatar = it,
                isBought = player.hasPet(it),
                isCurrent = player.pet.avatar == it,
                isLocked = (it == PetAvatar.DOG && !player.hasPet(PetAvatar.DOG))
            )
        }

    override fun defaultState() =
        PetStoreViewState(
            type = PetStoreViewState.StateType.LOADING,
            playerGems = 0,
            pets = listOf()
        )
}

data class PetStoreViewState(
    val type: StateType = StateType.DATA_CHANGED,
    val playerGems: Int = 0,
    val pets: List<PetModel> = listOf()
) : ViewState {
    data class PetModel(
        val avatar: PetAvatar,
        val isBought: Boolean,
        val isCurrent: Boolean,
        val isLocked: Boolean
    )

    enum class StateType {
        LOADING,
        DATA_CHANGED,
        PET_TOO_EXPENSIVE,
        PET_BOUGHT,
        PET_CHANGED,
        SHOW_GEM_STORE
    }
}


fun PetStoreViewState.PetModel.toAndroidPetModel(): PetStoreViewController.PetViewModel {
    val androidAvatar = AndroidPetAvatar.valueOf(avatar.name)

    return when {
        isCurrent -> {
            PetStoreViewController.PetViewModel(
                avatar = avatar,
                name = androidAvatar.petName,
                image = androidAvatar.image,
                price = avatar.gemPrice.toString(),
                description = androidAvatar.description,
                actionText = null,
                moodImage = androidAvatar.moodImage[PetMood.HAPPY]!!,
                showAction = false,
                showIsCurrent = true,
                action = null
            )
        }

        isBought -> {
            PetStoreViewController.PetViewModel(
                avatar = avatar,
                name = androidAvatar.petName,
                image = androidAvatar.image,
                price = avatar.gemPrice.toString(),
                description = androidAvatar.description,
                actionText = R.string.store_pet_in_inventory,
                moodImage = androidAvatar.moodImage[PetMood.GOOD]!!,
                showAction = true,
                showIsCurrent = false,
                action = PetStoreViewController.PetViewModel.Action.CHANGE
            )
        }

        isLocked -> {
            PetStoreViewController.PetViewModel(
                avatar = avatar,
                name = androidAvatar.petName,
                image = androidAvatar.image,
                price = avatar.gemPrice.toString(),
                description = androidAvatar.description,
                actionText = R.string.unlock,
                moodImage = androidAvatar.moodImage[PetMood.GOOD]!!,
                showAction = true,
                showIsCurrent = false,
                action = PetStoreViewController.PetViewModel.Action.UNLOCK
            )
        }

        else -> {
            PetStoreViewController.PetViewModel(
                avatar = avatar,
                name = androidAvatar.petName,
                image = androidAvatar.image,
                price = avatar.gemPrice.toString(),
                description = androidAvatar.description,
                actionText = R.string.store_buy_pet,
                moodImage = androidAvatar.moodImage[PetMood.GOOD]!!,
                showAction = true,
                showIsCurrent = false,
                action = PetStoreViewController.PetViewModel.Action.BUY
            )
        }
    }
}