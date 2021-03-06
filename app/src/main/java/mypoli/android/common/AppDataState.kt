package mypoli.android.common

import mypoli.android.common.redux.Action
import mypoli.android.common.redux.State
import mypoli.android.player.Player
import mypoli.android.quest.Quest
import mypoli.android.quest.schedule.agenda.usecase.CreateAgendaItemsUseCase
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/24/2018.
 */

sealed class DataLoadedAction : Action {
    data class PlayerChanged(val player: Player) : DataLoadedAction()
    data class TodayQuestsChanged(val quests: List<Quest>) : DataLoadedAction()
    data class AgendaItemsChanged(
        val start: LocalDate,
        val end: LocalDate,
        val agendaItems: List<CreateAgendaItemsUseCase.AgendaItem>,
        val currentAgendaItemDate: LocalDate?
    ) : DataLoadedAction()
}

data class AppDataState(
    val today: LocalDate,
    val player: Player?,
    val todayQuests: List<Quest>
) : State

object AppDataReducer : AppStateReducer<AppDataState> {

    override fun reduce(state: AppState, action: Action) =
        state.appDataState.let {
            when (action) {

                is DataLoadedAction.PlayerChanged -> {
                    it.copy(
                        player = action.player
                    )
                }

                is DataLoadedAction.TodayQuestsChanged -> {
                    it.copy(
                        todayQuests = action.quests
                    )
                }
                else -> it
            }

        }

    override fun defaultState(): AppDataState {
        return AppDataState(
            today = LocalDate.now(),
            player = null,
            todayQuests = listOf()
        )
    }

}