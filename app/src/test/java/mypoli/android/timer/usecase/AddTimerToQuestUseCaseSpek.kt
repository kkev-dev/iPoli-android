package mypoli.android.timer.usecase

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import mypoli.android.Constants
import mypoli.android.common.datetime.Time
import mypoli.android.quest.*
import mypoli.android.quest.data.persistence.QuestRepository
import mypoli.android.timer.pomodoros
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 1/22/18.
 */
class AddTimerToQuestUseCaseSpek : Spek({

    describe("AddTimerToQuestUseCase") {

        fun executeUseCase(
            quest: Quest,
            isPomodoro: Boolean,
            time: Instant = Instant.now(),
            startedQuests: List<Quest> = listOf()
        ): AddTimerToQuestUseCase.Result {
            val questRepoMock = mock<QuestRepository> {
                on { findById(any()) } doReturn quest
                on { save(any()) } doAnswer { invocation ->
                    invocation.getArgument(0)
                }
                on { findStartedQuests() } doReturn startedQuests
            }
            return AddTimerToQuestUseCase(questRepoMock, mock(), mock())
                .execute(AddTimerToQuestUseCase.Params(quest.id, isPomodoro, time))
        }

        val quest = Quest(
            name = "",
            color = Color.BLUE,
            category = Category("Wellness", Color.BLUE),
            scheduledDate = LocalDate.now(),
            duration = 30,
            reminder = Reminder("", Time.now(), LocalDate.now())
        )

        val now = Instant.now()

        it("should save countdown timer") {
            val result = executeUseCase(quest, false, now)
            result.otherTimerStopped.`should be false`()
            val timeRanges = result.quest.timeRanges
            timeRanges.size.`should be equal to`(1)
            timeRanges.first().type.`should be`(TimeRange.Type.COUNTDOWN)
            timeRanges.first().start.`should equal`(now)
        }

        it("should add the first time range") {
            val result = executeUseCase(quest, true)
            result.otherTimerStopped.`should be false`()
            result.quest.timeRanges.size `should be equal to` (1)
            val range = result.quest.timeRanges.first()
            range.start.`should not be null`()
            range.end.`should be null`()
        }

        it("should require not starter countdown timer") {
            val exec = {
                executeUseCase(
                    quest.copy(
                        timeRanges = listOf(
                            TimeRange(
                                TimeRange.Type.COUNTDOWN,
                                quest.duration,
                                now
                            )
                        )
                    ),
                    false,
                    now
                )
            }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should require not starter pomodoro timer") {
            val exec = {
                executeUseCase(
                    quest.copy(
                        timeRanges = listOf(
                            TimeRange(
                                TimeRange.Type.POMODORO_WORK,
                                Constants.DEFAULT_POMODORO_WORK_DURATION,
                                now
                            )
                        )
                    ),
                    true,
                    now
                )
            }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should stop other countdown timer") {
            val result = executeUseCase(
                quest = quest,
                isPomodoro = false,
                time = now,
                startedQuests = listOf(
                    quest.copy(
                        timeRanges = listOf(
                            TimeRange(
                                TimeRange.Type.COUNTDOWN,
                                quest.duration,
                                now
                            )
                        )
                    )
                )
            )
            result.otherTimerStopped.`should be true`()
            result.quest.hasCountDownTimer.`should be true`()
        }

        it("should stop other pomodoro timer") {
            val result = executeUseCase(
                quest = quest,
                isPomodoro = true,
                time = now,
                startedQuests = listOf(
                    quest.copy(
                        timeRanges = listOf(
                            TimeRange(
                                TimeRange.Type.POMODORO_WORK,
                                1.pomodoros(),
                                now
                            )
                        )
                    )
                )
            )
            result.otherTimerStopped.`should be true`()
            result.quest.hasPomodoroTimer.`should be true`()
        }
    }
})