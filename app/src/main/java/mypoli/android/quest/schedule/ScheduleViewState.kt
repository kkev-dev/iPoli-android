package mypoli.android.quest.schedule

import android.content.Context
import mypoli.android.R
import mypoli.android.common.AppState
import mypoli.android.common.BaseViewStateReducer
import mypoli.android.common.DataLoadedAction
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.common.text.CalendarFormatter
import mypoli.android.quest.schedule.agenda.AgendaAction
import mypoli.android.quest.schedule.agenda.AgendaViewState
import mypoli.android.quest.schedule.calendar.CalendarAction
import mypoli.android.quest.schedule.calendar.CalendarViewState
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 10/21/17.
 */

sealed class ScheduleAction : Action {
    object ExpandToolbar : ScheduleAction()
    object ExpandWeekToolbar : ScheduleAction()

    data class ScheduleChangeDate(val year: Int, val month: Int, val day: Int) : ScheduleAction()

    data class ChangeMonth(val year: Int, val month: Int) : ScheduleAction()
    object ToggleViewMode : ScheduleAction()
}

object ScheduleReducer : BaseViewStateReducer<ScheduleViewState>() {

    override val stateKey = key<ScheduleViewState>()

    override fun defaultState() =
        ScheduleViewState(
            ScheduleViewState.StateType.LOADING,
            YearMonth.now(),
            LocalDate.now(),
            viewMode = ScheduleViewState.ViewMode.CALENDAR,
            datePickerState = ScheduleViewState.DatePickerState.INVISIBLE
        )

    override fun reduce(state: AppState, subState: ScheduleViewState, action: Action) =
        when (action) {
            is DataLoadedAction.PlayerChanged -> reducePlayerChanged(
                subState,
                action
            )
            is ScheduleAction -> reduceCalendarAction(
                subState,
                action
            )
            is CalendarAction.SwipeChangeDate -> {
                val currentPos = state.stateFor(CalendarViewState::class.java).adapterPosition
                val newPos = action.adapterPosition
                val curDate = subState.currentDate
                val newDate = if (newPos < currentPos)
                    curDate.minusDays(1)
                else
                    curDate.plusDays(1)

                subState.copy(
                    type = ScheduleViewState.StateType.SWIPE_DATE_CHANGED,
                    currentDate = newDate
                )
            }
            is AgendaAction.FirstVisibleItemChanged -> {

                val itemPos = action.itemPosition
                val startDate =
                    state.stateFor(AgendaViewState::class.java).agendaItems[itemPos].startDate()

                if (subState.currentDate.isEqual(startDate)) {
                    subState.copy(
                        type = ScheduleViewState.StateType.IDLE
                    )
                } else {
                    subState.copy(
                        type = ScheduleViewState.StateType.DATE_AUTO_CHANGED,
                        currentDate = startDate,
                        currentMonth = YearMonth.of(startDate.year, startDate.month)
                    )
                }

            }
            else -> subState
        }

    private fun reducePlayerChanged(
        state: ScheduleViewState,
        action: DataLoadedAction.PlayerChanged
    ): ScheduleViewState {
        val player = action.player
        val type = when {
            state.level == 0 -> ScheduleViewState.StateType.DATA_CHANGED
            state.level != player.level -> ScheduleViewState.StateType.LEVEL_CHANGED
            else -> ScheduleViewState.StateType.XP_AND_COINS_CHANGED
        }
        return state.copy(
            type = type,
            level = player.level,
            progress = player.experienceProgressForLevel,
            coins = player.coins,
            maxProgress = player.experienceForNextLevel
        )
    }

    private fun reduceCalendarAction(state: ScheduleViewState, action: ScheduleAction) =
        when (action) {
            ScheduleAction.ExpandWeekToolbar -> {
                when (state.datePickerState) {
                    ScheduleViewState.DatePickerState.SHOW_WEEK -> state.copy(
                        type = ScheduleViewState.StateType.DATE_PICKER_CHANGED,
                        datePickerState = ScheduleViewState.DatePickerState.SHOW_MONTH
                    )
                    else -> state.copy(
                        type = ScheduleViewState.StateType.DATE_PICKER_CHANGED,
                        datePickerState = ScheduleViewState.DatePickerState.SHOW_WEEK
                    )
                }
            }

            ScheduleAction.ExpandToolbar -> {
                when (state.datePickerState) {
                    ScheduleViewState.DatePickerState.INVISIBLE -> state.copy(
                        type = ScheduleViewState.StateType.DATE_PICKER_CHANGED,
                        datePickerState = ScheduleViewState.DatePickerState.SHOW_WEEK
                    )
                    else -> state.copy(
                        type = ScheduleViewState.StateType.DATE_PICKER_CHANGED,
                        datePickerState = ScheduleViewState.DatePickerState.INVISIBLE
                    )
                }
            }

            is ScheduleAction.ScheduleChangeDate -> {
                state.copy(
                    type = ScheduleViewState.StateType.CALENDAR_DATE_CHANGED,
                    currentDate = LocalDate.of(action.year, action.month, action.day),
                    currentMonth = YearMonth.of(action.year, action.month)
                )
            }

            is ScheduleAction.ChangeMonth -> {
                state.copy(
                    type = ScheduleViewState.StateType.MONTH_CHANGED,
                    currentMonth = YearMonth.of(action.year, action.month)
                )
            }

            is ScheduleAction.ToggleViewMode -> {
                state.copy(
                    type = ScheduleViewState.StateType.VIEW_MODE_CHANGED,
                    viewMode = if (state.viewMode == ScheduleViewState.ViewMode.CALENDAR) ScheduleViewState.ViewMode.AGENDA else ScheduleViewState.ViewMode.CALENDAR
                )
            }
        }
}

data class ScheduleViewState(
    val type: StateType,
    val currentMonth: YearMonth,
    val currentDate: LocalDate,
    val datePickerState: DatePickerState,
    val progress: Int = 0,
    val maxProgress: Int = 0,
    val level: Int = 0,
    val coins: Int = 0,
    val viewMode: ViewMode
) : ViewState {

    enum class StateType {
        LOADING, IDLE,
        INITIAL, CALENDAR_DATE_CHANGED, SWIPE_DATE_CHANGED, DATE_PICKER_CHANGED, MONTH_CHANGED,
        LEVEL_CHANGED, XP_AND_COINS_CHANGED, DATA_CHANGED,
        VIEW_MODE_CHANGED, DATE_AUTO_CHANGED
    }

    enum class ViewMode {
        CALENDAR, AGENDA
    }

    enum class DatePickerState {
        INVISIBLE, SHOW_WEEK, SHOW_MONTH
    }
}

val ScheduleViewState.viewModeIcon
    get() = if (viewMode == ScheduleViewState.ViewMode.CALENDAR)
        R.drawable.ic_format_list_bulleted_white_24dp
    else
        R.drawable.ic_event_white_24dp

val ScheduleViewState.viewModeTitle
    get() = if (viewMode == ScheduleViewState.ViewMode.CALENDAR) "Agenda" else "Calendar"

fun ScheduleViewState.dayText(context: Context) =
    CalendarFormatter(context).day(currentDate)

fun ScheduleViewState.dateText(context: Context) =
    CalendarFormatter(context).date(currentDate)

val ScheduleViewState.monthText: String
    get() = DateTimeFormatter.ofPattern("MMMM").format(currentMonth)
