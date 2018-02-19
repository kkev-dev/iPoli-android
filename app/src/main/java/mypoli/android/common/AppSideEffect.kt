package mypoli.android.common

import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import mypoli.android.challenge.category.list.ChallengeListForCategoryAction
import mypoli.android.challenge.usecase.BuyChallengeUseCase
import mypoli.android.common.DataLoadedAction.PlayerChanged
import mypoli.android.common.di.Module
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.Dispatcher
import mypoli.android.common.redux.SideEffect
import mypoli.android.common.view.AppWidgetUtil
import mypoli.android.myPoliApp
import mypoli.android.pet.store.PetStoreAction
import mypoli.android.pet.usecase.BuyPetUseCase
import mypoli.android.player.Player
import mypoli.android.quest.Quest
import mypoli.android.quest.schedule.ScheduleAction
import mypoli.android.quest.schedule.ScheduleViewState
import mypoli.android.quest.schedule.agenda.AgendaAction
import mypoli.android.quest.schedule.agenda.AgendaReducer
import mypoli.android.quest.schedule.agenda.AgendaViewState
import mypoli.android.quest.schedule.agenda.usecase.CreateAgendaItemsUseCase
import mypoli.android.quest.schedule.agenda.usecase.FindAgendaDatesUseCase
import mypoli.android.quest.schedule.calendar.CalendarAction
import mypoli.android.quest.schedule.calendar.CalendarViewState
import mypoli.android.quest.usecase.CompleteQuestUseCase.Params.WithQuest
import org.threeten.bp.LocalDate
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/27/2018.
 */

abstract class AppSideEffect : SideEffect<AppState>,
    Injects<Module> {

    private var dispatcher: Dispatcher? = null

    override suspend fun execute(action: Action, state: AppState, dispatcher: Dispatcher) {
        inject(myPoliApp.module(myPoliApp.instance))
        this.dispatcher = dispatcher
        doExecute(action, state)
    }

    abstract suspend fun doExecute(
        action: Action,
        state: AppState
    )

    fun dispatch(action: Action) {
        dispatcher!!.dispatch(action)
    }
}

class BuyPredefinedChallengeSideEffect : AppSideEffect() {

    override suspend fun doExecute(action: Action, state: AppState) {
        val challenge = (action as ChallengeListForCategoryAction.BuyChallenge).challenge
        val result = buyChallengeUseCase.execute(BuyChallengeUseCase.Params(challenge))
        when (result) {
            is BuyChallengeUseCase.Result.ChallengeBought -> {
                dispatch(ChallengeListForCategoryAction.ChallengeBought(challenge))
            }

            BuyChallengeUseCase.Result.TooExpensive -> {
                dispatch(
                    ChallengeListForCategoryAction.ChallengeTooExpensive(
                        challenge
                    )
                )
            }
        }
    }

    private val buyChallengeUseCase by required { buyChallengeUseCase }

    override fun canHandle(action: Action) = action is ChallengeListForCategoryAction.BuyChallenge
}

class ChangePetSideEffect : AppSideEffect() {

    private val changePetUseCase by required { changePetUseCase }

    override suspend fun doExecute(action: Action, state: AppState) {
        changePetUseCase.execute((action as PetStoreAction.ChangePet).pet)
    }

    override fun canHandle(action: Action) = action is PetStoreAction.ChangePet
}

class BuyPetSideEffect : AppSideEffect() {

    private val buyPetUseCase by required { buyPetUseCase }

    override suspend fun doExecute(action: Action, state: AppState) {
        val result = buyPetUseCase.execute((action as PetStoreAction.BuyPet).pet)
        when (result) {
            is BuyPetUseCase.Result.PetBought -> {
                dispatch(PetStoreAction.PetBought)
            }
            BuyPetUseCase.Result.TooExpensive -> {
                dispatch(PetStoreAction.PetTooExpensive)
            }
        }
    }

