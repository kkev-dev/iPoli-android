package mypoli.android.quest.data.persistence

import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import mypoli.android.common.datetime.*
import mypoli.android.common.persistence.BaseCollectionFirestoreRepository
import mypoli.android.common.persistence.CollectionRepository
import mypoli.android.common.persistence.FirestoreModel
import mypoli.android.pet.Food
import mypoli.android.quest.*
import org.threeten.bp.*
import kotlin.coroutines.experimental.CoroutineContext

interface QuestRepository : CollectionRepository<Quest> {
    fun listenForScheduledBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): ReceiveChannel<List<Quest>>

    fun listenForScheduledAt(date: LocalDate): ReceiveChannel<List<Quest>>
    fun findScheduledAt(date: LocalDate): List<Quest>
    fun findForRepeatingQuestBetween(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate
    ): List<Quest>

    fun findNextReminderTime(afterTime: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): LocalDateTime?
    fun findQuestsToRemind(remindTime: LocalDateTime): List<Quest>
    fun findCompletedForDate(date: LocalDate): List<Quest>
    fun findStartedQuests(): List<Quest>
    fun findLastScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate?
    fun findFirstScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate?
}

data class DbQuest(override val map: MutableMap<String, Any?> = mutableMapOf()) :
    FirestoreModel {
    override var id: String by map
    var name: String by map
    var color: String by map
    var icon: String? by map
    var category: String by map
    var duration: Int by map
    var reminder: MutableMap<String, Any?>? by map
    var startMinute: Long? by map
    var experience: Long? by map
    var coins: Long? by map
    var bounty: MutableMap<String, Any?>? by map
    var scheduledDate: Long by map
    var completedAtDate: Long? by map
    var completedAtMinute: Long? by map
    var timeRanges: List<MutableMap<String, Any?>> by map
    var timeRangeCount: Int by map
    override var createdAt: Long by map
    override var updatedAt: Long by map
    override var removedAt: Long? by map
}

data class DbReminder(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var message: String by map
    var minute: Int by map
    var date: Long by map
}

data class DbBounty(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var type: String by map
    var name: String? by map

    enum class Type {
        NONE, FOOD
    }
}

data class DbTimeRange(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var type: String by map
    var duration: Int by map
    var start: Long? by map
    var end: Long? by map
}

