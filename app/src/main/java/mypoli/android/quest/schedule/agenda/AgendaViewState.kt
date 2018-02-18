package mypoli.android.quest.schedule.agenda

import com.mikepenz.ionicons_typeface_library.Ionicons
import mypoli.android.R
import mypoli.android.common.AppState
import mypoli.android.common.BaseViewStateReducer
import mypoli.android.common.DataLoadedAction
import mypoli.android.common.datetime.isBetween
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.State
import mypoli.android.common.text.DateFormatter
import mypoli.android.common.view.AndroidColor
import mypoli.android.common.view.AndroidIcon
import mypoli.android.quest.Quest
import mypoli.android.quest.schedule.agenda.usecase.CreateAgendaItemsUseCase
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import java.util.*

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/26/2018.
 */

sealed class AgendaAction : Action {
    data class LoadBefore(val itemPosition: Int) : AgendaAction()
    data class LoadAfter(val itemPosition: Int) : AgendaAction()
    data class CompleteQuest(val itemPosition: Int) : AgendaAction()
    data class UndoCompleteQuest(val itemPosition: Int) : AgendaAction()
    data class FirstVisibleItemChanged(val itemPosition: Int) : AgendaAction()
}

data class AgendaState(
    val type: StateType,
    val agendaItems: List<CreateAgendaItemsUseCase.AgendaItem>,
    val scrollToPosition: Int?,
    val userScrollPosition: Int?
) : State {
    enum class StateType {
        LOADING,
        DATA_CHANGED,
        SHOW_TOP_LOADER,
        SHOW_BOTTOM_LOADER,
        IDLE
    }
}

object AgendaReducer : BaseViewStateReducer<AgendaViewState>() {

    override val stateKey = key<AgendaViewState>()


    override fun reduce(
        state: AppState,
        subState: AgendaViewState,
        action: Action
    ): AgendaViewState {
        return when (action) {
            is DataLoadedAction.AgendaItemsChanged -> {
                val userScrolledToPosition =
                    if (subState.userScrollPosition != null) {
                        val userDate =
                            subState.agendaItems[subState.userScrollPosition].startDate()
                        findItemPositionToScrollTo(userDate, action.agendaItems)
                    } else null

                subState.copy(
                    type = AgendaState.StateType.DATA_CHANGED,
                    agendaItems = action.agendaItems,
                    scrollToPosition = findItemPositionToScrollTo(
                        action.currentAgendaItemDate,
                        action.agendaItems
                    ),
                    userScrollPosition = userScrolledToPosition,
                    shouldScrollToUserPosition = false
                )
            }
            is AgendaAction.LoadBefore -> {
                subState.copy(
                    type = AgendaState.StateType.SHOW_TOP_LOADER,
                    userScrollPosition = action.itemPosition
                )
            }
            is AgendaAction.LoadAfter -> {
                subState.copy(
                    type = AgendaState.StateType.SHOW_BOTTOM_LOADER,
                    userScrollPosition = action.itemPosition
                )
            }
            is AgendaAction.FirstVisibleItemChanged -> {
                subState.copy(
                    type = AgendaState.StateType.IDLE,
                    userScrollPosition = action.itemPosition
                )
            }
            else -> subState

        }
    }

    private fun findItemPositionToScrollTo(
        date: LocalDate?,
        agendaItems: List<CreateAgendaItemsUseCase.AgendaItem>
    ) = date?.let {
        val currentAgendaItemDate = it
        val index = agendaItems.indexOfLast {
            when (it) {
                is CreateAgendaItemsUseCase.AgendaItem.Date ->
                    it.startDate() == currentAgendaItemDate
                is CreateAgendaItemsUseCase.AgendaItem.Week ->
                    currentAgendaItemDate.isBetween(
                        it.start,
                        it.end
                    )
                else -> false
            }
        }
        if (index < 0) null
        else index
    }

    override fun defaultState() = AgendaViewState(
        type = AgendaState.StateType.LOADING,
        agendaItems = listOf(),
        scrollToPosition = null,
        userScrollPosition = null,
        shouldScrollToUserPosition = true
    )

