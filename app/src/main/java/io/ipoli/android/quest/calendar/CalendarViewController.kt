package io.ipoli.android.quest.calendar

import android.content.Context
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.di.Module
import io.ipoli.android.common.mvi.MviViewController
import io.ipoli.android.common.mvi.ViewStateRenderer
import io.ipoli.android.iPoliApp
import io.ipoli.android.quest.calendar.CalendarViewState.DatePickerState.*
import io.ipoli.android.quest.calendar.CalendarViewState.StateType.DATE_CHANGED
import io.ipoli.android.quest.calendar.dayview.view.DayViewController
import kotlinx.android.synthetic.main.controller_calendar.view.*
import kotlinx.android.synthetic.main.controller_calendar_toolbar.view.*
import org.threeten.bp.LocalDate
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import sun.bob.mcalendarview.CellConfig
import sun.bob.mcalendarview.listeners.OnDateClickListener
import sun.bob.mcalendarview.listeners.OnMonthScrollListener
import sun.bob.mcalendarview.vo.DateData

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 9/8/17.
 */
class CalendarViewController :
    MviViewController<CalendarViewState, CalendarViewController, CalendarPresenter, CalendarIntent>,
    Injects<Module>,
    ViewStateRenderer<CalendarViewState> {

    private val presenter by required { calendarPresenter }

    private lateinit var calendarToolbar: ViewGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {

        val view = inflater.inflate(R.layout.controller_calendar, container, false)

        view.pager.adapter = pagerAdapter
        view.pager.currentItem = Companion.MID_POSITION

        val toolbar = activity!!.findViewById<Toolbar>(R.id.toolbar)
        calendarToolbar = inflater.inflate(R.layout.controller_calendar_toolbar, toolbar, false) as ViewGroup
        toolbar.addView(calendarToolbar)

        initDayPicker(view, calendarToolbar)

        view.pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                send(SwipeChangeDateIntent(position))
            }
        })

        return view
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        send(LoadDataIntent(LocalDate.now()))
    }

    private var currentMidDate = LocalDate.now()

    constructor(args: Bundle? = null) : super(args)

    private val pagerAdapter = object : RouterPagerAdapter(this) {
        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val plusDays = position - Companion.MID_POSITION
                val dayViewDate = currentMidDate.plusDays(plusDays.toLong())
                val page = DayViewController(dayViewDate)
                router.setRoot(RouterTransaction.with(page))
            }
        }

        override fun getCount(): Int = Companion.MAX_VISIBLE_DAYS

        override fun getItemPosition(item: Any?): Int =
            PagerAdapter.POSITION_NONE
    }

    private fun changeCurrentDay(date: LocalDate) {
        currentMidDate = date
        pagerAdapter.notifyDataSetChanged()
        view!!.pager.setCurrentItem(Companion.MID_POSITION, false)
    }

    private fun initDayPicker(view: View, calendarToolbar: ViewGroup) {
//        val monthPattern = DateTimeFormatter.ofPattern("MMMM")
        view.dayPickerContainer.visibility = View.GONE
        val calendarIndicator = calendarToolbar.calendarIndicator

        var currentDate = LocalDate.now()
        view.dayPicker.markDate(DateData(currentDate.year, currentDate.monthValue, currentDate.dayOfMonth))

        calendarToolbar.setOnClickListener {
            calendarIndicator.animate().rotationBy(180f).duration = 200
//            view.currentMonth.text = LocalDate.now().format(monthPattern)

            send(ExpandToolbarIntent)
        }

        view.expander.setOnClickListener {
            send(ExpandToolbarWeekIntent)
        }

        view.dayPicker.setOnDateClickListener(object : OnDateClickListener() {
            override fun onDateClick(v: View, date: DateData) {

                send(ChangeDateIntent(date.year, date.month, date.day))
//                currentDate = LocalDate.of(date.year, date.month, date.day)
//                view.currentMonth.text = currentDate.format(monthPattern)
            }
        })

        view.dayPicker.setOnMonthScrollListener(object : OnMonthScrollListener() {
            override fun onMonthChange(year: Int, month: Int) {
                send(ChangeMonthIntent(year, month))
//                val localDate = LocalDate.of(year, month, 1)
//                view.currentMonth.text = localDate.format(monthPattern)
            }

            override fun onMonthScroll(positionOffset: Float) {
            }

        })
    }

    override fun createPresenter() = presenter

    override fun render(state: CalendarViewState, view: View) {
        calendarToolbar.day.text = state.dayText
        calendarToolbar.date.text = state.dateText
        view.currentMonth.text = state.monthText
        val currentDate = state.currentDate

        if (state.datePickerState == SHOW_MONTH) {
            CellConfig.ifMonth = true
            CellConfig.Week2MonthPos = CellConfig.middlePosition
            view.dayPicker.expand()
        }

        if (state.datePickerState == INVISIBLE) {
            view.dayPickerContainer.visibility = View.GONE
            val layoutParams = view.pager.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = 0
            view.pager.layoutParams = layoutParams

            CellConfig.Month2WeekPos = CellConfig.middlePosition
            CellConfig.ifMonth = false
            CellConfig.weekAnchorPointDate = DateData(currentDate.year, currentDate.monthValue, currentDate.dayOfMonth)
            view.dayPicker.shrink()
        }
        if (state.datePickerState == SHOW_WEEK) {
            val layoutParams = view.pager.layoutParams as ViewGroup.MarginLayoutParams
            CellConfig.Month2WeekPos = CellConfig.middlePosition
            CellConfig.ifMonth = false
            CellConfig.weekAnchorPointDate = DateData(currentDate.year, currentDate.monthValue, currentDate.dayOfMonth)
            view.dayPicker.shrink()
            layoutParams.topMargin = ViewUtils.dpToPx(-12f, view.context).toInt()
            view.pager.layoutParams = layoutParams
            view.dayPickerContainer.visibility = View.VISIBLE
            view.pager.layoutParams = layoutParams
        }

        if (state.type == DATE_CHANGED) {
            view.dayPicker.markedDates.removeAdd()
            view.dayPicker.markDate(DateData(currentDate.year, currentDate.monthValue, currentDate.dayOfMonth))

            view.pager.adapter.notifyDataSetChanged()
            view.pager.setCurrentItem(state.adapterPosition, false)
        }

    }

    override fun onDestroyView(view: View) {
        if (!activity!!.isChangingConfigurations) {
            view.pager.adapter = null
        }
        super.onDestroyView(view)
    }

    override fun onContextAvailable(context: Context) {
        inject(iPoliApp.module(context, router))
    }

    companion object {
        const val MID_POSITION = 49
        const val MAX_VISIBLE_DAYS = 100
    }
}