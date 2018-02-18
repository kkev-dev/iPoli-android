package mypoli.android.challenge.category.list

import mypoli.android.challenge.data.AndroidPredefinedChallenge
import mypoli.android.challenge.data.Challenge
import mypoli.android.challenge.data.PredefinedChallenge
import mypoli.android.common.AppState
import mypoli.android.common.BaseViewStateReducer
import mypoli.android.common.DataLoadedAction
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/29/17.
 */

sealed class ChallengeListForCategoryAction : Action {

    data class LoadData(val challengeCategory: Challenge.Category) :
        ChallengeListForCategoryAction()

    data class BuyChallenge(val challenge: PredefinedChallenge) : ChallengeListForCategoryAction()

    data class ChallengeBought(val challenge: PredefinedChallenge) :
        ChallengeListForCategoryAction()

    data class ChallengeTooExpensive(val challenge: PredefinedChallenge) :
        ChallengeListForCategoryAction()
}

object ChallengeListForCategoryReducer : BaseViewStateReducer<ChallengeListForCategoryViewState>() {

    override val stateKey = key<ChallengeListForCategoryViewState>()

    override fun reduce(
        state: AppState,
        subState: ChallengeListForCategoryViewState,
        action: Action
    ): ChallengeListForCategoryViewState {

        val player = state.dataState.player

        val challengeListState = subState.copy(
            playerGems = player?.gems ?: 0
        )

        return when (action) {

            is ChallengeListForCategoryAction.LoadData -> {
                challengeListState.copy(
                    type = ChallengeListForCategoryViewState.StateType.PLAYER_CHANGED,
                    challenges = PredefinedChallenge.values().filter { it.category == action.challengeCategory }.map {
                        ChallengeListForCategoryViewState.ChallengeModel(
                            it,
                            it.gemPrice,
                            player?.hasChallenge(it) ?: false
                        )
                    }
                )
            }

            is DataLoadedAction.PlayerChanged -> {
                challengeListState.copy(
                    type = ChallengeListForCategoryViewState.StateType.PLAYER_CHANGED,
                    challenges = PredefinedChallenge.values().filter { it.category == challengeListState.challengeCategory }.map {
                        ChallengeListForCategoryViewState.ChallengeModel(
                            it,
                            it.gemPrice,
                            action.player.hasChallenge(it)
                        )
                    }
                )
            }

            is ChallengeListForCategoryAction.BuyChallenge -> {
                challengeListState.copy(
                    type = ChallengeListForCategoryViewState.StateType.LOADING
                )
            }

            is ChallengeListForCategoryAction.ChallengeBought -> {
                challengeListState.copy(
                    type = ChallengeListForCategoryViewState.StateType.CHALLENGE_BOUGHT
                )
            }

            is ChallengeListForCategoryAction.ChallengeTooExpensive -> {
                challengeListState.copy(
                    type = ChallengeListForCategoryViewState.StateType.CHALLENGE_TOO_EXPENSIVE
                )
            }
            else -> challengeListState
        }
    }

    override fun defaultState(): ChallengeListForCategoryViewState {
        return ChallengeListForCategoryViewState(
            type = ChallengeListForCategoryViewState.StateType.LOADING
        )
    }
}

data class ChallengeListForCategoryViewState(
    val type: StateType,
    val challengeCategory: Challenge.Category? = null,
    val playerGems: Int = 0,
    val challenges: List<ChallengeModel> = listOf()
) : ViewState {

    enum class StateType {
        LOADING,
        PLAYER_CHANGED,
        CHALLENGE_BOUGHT,
        CHALLENGE_TOO_EXPENSIVE
    }

    data class ChallengeModel(
        val challenge: PredefinedChallenge,
        val gemPrice: Int,
        val isBought: Boolean
    )
}

fun ChallengeListForCategoryViewState.ChallengeModel.toAndroidChallenge(): ChallengeListForCategoryViewController.ChallengeViewModel {
    return AndroidPredefinedChallenge.valueOf(challenge.name).let {
        ChallengeListForCategoryViewController.ChallengeViewModel(
            it.title,
            it.description,
            it.category.color,
            it.smallImage,
            gemPrice,
            isBought,
            challenge
        )
    }

}