    const val ITEMS_BEFORE_COUNT = 25
    const val ITEMS_AFTER_COUNT = 35
}

data class AgendaViewState(
    val type: AgendaState.StateType,
    val userScrollPosition: Int?,
    val scrollToPosition: Int?,
    val shouldScrollToUserPosition: Boolean,
    val agendaItems: List<CreateAgendaItemsUseCase.AgendaItem>
) : ViewState {

    enum class StateType {
        LOADING,
        DATA_CHANGED,
        SHOW_TOP_LOADER,
        SHOW_BOTTOM_LOADER,
        IDLE
    }

}

fun AgendaViewState.toAgendaItemViewModels() =
    agendaItems.mapIndexed { index, item ->
        toAgendaViewModel(
            item,
            if (agendaItems.lastIndex >= index + 1) agendaItems[index + 1] else null
        )
    }

private fun toAgendaViewModel(
    agendaItem: CreateAgendaItemsUseCase.AgendaItem,
    nextAgendaItem: CreateAgendaItemsUseCase.AgendaItem? = null
): AgendaViewController.AgendaViewModel {

    return when (agendaItem) {
        is CreateAgendaItemsUseCase.AgendaItem.QuestItem -> {
            val quest = agendaItem.quest
            val showDivider =
                !(nextAgendaItem == null || nextAgendaItem !is CreateAgendaItemsUseCase.AgendaItem.QuestItem)
            val color = if (quest.isCompleted)
                R.color.md_grey_500
            else
                AndroidColor.valueOf(quest.color.name).color500

            AgendaViewController.QuestViewModel(
                id = quest.id,
                name = quest.name,
                startTime = formatStartTime(quest),
                color = color,
                icon = quest.icon?.let { AndroidIcon.valueOf(it.name).icon }
                    ?: Ionicons.Icon.ion_android_clipboard,
                isCompleted = quest.isCompleted,
                showDivider = showDivider
            )
        }
        is CreateAgendaItemsUseCase.AgendaItem.Date -> {
            val date = agendaItem.date
            val dayOfMonth = date.dayOfMonth
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .toUpperCase()
            AgendaViewController.DateHeaderViewModel("$dayOfMonth $dayOfWeek")
        }
        is CreateAgendaItemsUseCase.AgendaItem.Week -> {
            val start = agendaItem.start
            val end = agendaItem.end
            val label = if (start.month != end.month) {
                "${DateFormatter.formatDayWithWeek(start)} - ${DateFormatter.formatDayWithWeek(
                    end
                )}"
            } else {
                "${start.dayOfMonth} - ${DateFormatter.formatDayWithWeek(end)}"
            }

            AgendaViewController.WeekHeaderViewModel(label)
        }
        is CreateAgendaItemsUseCase.AgendaItem.Month -> {
            AgendaViewController.MonthDividerViewModel(
                monthToImage[agendaItem.month.month]!!,
                agendaItem.month.format(
                    DateTimeFormatter.ofPattern("MMMM yyyy")
                )
            )
        }
    }

}

private fun formatStartTime(quest: Quest): String {
    val start = quest.startTime ?: return "Unscheduled"
    val end = start.plus(quest.actualDuration.asMinutes.intValue)
    return "$start - $end"
}

private val monthToImage = mapOf<Month, Int>(
    Month.JANUARY to R.drawable.agenda_january,
    Month.FEBRUARY to R.drawable.agenda_february,
    Month.MARCH to R.drawable.agenda_march,
    Month.APRIL to R.drawable.agenda_april,
    Month.MAY to R.drawable.agenda_may,
    Month.JUNE to R.drawable.agenda_june,
    Month.JULY to R.drawable.agenda_july,
    Month.AUGUST to R.drawable.agenda_august,
    Month.SEPTEMBER to R.drawable.agenda_september,
    Month.OCTOBER to R.drawable.agenda_october,
    Month.NOVEMBER to R.drawable.agenda_november,
    Month.DECEMBER to R.drawable.agenda_december

)