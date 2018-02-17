package mypoli.android.quest.schedule

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.design.widget.FloatingActionButton
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import kotlinx.android.synthetic.main.controller_schedule.view.*
import kotlinx.android.synthetic.main.view_calendar_toolbar.view.*
import mypoli.android.R
import mypoli.android.common.ViewUtils
import mypoli.android.common.redux.android.ReduxViewController
import mypoli.android.common.view.*
import mypoli.android.common.view.changehandler.CircularRevealChangeHandler
import mypoli.android.quest.schedule.ScheduleViewState.DatePickerState.*
import mypoli.android.quest.schedule.ScheduleViewState.StateType.*
import mypoli.android.quest.schedule.addquest.AddQuestViewController
import mypoli.android.quest.schedule.agenda.AgendaViewController
import mypoli.android.quest.schedule.calendar.CalendarViewController
import org.threeten.bp.LocalDate
import sun.bob.mcalendarview.CellConfig
import sun.bob.mcalendarview.MarkStyle
import sun.bob.mcalendarview.listeners.OnDateClickListener
import sun.bob.mcalendarview.listeners.OnMonthScrollListener
import sun.bob.mcalendarview.vo.DateData

class ScheduleViewController(args: Bundle? = null) :
    ReduxViewController<ScheduleAction, ScheduleViewState, ScheduleReducer>(args) {

    override val reducer = ScheduleReducer

    private lateinit var calendarToolbar: ViewGroup

    @DrawableRes
    private var viewModeIcon = R.drawable.ic_format_list_bulleted_white_24dp

    private var viewModeTitle = "Agenda"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.controller_schedule, container, false)

        initAddQuest(view)

        setChildController(view.contentContainer, CalendarViewController())

        return view
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        calendarToolbar = addToolbarView(R.layout.view_calendar_toolbar) as ViewGroup

        initDayPicker(view, calendarToolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.schedule_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.actionViewMode).setIcon(viewModeIcon).setTitle(viewModeTitle)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {

            R.id.actionViewMode -> {
                dispatch(ScheduleAction.ToggleViewMode)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }


    private fun initAddQuest(view: View) {
        view.addContainerBackground.setOnClickListener {
            addContainerRouter(view).popCurrentController()
            ViewUtils.hideKeyboard(view)
            closeAddContainer()
        }
    }

    private fun openAddContainer(currentDate: LocalDate) {
        val addContainer = view!!.addContainer

        val fab = view!!.addQuest

        val halfWidth = addContainer.width / 2

        val fabSet = createFabAnimator(fab, halfWidth.toFloat() - fab.width / 2)
        fabSet.start()

        fabSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                addContainer.visibility = View.VISIBLE
                fab.visibility = View.INVISIBLE

                animateShowAddContainer()

                val handler = CircularRevealChangeHandler(
                    addContainer,
                    addContainer,
                    duration = shortAnimTime
                )
                val childRouter = addContainerRouter(view!!)
                val addQuestViewController = AddQuestViewController({
                    childRouter.popCurrentController()
                    closeAddContainer()
                }, currentDate)

                childRouter.setRoot(
                    RouterTransaction.with(addQuestViewController)
                        .pushChangeHandler(handler)
                        .popChangeHandler(handler)
                )
            }
        })
    }

    private fun addContainerRouter(view: View) =
        getChildRouter(view.addContainer, "add-quest")

    private fun animateShowAddContainer() {
        val addContainerBackground = view!!.addContainerBackground
        addContainerBackground.alpha = 0f
        addContainerBackground.visibility = View.VISIBLE
        addContainerBackground.animate().alpha(1f).setDuration(longAnimTime).start()
    }

    private fun closeAddContainer() {
        view!!.addContainerBackground.visibility = View.GONE
        val duration = view!!.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        val addContainer = view!!.addContainer
        val fab = view!!.addQuest

        val revealAnim = RevealAnimator().createWithEndRadius(
            view = addContainer,
            endRadius = (fab.width / 2).toFloat(),
            reverse = true
        )
        revealAnim.duration = duration
        revealAnim.startDelay = 300
        revealAnim.interpolator = AccelerateDecelerateInterpolator()
        revealAnim.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationEnd(animation: Animator?) {
                if (view == null) {
                    return
                }
                addContainer.visibility = View.INVISIBLE
                view!!.addContainer.requestFocus()
                fab.visibility = View.VISIBLE

                val fabSet = createFabAnimator(
                    fab,
                    (addContainer.width - fab.width - ViewUtils.dpToPx(16f, fab.context)),
                    reverse = true
                )
                fabSet.start()

            }

        })
        revealAnim.start()
    }

    private fun createFabAnimator(
        fab: FloatingActionButton,
        x: Float,
        reverse: Boolean = false
    ): AnimatorSet {
        val duration = view!!.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        val fabTranslation = ObjectAnimator.ofFloat(fab, "x", x)

        val fabColor = attr(R.attr.colorAccent)
        val primaryColor = attr(R.attr.colorPrimary)

        val startColor = if (reverse) primaryColor else fabColor
        val endColor = if (reverse) fabColor else primaryColor

        val rgbAnim = ObjectAnimator.ofArgb(
            fab,
            "backgroundTint",
            startColor, endColor
        )
        rgbAnim.addUpdateListener({ animation ->
            val value = animation.animatedValue as Int
            fab.backgroundTintList = ColorStateList.valueOf(value)
        })

        return AnimatorSet().also {
            it.playTogether(fabTranslation, rgbAnim)
            it.interpolator = AccelerateDecelerateInterpolator()
            it.duration = duration
        }
    }

    private fun initDayPicker(view: View, calendarToolbar: ViewGroup) {
        view.datePickerContainer.visibility = View.GONE

        view.datePicker.setMarkedStyle(MarkStyle.BACKGROUND, attr(R.attr.colorAccent))

        val currentDate = LocalDate.now()

        val dateData = DateData(
            currentDate.year,
            currentDate.monthValue,
            currentDate.dayOfMonth
        )

        CellConfig.m2wPointDate = dateData
        CellConfig.w2mPointDate = dateData

        view.datePicker.markDate(dateData)

        calendarToolbar.dispatchOnClick(ScheduleAction.ExpandToolbar)
        view.expander.dispatchOnClick(ScheduleAction.ExpandWeekToolbar)

        view.datePicker.setOnDateClickListener(object : OnDateClickListener() {
            override fun onDateClick(v: View, date: DateData) {
                dispatch(
                    ScheduleAction.ScheduleChangeDate(
                        date.year,
                        date.month,
                        date.day
                    )
                )
            }
        })

        view.datePicker.setOnMonthScrollListener(object : OnMonthScrollListener() {
            override fun onMonthChange(year: Int, month: Int) {
                dispatch(ScheduleAction.ChangeMonth(year, month))
            }

            override fun onMonthScroll(positionOffset: Float) {
            }

        })
    }

    override fun render(state: ScheduleViewState, view: View) {

        val levelProgress = view.levelProgress

        calendarToolbar.day.text = state.dayText(activity!!)
        calendarToolbar.date.text = state.dateText(activity!!)
        view.currentMonth.text = state.monthText

        view.addQuest.setOnClickListener {
            openAddContainer(state.currentDate)
        }

        when (state.type) {

            LOADING -> levelProgress.visible = false

            XP_AND_COINS_CHANGED -> {
                levelProgress.visible = true
                val animator = ObjectAnimator.ofInt(
                    levelProgress,
                    "progress",
                    levelProgress.progress,
                    state.progress
                )
                animator.duration = shortAnimTime
                animator.start()
                calendarToolbar.playerGems.text = state.coins.toString()
            }

            LEVEL_CHANGED -> {
                levelProgress.max = state.maxProgress
                levelProgress.progress = state.progress
                calendarToolbar.playerLevel.text =
                    resources!!.getString(R.string.player_level, state.level)
            }

            DATE_PICKER_CHANGED -> renderDatePicker(
                state.datePickerState,
                view,
                state.currentDate
            )

            INITIAL -> {
                levelProgress.visible = true
                levelProgress.max = state.maxProgress
                levelProgress.progress = state.progress
                calendarToolbar.playerLevel.text =
                    resources!!.getString(R.string.player_level, state.level)
                calendarToolbar.playerGems.text = state.coins.toString()
            }

            DATA_CHANGED -> {
                levelProgress.visible = true
                levelProgress.max = state.maxProgress
                levelProgress.progress = state.progress
                calendarToolbar.playerLevel.text =
                    resources!!.getString(R.string.player_level, state.level)
                calendarToolbar.playerGems.text = state.coins.toString()
            }

            DATE_AUTO_CHANGED -> {
                val dateData = DateData(
                    state.currentDate.year,
                    state.currentDate.monthValue,
                    state.currentDate.dayOfMonth
                )
                view.datePicker.markedDates.removeAdd()
                view.datePicker.markDate(
                    dateData
                )
                view.datePicker.travelTo(dateData)

                renderDatePicker(
                    state.datePickerState,
                    view,
                    state.currentDate
                )
            }

            CALENDAR_DATE_CHANGED ->
                markSelectedDate(view, state.currentDate)

            SWIPE_DATE_CHANGED -> {
                markSelectedDate(view, state.currentDate)
            }

            VIEW_MODE_CHANGED -> {
                val handler = FadeChangeHandler()
                val childRouter = getChildRouter(view.contentContainer, null)
                val newController =
                    if (state.viewMode == ScheduleViewState.ViewMode.CALENDAR)
                        CalendarViewController()
                    else
                        AgendaViewController()
                childRouter.popCurrentController()
                childRouter.setRoot(
                    RouterTransaction.with(newController)
                        .pushChangeHandler(handler)
                        .popChangeHandler(handler)
                )

                viewModeIcon = state.viewModeIcon
                viewModeTitle = state.viewModeTitle
                activity?.invalidateOptionsMenu()
            }
        }
    }

    private fun renderDatePicker(
        datePickerState: ScheduleViewState.DatePickerState,
        view: View,
        currentDate: LocalDate
    ) {
        when (datePickerState) {
            SHOW_MONTH -> showMonthDatePicker(view)
            SHOW_WEEK -> showWeekDatePicker(view, currentDate)
            INVISIBLE -> hideDatePicker(view, currentDate)
        }
    }

    private fun showWeekDatePicker(view: View, currentDate: LocalDate) {
        calendarToolbar.calendarIndicator.animate().rotation(180f).duration = shortAnimTime
        CellConfig.Month2WeekPos = CellConfig.middlePosition
        CellConfig.ifMonth = false
        CellConfig.weekAnchorPointDate =
            DateData(currentDate.year, currentDate.monthValue, currentDate.dayOfMonth)
        view.datePicker.shrink()
        view.datePickerContainer.visibility = View.VISIBLE
        view.expander.setImageResource(R.drawable.ic_arrow_drop_down_white_24dp)
    }

    private fun showMonthDatePicker(view: View) {
        CellConfig.ifMonth = true
        CellConfig.Week2MonthPos = CellConfig.middlePosition
        view.datePicker.expand()
        view.expander.setImageResource(R.drawable.ic_arrow_drop_up_white_24dp)
    }

    private fun hideDatePicker(view: View, currentDate: LocalDate) {
        calendarToolbar.calendarIndicator.animate().rotation(0f).duration = shortAnimTime
        view.datePickerContainer.visibility = View.GONE
        CellConfig.Month2WeekPos = CellConfig.middlePosition
        CellConfig.ifMonth = false
        CellConfig.weekAnchorPointDate =
            DateData(currentDate.year, currentDate.monthValue, currentDate.dayOfMonth)
        view.datePicker.shrink()
    }

    private fun markSelectedDate(view: View, currentDate: LocalDate) {
        CellConfig.weekAnchorPointDate =
            DateData(currentDate.year, currentDate.monthValue, currentDate.dayOfMonth)
        view.datePicker.markedDates.removeAdd()

        val dateData = DateData(
            currentDate.year,
            currentDate.monthValue,
            currentDate.dayOfMonth
        )
        view.datePicker.markDate(
            dateData
        )
    }

    override fun onDestroy() {
        removeToolbarView(calendarToolbar)
        super.onDestroy()
    }

    fun onStartEdit() {
        view!!.addQuest.visible = false
    }

    fun onStopEdit() {
        view!!.addQuest.visible = true
    }
}