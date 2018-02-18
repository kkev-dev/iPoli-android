package mypoli.android.quest.usecase

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import mypoli.android.common.datetime.Time
import mypoli.android.player.usecase.RemoveRewardFromPlayerUseCase
import mypoli.android.quest.Category
import mypoli.android.quest.Color
import mypoli.android.quest.Quest
import mypoli.android.quest.Reminder
import mypoli.android.quest.data.persistence.QuestRepository
import mypoli.android.quest.job.ReminderScheduler
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 11/15/17.
 */
class UndoCompleteQuestUseCaseSpek : Spek({

    describe("UndoCompletedQuestUseCase") {
        val questId = "sampleid"

        val quest = Quest(
            name = "",
            color = Color.BLUE,
            category = Category("Wellness", Color.BLUE),
            scheduledDate = LocalDate.now(),
            duration = 30,
            reminder = Reminder("", Time.now(), LocalDate.now()),
            experience = 20,
            coins = 10,
            bounty = Quest.Bounty.None
        )

        var questRepo = mock<QuestRepository>()

        var reminderScheduler = mock<ReminderScheduler>()

        val removeRewardFromPlayerUseCase = mock<RemoveRewardFromPlayerUseCase>()

        var useCase = UndoCompletedQuestUseCase(
            questRepo,
            reminderScheduler,
            removeRewardFromPlayerUseCase
        )

        beforeEachTest {
            questRepo = mock<QuestRepository> {

                on { findById(any()) } doReturn
                    quest

                on { findNextReminderTime(any()) } doReturn LocalDateTime.now()
            }

            reminderScheduler = mock<ReminderScheduler>()

            useCase = UndoCompletedQuestUseCase(
                questRepo,
                reminderScheduler,
                removeRewardFromPlayerUseCase
            )
        }

        it("should fail when questId is empty") {
            val call = { useCase.execute("") }
            call `should throw` IllegalArgumentException::class
        }

        it("should mark quest as not completed") {
            val newQuest = useCase.execute(questId)
            newQuest.completedAtTime.shouldBeNull()
            newQuest.completedAtDate.shouldBeNull()
        }

        it("should schedule next reminder") {
            useCase.execute(questId)
            Verify on reminderScheduler that reminderScheduler.schedule(any()) was called
        }

        it("should not remove XP") {
            val newQuest = useCase.execute(questId)
            newQuest.experience.shouldNotBeNull()
        }

        it("should not remove coins") {
            val newQuest = useCase.execute(questId)
            newQuest.coins.shouldNotBeNull()
        }
    }

})