class FirestoreQuestRepository(
    database: FirebaseFirestore,
    coroutineContext: CoroutineContext,
    sharedPreferences: SharedPreferences
) : BaseCollectionFirestoreRepository<Quest, DbQuest>(
    database,
    coroutineContext,
    sharedPreferences
), QuestRepository {

    override fun listenForScheduledBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): ReceiveChannel<List<Quest>> {

        val query = collectionReference
            .whereGreaterThanOrEqualTo("scheduledDate", startDate.startOfDayUTC())
            .whereLessThanOrEqualTo("scheduledDate", endDate.startOfDayUTC())
            .orderBy("scheduledDate")
            .orderBy("startMinute")
        return listenForChanges(query)
    }

    override fun findScheduledAt(date: LocalDate) =
        collectionReference
            .whereGreaterThanOrEqualTo("scheduledDate", date.startOfDayUTC())
            .whereLessThanOrEqualTo("scheduledDate", date.startOfDayUTC())
            .orderBy("scheduledDate")
            .orderBy("startMinute")
            .entities

    override fun findForRepeatingQuestBetween(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate
    ): List<Quest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listenForScheduledAt(date: LocalDate): ReceiveChannel<List<Quest>> {
        val query = collectionReference
            .whereEqualTo("scheduledDate", date.startOfDayUTC())
        return listenForChanges(query)
    }

    override fun findNextReminderTime(afterTime: ZonedDateTime): LocalDateTime? {

        val currentDateMillis = afterTime.toLocalDate().startOfDayUTC()

        val millisOfDay = afterTime.toLocalTime().toSecondOfDay().seconds.millisValue

        val query =
            remindersReference
                .orderBy("date")
                .orderBy("millisOfDay")
                .startAt(
                    currentDateMillis,
                    millisOfDay + 1
                )
                .limit(1)

        val documents = query.documents
        if (documents.isEmpty()) {
            return null
        }

        val reminder = documents[0]

        val remindDate = (reminder.get("date") as Long).startOfDayUtc
        val remindMillis = reminder.get("millisOfDay") as Long
        return LocalDateTime.of(
            remindDate,
            LocalTime.ofSecondOfDay(remindMillis.milliseconds.asSeconds.longValue)
        )
    }

    override fun findQuestsToRemind(remindTime: LocalDateTime): List<Quest> {
        val query = remindersReference
            .whereEqualTo("date", remindTime.toLocalDate().startOfDayUTC())
            .whereEqualTo(
                "millisOfDay", remindTime.toLocalTime().toSecondOfDay().seconds.millisValue
            )
        val documents = query.documents
        if (documents.isEmpty()) {
            return listOf()
        }
        val questIds = documents.map {
            it["questId"]
        }

        var questRef: Query = collectionReference
        questIds.forEach {
            questRef = questRef.whereEqualTo("id", it)
        }
        return questRef.entities
    }

    override fun findStartedQuests(): List<Quest> {
        val query = collectionReference
            .whereEqualTo("completedAtDate", null)
            .whereGreaterThan("timeRangeCount", 0)
        return query.entities
    }

    override fun findCompletedForDate(date: LocalDate): List<Quest> {
        val query = collectionReference
            // Due to Firestore bug (kinda) we can't query using the same value as data
            // see https://stackoverflow.com/a/47379643/6336582
            .whereGreaterThan("completedAtDate", date.startOfDayUTC() - 1)
            .whereLessThanOrEqualTo("completedAtDate", date.startOfDayUTC())
        return query.entities
    }

    override fun findLastScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate? {
        val endDateQuery = collectionReference
            .whereGreaterThan("scheduledDate", currentDate.startOfDayUTC())
            .limit(maxQuests.toLong())
            .orderBy("scheduledDate", Query.Direction.ASCENDING)
        val endDateQuests = endDateQuery.entities

        if (endDateQuests.isEmpty()) {
            return null
        }

        return endDateQuests.last().scheduledDate
    }

    override fun findFirstScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate? {
        val startDateQuery = collectionReference
            .whereLessThan("scheduledDate", currentDate.startOfDayUTC())
            .limit(maxQuests.toLong())
            .orderBy("scheduledDate", Query.Direction.DESCENDING)
        val startDateQuests = startDateQuery.entities

        if (startDateQuests.isEmpty()) {
            return null
        }

        return startDateQuests.last().scheduledDate
    }

    override val collectionReference
        get() = database.collection("players").document(playerId).collection("quests")

    private val remindersReference
        get() = database.collection("players").document(playerId).collection("reminders")

    override fun save(entity: Quest): Quest {
        val quest = super.save(entity)
        quest.reminder?.let {
            saveReminders(quest.id, listOf(it))
        }
        return quest
    }

    private fun saveReminders(questId: String, reminders: List<Reminder>) {
        deleteReminders(questId)
        addReminders(reminders, questId)
    }

    private fun addReminders(
        reminders: List<Reminder>,
        questId: String
    ) {
        reminders.forEach {
            val r = mapOf(
                "questId" to questId,
                "date" to it.remindDate.startOfDayUTC(),
                "millisOfDay" to it.remindTime.toMillisOfDay()
            )
            remindersReference.add(r)
        }
    }

    private fun deleteReminders(questId: String) {
        val query = remindersReference.whereEqualTo("questId", questId)
        query.documents.forEach {
            remindersReference.document(it.id).delete()
        }
    }

    override fun remove(id: String) {
        super.remove(id)
        deleteReminders(id)
    }

    override fun undoRemove(id: String) {
        super.undoRemove(id)
        val quest = findById(id)!!
        quest.reminder?.let {
            addReminders(listOf(it), id)
        }
    }

    override fun toEntityObject(dataMap: MutableMap<String, Any?>): Quest {
        val cq = DbQuest(dataMap.withDefault {
            null
        })

        val plannedDate = cq.scheduledDate.startOfDayUtc
        val plannedTime = cq.startMinute?.let { Time.of(it.toInt()) }

        return Quest(
            id = cq.id,
            name = cq.name,
            color = Color.valueOf(cq.color),
            icon = cq.icon?.let {
                Icon.valueOf(it)
            },
            category = Category(cq.category, Color.GREEN),
            scheduledDate = plannedDate,
            startTime = plannedTime,
            duration = cq.duration,
            experience = cq.experience?.toInt(),
            coins = cq.coins?.toInt(),
            bounty = cq.bounty?.let {
                val cr = DbBounty(it)
                when {
                    cr.type == DbBounty.Type.NONE.name -> Quest.Bounty.None
                    cr.type == DbBounty.Type.FOOD.name -> Quest.Bounty.Food(Food.valueOf(cr.name!!))
                    else -> null
                }
            },
            completedAtDate = cq.completedAtDate?.startOfDayUtc,
            completedAtTime = cq.completedAtMinute?.let {
                Time.of(it.toInt())
            },
            reminder = cq.reminder?.let {
                val cr = DbReminder(it)
                Reminder(cr.message, Time.of(cr.minute), cr.date.startOfDayUtc)
            },
            timeRanges = cq.timeRanges.map {
                val ctr = DbTimeRange(it)
                TimeRange(
                    TimeRange.Type.valueOf(ctr.type),
                    ctr.duration,
                    ctr.start?.instant,
                    ctr.end?.instant
                )
            }
        )
    }

    override fun toDatabaseObject(entity: Quest): DbQuest {
        val q = DbQuest()
        q.id = entity.id
        q.name = entity.name
        q.category = entity.category.name
        q.color = entity.color.name
        q.icon = entity.icon?.name
        q.duration = entity.duration
        q.scheduledDate = entity.scheduledDate.startOfDayUTC()
        q.reminder = entity.reminder?.let {
            createDbReminder(it).map
        }
        q.experience = entity.experience?.toLong()
        q.coins = entity.coins?.toLong()
        q.bounty = entity.bounty?.let {
            val cr = DbBounty()

            cr.type = when (it) {
                Quest.Bounty.None -> DbBounty.Type.NONE.name
                is Quest.Bounty.Food -> DbBounty.Type.FOOD.name
            }

            if (it is Quest.Bounty.Food) {
                cr.name = it.food.name
            }

            cr.map
        }
        q.startMinute = entity.startTime?.toMinuteOfDay()?.toLong()
        q.completedAtDate = entity.completedAtDate?.startOfDayUTC()
        q.completedAtMinute = entity.completedAtTime?.toMinuteOfDay()?.toLong()
        q.timeRanges = entity.timeRanges.map {
            createDbTimeRange(it).map
        }
        q.timeRangeCount = q.timeRanges.size
        return q
    }

    private fun createDbTimeRange(timeRange: TimeRange): DbTimeRange {
        val cTimeRange = DbTimeRange()
        cTimeRange.type = timeRange.type.name
        cTimeRange.duration = timeRange.duration
        cTimeRange.start = timeRange.start?.toEpochMilli()
        cTimeRange.end = timeRange.end?.toEpochMilli()
        return cTimeRange
    }

    private fun createDbReminder(reminder: Reminder): DbReminder {
        val cr = DbReminder()
        cr.message = reminder.message
        cr.date = reminder.remindDate.startOfDayUTC()
        cr.minute = reminder.remindTime.toMinuteOfDay()
        return cr
    }

}