    override fun canHandle(action: Action) = action is PetStoreAction.BuyPet
}

class AgendaSideEffect : AppSideEffect() {

    private val completeQuestUseCase by required { completeQuestUseCase }
    private val undoCompletedQuestUseCase by required { undoCompletedQuestUseCase }
    private val findAgendaDatesUseCase by required { findAgendaDatesUseCase }
    private val createAgendaItemsUseCase by required { createAgendaItemsUseCase }
    private val questRepository by required { questRepository }

    private var scheduledQuestsChannel: ReceiveChannel<List<Quest>>? = null

    override suspend fun doExecute(action: Action, state: AppState) {

        when (action) {
            is AgendaAction.LoadBefore -> {
                val agendaItems = state.stateFor(AgendaViewState::class.java).agendaItems
                val position = action.itemPosition
                val agendaItem = agendaItems[position]
                val agendaDate = agendaItem.startDate()
                val result = findAgendaDatesUseCase.execute(
                    FindAgendaDatesUseCase.Params.Before(
                        agendaDate,
                        AgendaReducer.ITEMS_BEFORE_COUNT
                    )
                )
                var start = agendaDate.minusMonths(3)
                (result as FindAgendaDatesUseCase.Result.Before).date?.let {
                    start = it
                }
                val end =
                    agendaItems[position + AgendaReducer.ITEMS_AFTER_COUNT - 1].startDate()
                listenForAgendaItems(start, end, agendaDate, false)
            }
            is AgendaAction.LoadAfter -> {
                val agendaItems = state.stateFor(AgendaViewState::class.java).agendaItems
                val position = action.itemPosition
                val agendaItem = agendaItems[position]
                val agendaDate = agendaItem.startDate()
                val result = findAgendaDatesUseCase.execute(
                    FindAgendaDatesUseCase.Params.After(agendaDate, AgendaReducer.ITEMS_AFTER_COUNT)
                )
                val start = agendaItems[position - AgendaReducer.ITEMS_BEFORE_COUNT].startDate()
                var end = agendaDate.plusMonths(3)
                (result as FindAgendaDatesUseCase.Result.After).date?.let {
                    end = it
                }

                listenForAgendaItems(start, end, agendaDate, false)
            }

            is AgendaAction.CompleteQuest -> {
                val adapterPos = action.itemPosition
                val agendaState = state.stateFor(AgendaViewState::class.java)
                val questItem =
                    agendaState.agendaItems[adapterPos] as CreateAgendaItemsUseCase.AgendaItem.QuestItem
                completeQuestUseCase.execute(WithQuest(questItem.quest))
            }

            is AgendaAction.UndoCompleteQuest -> {
                val agendaItems = state.stateFor(AgendaViewState::class.java).agendaItems
                val adapterPos = action.itemPosition
                val questItem =
                    agendaItems[adapterPos] as CreateAgendaItemsUseCase.AgendaItem.QuestItem
                undoCompletedQuestUseCase.execute(questItem.quest.id)
            }

            is LoadDataAction.All -> {
                val agendaDate = state.dataState.today
                val pair = findAllAgendaDates(agendaDate)
                val start = pair.first
                val end = pair.second

                listenForAgendaItems(start, end, agendaDate, true)
            }
            is ScheduleAction.ScheduleChangeDate -> {
                val agendaDate = LocalDate.of(action.year, action.month, action.day)
                val pair = findAllAgendaDates(agendaDate)
                val start = pair.first
                val end = pair.second
                listenForAgendaItems(start, end, agendaDate, true)
            }
            is CalendarAction.SwipeChangeDate -> {
                val calendarState = state.stateFor(CalendarViewState::class.java)
                val currentPos = calendarState.adapterPosition
                val newPos = action.adapterPosition
                val scheduleState = state.stateFor(ScheduleViewState::class.java)
                val curDate = scheduleState.currentDate
                val agendaDate = if (newPos < currentPos)
                    curDate.minusDays(1)
                else
                    curDate.plusDays(1)
                val pair = findAllAgendaDates(agendaDate)
                val start = pair.first
                val end = pair.second
                listenForAgendaItems(start, end, agendaDate, true)
            }
        }
    }

