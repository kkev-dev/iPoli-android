package mypoli.android.repeatingquest.usecase

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import mypoli.android.TestUtil
import mypoli.android.TestUtil.firstDateOfWeek
import mypoli.android.TestUtil.lastDateOfWeek
import mypoli.android.quest.Quest
import mypoli.android.quest.data.persistence.QuestRepository
import mypoli.android.repeatingquest.entity.RepeatingPattern
import mypoli.android.repeatingquest.entity.RepeatingQuest
import mypoli.android.repeatingquest.persistence.RepeatingQuestRepository
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.xit
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.temporal.TemporalAdjusters

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 02/14/2018.
 */
class FindQuestsForRepeatingQuestSpek : Spek({

    describe("FindQuestsForRepeatingQuest") {

        fun doExecuteUseCase(
            quest: RepeatingQuest,
            start: LocalDate,
            end: LocalDate,
            questRepo: QuestRepository = TestUtil.questRepoMock(),
            rqRepo: RepeatingQuestRepository = TestUtil.repeatingQuestRepoMock()
        ) =
            FindQuestsForRepeatingQuest(questRepo, rqRepo).execute(
                FindQuestsForRepeatingQuest.Params(
                    repeatingQuest = quest,
                    start = start,
                    end = end,
                    firstDayOfWeek = DayOfWeek.MONDAY,
                    lastDayOfWeek = DayOfWeek.SUNDAY
                )
            )


        fun executeUseCaseWithRepeatingQuestResult(
            quest: RepeatingQuest,
            start: LocalDate,
            end: LocalDate,
            questRepo: QuestRepository = TestUtil.questRepoMock(),
            rqRepo: RepeatingQuestRepository = TestUtil.repeatingQuestRepoMock()
        ) = doExecuteUseCase(quest, start, end, questRepo, rqRepo).repeatingQuest

        fun executeUseCase(
            quest: RepeatingQuest,
            start: LocalDate,
            end: LocalDate,
            questRepo: QuestRepository = TestUtil.questRepoMock(),
            rqRepo: RepeatingQuestRepository = TestUtil.repeatingQuestRepoMock()
        ) = doExecuteUseCase(quest, start, end, questRepo, rqRepo).quests


        fun mockQuestsForRepeatingQuest(quests: List<Quest> = listOf()) =
            mock<QuestRepository> {
                on {
                    findForRepeatingQuestBetween(
                        any(),
                        any(),
                        any()
                    )
                } doReturn quests
            }

        val questId = "qid"

        it("should require end after start") {
            val exec = {
                val today = LocalDate.now()
                executeUseCase(TestUtil.repeatingQuest, today, today)
            }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should not schedule anything before quest start") {
            val quests = executeUseCase(
                quest = TestUtil.repeatingQuest.copy(start = lastDateOfWeek.plusDays(1)),
                start = firstDateOfWeek,
                end = lastDateOfWeek
            )

            quests.`should be empty`()
        }

        it("should not schedule anything after quest end") {
            val quests = executeUseCase(
                quest = TestUtil.repeatingQuest.copy(
                    start = firstDateOfWeek.minusDays(2),
                    end = firstDateOfWeek.minusDays(1)
                ),
                start = firstDateOfWeek,
                end = lastDateOfWeek
            )

            quests.`should be empty`()
        }

        describe("repeating daily") {

            it("should schedule for every day at start of week when no quests are scheduled") {

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    end = lastDateOfWeek
                )
                quests.size.`should be`(7)
            }

            it("should schedule for every day at start of week when 1 quest is scheduled") {

                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(7)
                quests.first().id.`should be`(questId)
                quests.filter { it.id.isEmpty() }.size.`should be`(6)
            }

            it("should find only stored quests") {


                val repo = mockQuestsForRepeatingQuest(
                    List(7) { i ->
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek.plusDays(i.toLong())
                        )
                    }
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(7)
                quests.forEach { it.id.`should be`(questId) }
            }

            it("should not schedule for dates with deleted quest") {

                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek,
                            isRemoved = true
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(6)
                quests.filter { it.scheduledDate.isEqual(firstDateOfWeek) }
                    .`should be empty`()
            }

            it("should not schedule for dates after repeating quest end") {

                val repo = mockQuestsForRepeatingQuest(listOf())

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        start = firstDateOfWeek,
                        end = firstDateOfWeek.plusDays(3)
                    ),
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(4)
            }

            it("should not schedule for dates before repeating quest start") {

                val repo = mockQuestsForRepeatingQuest(listOf())

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        start = firstDateOfWeek.plusDays(1),
                        end = lastDateOfWeek
                    ),
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(6)
            }

        }

        describe("repeating weekly") {

            describe("fixed") {

                it("should schedule for every week day in pattern") {

                    val repo = mockQuestsForRepeatingQuest(listOf())

                    val quests = executeUseCase(
                        quest = TestUtil.repeatingQuest.copy(
                            repeatingPattern = RepeatingPattern.Weekly(
                                setOf(
                                    DayOfWeek.MONDAY,
                                    DayOfWeek.WEDNESDAY,
                                    DayOfWeek.FRIDAY
                                )
                            )
                        ),
                        start = firstDateOfWeek,
                        end = lastDateOfWeek.plusDays(3),
                        questRepo = repo
                    )
                    quests.size.`should be`(5)
                    quests.filter { it.id.isEmpty() }.size.`should be`(5)
                }

                it("should not schedule for moved stored quest") {

                    val repo = mockQuestsForRepeatingQuest(
                        listOf(
                            TestUtil.quest.copy(
                                id = questId,
                                originalScheduledDate = firstDateOfWeek,
                                scheduledDate = firstDateOfWeek.plusDays(1)
                            )
                        )
                    )

                    val quests = executeUseCase(
                        quest = TestUtil.repeatingQuest.copy(
                            repeatingPattern = RepeatingPattern.Weekly(
                                setOf(
                                    DayOfWeek.MONDAY,
                                    DayOfWeek.WEDNESDAY,
                                    DayOfWeek.FRIDAY
                                )
                            )
                        ),
                        start = firstDateOfWeek,
                        end = lastDateOfWeek,
                        questRepo = repo
                    )
                    quests.size.`should be`(3)
                    quests.filter { it.id.isEmpty() }.size.`should be`(2)
                }
            }

            describe("flexible") {

                fun createQuest(
                    timesPerWeek: Int,
                    preferredDays: Set<DayOfWeek> = setOf(),
                    scheduledPeriods: Map<LocalDate, List<LocalDate>> = mapOf()
                ): RepeatingQuest {
                    return TestUtil.repeatingQuest.copy(
                        repeatingPattern = RepeatingPattern.Flexible.Weekly(
                            timesPerWeek = timesPerWeek,
                            preferredDays = preferredDays,
                            scheduledPeriods = scheduledPeriods
                        )
                    )
                }

                it("should not allow 1 time per week with 1 preferred day") {


                    val exec = {

                        executeUseCase(
                            createQuest(
                                timesPerWeek = 1,
                                preferredDays = setOf(DayOfWeek.MONDAY)
                            ),
                            firstDateOfWeek,
                            lastDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should not allow to repeat less than once per week") {
                    val exec = {
                        executeUseCase(
                            createQuest(
                                timesPerWeek = 0,
                                preferredDays = setOf(DayOfWeek.MONDAY)
                            ),
                            firstDateOfWeek,
                            lastDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should not allow to repeat more than 7 times per week") {
                    val exec = {
                        executeUseCase(
                            createQuest(
                                timesPerWeek = 8,
                                preferredDays = setOf(DayOfWeek.MONDAY)
                            ),
                            firstDateOfWeek,
                            lastDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should schedule 1 quest without preferred days") {
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 1
                        ),
                        firstDateOfWeek,
                        lastDateOfWeek
                    )

                    quests.size.`should be`(1)
                }

                it("should schedule 1 quest with 2 preferred days") {
                    val preferredDays = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 1,
                            preferredDays = preferredDays
                        ),
                        firstDateOfWeek,
                        lastDateOfWeek
                    )

                    quests.size.`should be`(1)
                    val scheduledDate = quests.first().scheduledDate
                    scheduledDate.dayOfWeek.`should be in`(preferredDays)
                }

                it("should schedule 2 quest with 1 preferred day") {
                    val preferredDays = setOf(DayOfWeek.FRIDAY)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 2,
                            preferredDays = preferredDays
                        ),
                        firstDateOfWeek,
                        lastDateOfWeek
                    )

                    quests.size.`should be`(2)
                    val scheduledDates = quests.map { it.scheduledDate }
                    scheduledDates.filter { it.dayOfWeek == DayOfWeek.FRIDAY }.size.`should be`(1)
                }

                it("should schedule 3 quest with 4 preferred day") {
                    val preferredDays = setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    )
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 3,
                            preferredDays = preferredDays
                        ),
                        firstDateOfWeek.with(DayOfWeek.WEDNESDAY),
                        lastDateOfWeek
                    )

                    quests.size.`should be in range`(1, 2)
                    val possibleWeekDays = setOf(
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    )
                    if (quests.size == 1) {
                        quests.first().scheduledDate.dayOfWeek.`should be in`(
                            possibleWeekDays
                        )
                    } else {
                        quests.map { it.scheduledDate.dayOfWeek }.`should contain all`(
                            possibleWeekDays
                        )
                    }
                }

                it("should schedule 5 quests for 2 days") {
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 5
                        ),
                        firstDateOfWeek.with(DayOfWeek.SATURDAY),
                        lastDateOfWeek
                    )

                    quests.size.`should be in range`(1, 2)
                    val possibleWeekDays = setOf(
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                    )
                    if (quests.size == 1) {
                        quests.first().scheduledDate.dayOfWeek.`should be in`(
                            possibleWeekDays
                        )
                    } else {
                        quests.map { it.scheduledDate.dayOfWeek }.`should contain all`(
                            possibleWeekDays
                        )
                    }
                }

                xit("should schedule quests in different weeks") {
                    val possibleWeekDays = setOf(
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.FRIDAY
                    )
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 1,
                            preferredDays = possibleWeekDays
                        ),
                        firstDateOfWeek.with(DayOfWeek.THURSDAY),
                        lastDateOfWeek.plusDays(1).with(DayOfWeek.SATURDAY)
                    )

                    quests.size.`should be in range`(1, 2)
                    if (quests.size == 1) {
                        quests.first().scheduledDate.dayOfWeek.`should be in`(
                            possibleWeekDays
                        )
                    } else {
                        quests.forEach {
                            it.scheduledDate.dayOfWeek.`should be in`(possibleWeekDays)
                        }
                        val first = quests.first().scheduledDate
                        val second = quests[1].scheduledDate
                        first.dayOfWeek.`should not be`(second.dayOfWeek)
                    }
                }

                it("should add 1 scheduled period") {
                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerWeek = 1,
                            scheduledPeriods = mapOf()
                        ),
                        firstDateOfWeek,
                        lastDateOfWeek
                    )

                    val scheduledPeriods =
                        (rq.repeatingPattern as RepeatingPattern.Flexible.Weekly).scheduledPeriods
                    scheduledPeriods.size.`should be`(1)
                    scheduledPeriods.keys.first().`should be`(firstDateOfWeek)
                    scheduledPeriods[firstDateOfWeek]!!.size.`should be`(1)

                }

                it("should add 2 scheduled periods") {
                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerWeek = 3,
                            scheduledPeriods = mapOf()
                        ),
                        firstDateOfWeek.plusDays(1),
                        lastDateOfWeek.plusDays(3)
                    )

                    val scheduledPeriods =
                        (rq.repeatingPattern as RepeatingPattern.Flexible.Weekly).scheduledPeriods
                    scheduledPeriods.size.`should be`(2)
                    scheduledPeriods.keys.`should contain all`(
                        listOf(
                            firstDateOfWeek,
                            lastDateOfWeek.plusDays(1)
                        )
                    )
                }

                it("should add 1 scheduled periods and not override the other") {
                    val scheduledPeriodDates = listOf(
                        firstDateOfWeek,
                        firstDateOfWeek.plusDays(1),
                        firstDateOfWeek.plusDays(2)
                    )

                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerWeek = 3,
                            scheduledPeriods = mapOf(
                                firstDateOfWeek to scheduledPeriodDates
                            )
                        ),
                        firstDateOfWeek.plusDays(1),
                        lastDateOfWeek.plusDays(3)
                    )

                    val scheduledPeriods =
                        (rq.repeatingPattern as RepeatingPattern.Flexible.Weekly).scheduledPeriods
                    scheduledPeriods.size.`should be`(2)
                    scheduledPeriods[firstDateOfWeek]!!.`should contain all`(scheduledPeriodDates)
                }
            }
        }

        describe("repeating monthly") {

            describe("fixed") {

                it("should schedule for every month day in pattern when days present in month") {

                    val repo = mockQuestsForRepeatingQuest(listOf())

                    val start = LocalDate.of(2000, 2, 1)
                    val end = LocalDate.of(2000, 3, 15)


                    val quests = executeUseCase(
                        quest = TestUtil.repeatingQuest.copy(
                            start = start,
                            repeatingPattern = RepeatingPattern.Monthly(
                                setOf(1, 10, 29, 31)
                            )
                        ),
                        start = start,
                        end = end,
                        questRepo = repo
                    )
                    quests.size.`should be`(5)
                    quests.filter { it.id.isEmpty() }.size.`should be`(5)
                }

                it("should not schedule for stored quests") {

                    val start = LocalDate.of(2000, 2, 1)
                    val end = LocalDate.of(2000, 3, 15)

                    val repo = mockQuestsForRepeatingQuest(
                        listOf(
                            TestUtil.quest.copy(
                                id = questId,
                                originalScheduledDate = start
                            )
                        )
                    )

                    val quests = executeUseCase(
                        quest = TestUtil.repeatingQuest.copy(
                            start = start,
                            repeatingPattern = RepeatingPattern.Monthly(
                                setOf(1, 10, 29, 31)
                            )
                        ),
                        start = start,
                        end = end,
                        questRepo = repo
                    )
                    quests.size.`should be`(5)
                    quests.filter { it.id.isEmpty() }.size.`should be`(4)
                }
            }

            describe("flexible") {
                val firstJanuary = LocalDate.now().withMonth(1).withDayOfMonth(1)
                val lastJanuary = firstJanuary.with(TemporalAdjusters.lastDayOfMonth())
                val firstFebruary = LocalDate.now().withMonth(2).withDayOfMonth(1)
                val lastFebruary = firstFebruary.with(TemporalAdjusters.lastDayOfMonth())

                fun createQuest(
                    timesPerMonth: Int,
                    preferredDays: Set<Int> = setOf(),
                    scheduledPeriods: Map<LocalDate, List<LocalDate>> = mapOf()
                ): RepeatingQuest {
                    return TestUtil.repeatingQuest.copy(
                        start = firstJanuary,
                        repeatingPattern = RepeatingPattern.Flexible.Monthly(
                            timesPerMonth = timesPerMonth,
                            preferredDays = preferredDays,
                            scheduledPeriods = scheduledPeriods
                        )
                    )
                }

                it("should not allow 1 time per month with 1 preferred day") {

                    val exec = {

                        executeUseCase(
                            createQuest(
                                timesPerMonth = 1,
                                preferredDays = setOf(1)
                            ),
                            firstDateOfWeek,
                            lastDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should not allow to repeat less than once per month") {
                    val exec = {
                        executeUseCase(
                            createQuest(
                                timesPerMonth = 0,
                                preferredDays = setOf(1)
                            ),
                            firstDateOfWeek,
                            lastDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should not allow to repeat more than 31 times per month") {
                    val exec = {
                        executeUseCase(
                            createQuest(
                                timesPerMonth = 32,
                                preferredDays = setOf(1)
                            ),
                            firstDateOfWeek,
                            lastDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should schedule 1 quest without preferred days") {
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 1
                        ),
                        firstJanuary,
                        lastJanuary
                    )

                    quests.size.`should be`(1)
                }

                it("should schedule 1 quest with 2 preferred days") {
                    val preferredDays = setOf(10, 20)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 1,
                            preferredDays = preferredDays
                        ),
                        firstJanuary,
                        lastJanuary
                    )
                    quests.size.`should be`(1)
                    val scheduledDate = quests.first().scheduledDate
                    scheduledDate.dayOfMonth.`should be in`(preferredDays)
                }

                it("should schedule 2 quest with 1 preferred day") {
                    val preferredDays = setOf(10)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 2,
                            preferredDays = preferredDays
                        ),
                        firstJanuary,
                        lastJanuary
                    )

                    quests.size.`should be`(2)
                    val scheduledDates = quests.map { it.scheduledDate }
                    scheduledDates.filter { it.dayOfMonth == 10 }.size.`should be`(1)
                }

                it("should schedule 3 quest with 4 preferred day") {
                    val preferredDays = setOf(10, 12, 20, 25)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 3,
                            preferredDays = preferredDays
                        ),
                        firstJanuary.plusDays(15),
                        lastJanuary
                    )

                    quests.size.`should be in range`(1, 2)
                    val possibleMonthDays = setOf(20, 25)
                    if (quests.size == 1) {
                        quests.first().scheduledDate.dayOfMonth.`should be in`(
                            possibleMonthDays
                        )
                    } else {
                        quests.map { it.scheduledDate.dayOfMonth }.`should contain all`(
                            possibleMonthDays
                        )
                    }
                }

                it("should schedule 30 quests for 2 days") {
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 30
                        ),
                        firstJanuary.plusDays(29),
                        lastJanuary
                    )

                    quests.size.`should be in range`(1, 2)
                    val possibleMonthDays = setOf(30, 31)
                    if (quests.size == 1) {
                        quests.first().scheduledDate.dayOfMonth.`should be in`(
                            possibleMonthDays
                        )
                    } else {
                        quests.map { it.scheduledDate.dayOfMonth }.`should contain all`(
                            possibleMonthDays
                        )
                    }
                }

                it("should schedule 30 quests in February") {
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 30
                        ),
                        firstFebruary,
                        lastFebruary
                    )

                    quests.size.`should be`(firstFebruary.lengthOfMonth())
                }

                it("should schedule quests in different months") {
                    val possibleMonthDays = setOf(10, 20)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 1,
                            preferredDays = possibleMonthDays
                        ),
                        firstJanuary.plusDays(15),
                        firstFebruary.plusDays(25)
                    )

                    quests.size.`should be in range`(1, 2)
                    if (quests.size == 1) {
                        val scheduledDate = quests.first().scheduledDate
                        scheduledDate.dayOfMonth.`should be in`(possibleMonthDays)
                        scheduledDate.month.`should be`(Month.FEBRUARY)
                    } else {
                        quests.forEach {
                            it.scheduledDate.dayOfMonth.`should be in`(possibleMonthDays)
                        }
                        val first = quests.first().scheduledDate
                        val second = quests[1].scheduledDate
                        first.monthValue.`should not be equal to`(second.monthValue)
                    }
                }

                it("should add 1 scheduled period") {
                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerMonth = 1,
                            scheduledPeriods = mapOf()
                        ),
                        firstJanuary,
                        lastJanuary
                    )

                    val scheduledPeriods =
                        (rq.repeatingPattern as RepeatingPattern.Flexible.Monthly).scheduledPeriods
                    scheduledPeriods.size.`should be`(1)
                    scheduledPeriods.keys.first().`should be`(firstJanuary)
                    scheduledPeriods[firstJanuary]!!.size.`should be`(1)

                }

                it("should add 2 scheduled periods") {
                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerMonth = 3,
                            scheduledPeriods = mapOf()
                        ),
                        firstJanuary.plusDays(7),
                        lastFebruary.minusDays(7)
                    )

                    val scheduledPeriods =
                        (rq.repeatingPattern as RepeatingPattern.Flexible.Monthly).scheduledPeriods
                    scheduledPeriods.size.`should be`(2)
                    scheduledPeriods.keys.`should contain all`(
                        listOf(
                            firstJanuary,
                            firstFebruary
                        )
                    )
                }

                it("should add 1 scheduled periods and not override the other") {
                    val scheduledPeriodDates = listOf(
                        firstFebruary,
                        firstFebruary.plusDays(1),
                        firstFebruary.plusDays(2)
                    )

                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerMonth = 3,
                            scheduledPeriods = mapOf(
                                firstFebruary to scheduledPeriodDates
                            )
                        ),
                        firstJanuary.plusDays(7),
                        lastFebruary.minusDays(7)
                    )

                    val scheduledPeriods =
                        (rq.repeatingPattern as RepeatingPattern.Flexible.Monthly).scheduledPeriods
                    scheduledPeriods.size.`should be`(2)
                    scheduledPeriods[firstFebruary]!!.`should contain all`(scheduledPeriodDates)
                }
            }
        }

        describe("repeating yearly") {

            it("should not schedule for date outside range") {
                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatingPattern = RepeatingPattern.Yearly(
                            firstDateOfWeek.dayOfMonth,
                            firstDateOfWeek.monthValue
                        )
                    ),
                    start = firstDateOfWeek.plusDays(1),
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.`should be empty`()
            }

            it("should create when no quest is scheduled") {
                val repo = mockQuestsForRepeatingQuest(listOf())

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatingPattern = RepeatingPattern.Yearly(
                            firstDateOfWeek.dayOfMonth,
                            firstDateOfWeek.monthValue
                        )
                    ),
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(1)
                quests.first().id.`should be empty`()
            }

            it("should return scheduled quest") {
                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatingPattern = RepeatingPattern.Yearly(
                            firstDateOfWeek.dayOfMonth,
                            firstDateOfWeek.monthValue
                        )
                    ),
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(1)
                quests.first().id.`should be`(questId)
            }

            it("should not schedule for dates with deleted quest") {

                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek,
                            isRemoved = true
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatingPattern = RepeatingPattern.Yearly(
                            firstDateOfWeek.dayOfMonth,
                            firstDateOfWeek.monthValue
                        )
                    ),
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.`should be empty`()
            }

            it("should schedule quests for 3 years") {
                val start = firstDateOfWeek
                val end = firstDateOfWeek.plusYears(3)
                val repo = mockQuestsForRepeatingQuest(listOf())

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatingPattern = RepeatingPattern.Yearly(
                            firstDateOfWeek.plusDays(1).dayOfMonth,
                            firstDateOfWeek.plusDays(1).monthValue
                        )
                    ),
                    start = start,
                    end = end,
                    questRepo = repo
                )
                quests.size.`should be`(3)
                quests.filter { it.id.isEmpty() }.size.`should be`(3)
            }

            it("should schedule 1 quest and use 2 stored quests for 3 years") {
                val start = firstDateOfWeek
                val end = firstDateOfWeek.plusYears(3)
                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = "1",
                            originalScheduledDate = firstDateOfWeek.plusDays(1)
                        ),
                        TestUtil.quest.copy(
                            id = "2",
                            originalScheduledDate = firstDateOfWeek.plusDays(1).plusYears(2)
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatingPattern = RepeatingPattern.Yearly(
                            firstDateOfWeek.plusDays(1).dayOfMonth,
                            firstDateOfWeek.plusDays(1).monthValue
                        )
                    ),
                    start = start,
                    end = end,
                    questRepo = repo
                )
                quests.size.`should be`(3)
                quests.filter { it.id.isEmpty() }.size.`should be`(1)
            }
        }
    }
})