    private fun listenForAgendaItems(
        start: LocalDate,
        end: LocalDate,
        agendaDate: LocalDate,
        changeCurrentAgendaItem: Boolean
    ) {

        var isFirstData = true
        launch(UI) {
            scheduledQuestsChannel?.cancel()
            scheduledQuestsChannel = questRepository.listenForScheduledBetween(
                start,
                end
            )
            scheduledQuestsChannel!!.consumeEach {
                val agendaItems = createAgendaItemsUseCase.execute(
                    CreateAgendaItemsUseCase.Params(
                        agendaDate,
                        it,
                        AgendaReducer.ITEMS_BEFORE_COUNT,
                        AgendaReducer.ITEMS_AFTER_COUNT
                    )
                )
                dispatch(
                    DataLoadedAction.AgendaItemsChanged(
                        start = start,
                        end = end,
                        agendaItems = agendaItems,
                        currentAgendaItemDate = if (changeCurrentAgendaItem && isFirstData) agendaDate else null
                    )
                )
                isFirstData = false
            }
        }
    }

    private fun findAllAgendaDates(
        agendaDate: LocalDate
    ): Pair<LocalDate, LocalDate> {
        val result = findAgendaDatesUseCase.execute(
            FindAgendaDatesUseCase.Params.All(
                agendaDate,
                AgendaReducer.ITEMS_BEFORE_COUNT,
                AgendaReducer.ITEMS_AFTER_COUNT
            )
        ) as FindAgendaDatesUseCase.Result.All
        val start = result.start ?: agendaDate.minusMonths(3)
        val end = result.end ?: agendaDate.plusMonths(3)
        return Pair(start, end)
    }

    override fun canHandle(action: Action) =
        action == LoadDataAction.All
            || action is AgendaAction.LoadBefore
            || action is AgendaAction.LoadAfter
            || action is ScheduleAction.ScheduleChangeDate
            || action is CalendarAction.SwipeChangeDate

}

class LoadAllDataSideEffect : AppSideEffect() {

    private val playerRepository by required { playerRepository }
    private val questRepository by required { questRepository }

    private var playerChannel: ReceiveChannel<Player?>? = null
    private var scheduledQuestsChannel: ReceiveChannel<List<Quest>>? = null

    override suspend fun doExecute(action: Action, state: AppState) {

        if (action is LoadDataAction.ChangePlayer) {
            playerChannel?.cancel()
            scheduledQuestsChannel?.cancel()
            playerChannel = null
            scheduledQuestsChannel = null
            playerRepository.purge(action.oldPlayerId)
            listenForPlayer()
            listenForQuests(state)
        }

        if (action == LoadDataAction.All) {
            listenForPlayer()
            listenForQuests(state)
        }
    }

    private fun listenForQuests(
        state: AppState
    ) {
        launch(UI) {
            scheduledQuestsChannel?.cancel()
            scheduledQuestsChannel =
                questRepository.listenForScheduledAt(state.dataState.today)
            scheduledQuestsChannel!!.consumeEach {

                questRepository.listenForScheduledAt(state.dataState.today).consumeEach {

                    updateWidgets()
                    dispatch(DataLoadedAction.TodayQuestsChanged(it))
                }
            }
        }
    }

    private fun updateWidgets() {
        AppWidgetUtil.updateAgendaWidget(myPoliApp.instance)
    }

    private fun listenForPlayer() {
        launch(UI) {
            playerChannel?.cancel()
            playerChannel = playerRepository.listen()
            playerChannel!!.consumeEach {
                dispatch(PlayerChanged(it!!))
            }
        }
    }

    override fun canHandle(action: Action) =
        action == LoadDataAction.All
            || action is LoadDataAction.ChangePlayer
}