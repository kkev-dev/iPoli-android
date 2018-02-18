package mypoli.android.common.di

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.view.LayoutInflater
import com.couchbase.lite.Database
import com.couchbase.lite.DatabaseConfiguration
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import mypoli.android.auth.AuthReducer
import mypoli.android.challenge.PersonalizeChallengePresenter
import mypoli.android.challenge.category.ChallengeCategoryListPresenter
import mypoli.android.challenge.category.list.ChallengeListForCategoryReducer
import mypoli.android.challenge.usecase.BuyChallengeUseCase
import mypoli.android.challenge.usecase.ScheduleChallengeUseCase
import mypoli.android.common.*
import mypoli.android.common.redux.CoroutineSideEffectExecutor
import mypoli.android.common.redux.StateStore
import mypoli.android.common.text.CalendarFormatter
import mypoli.android.common.view.ColorPickerPresenter
import mypoli.android.common.view.CurrencyConverterPresenter
import mypoli.android.common.view.IconPickerDialogPresenter
import mypoli.android.common.view.PetMessagePresenter
import mypoli.android.home.HomeReducer
import mypoli.android.pet.AndroidJobLowerPetStatsScheduler
import mypoli.android.pet.LowerPetStatsScheduler
import mypoli.android.pet.PetDialogPresenter
import mypoli.android.pet.PetPresenter
import mypoli.android.pet.store.PetStoreReducer
import mypoli.android.pet.usecase.*
import mypoli.android.player.AndroidLevelDownScheduler
import mypoli.android.player.AndroidLevelUpScheduler
import mypoli.android.player.LevelDownScheduler
import mypoli.android.player.LevelUpScheduler
import mypoli.android.player.auth.saga.AuthSideEffect
import mypoli.android.player.persistence.FirestorePlayerRepository
import mypoli.android.player.persistence.PlayerRepository
import mypoli.android.player.usecase.*
import mypoli.android.player.view.LevelUpPresenter
import mypoli.android.quest.CompletedQuestPresenter
import mypoli.android.quest.data.persistence.FirestoreQuestRepository
import mypoli.android.quest.data.persistence.QuestRepository
import mypoli.android.quest.job.AndroidJobQuestCompleteScheduler
import mypoli.android.quest.job.AndroidJobReminderScheduler
import mypoli.android.quest.job.QuestCompleteScheduler
import mypoli.android.quest.job.ReminderScheduler
import mypoli.android.quest.schedule.ScheduleReducer
import mypoli.android.quest.schedule.addquest.AddQuestPresenter
import mypoli.android.quest.schedule.agenda.AgendaReducer
import mypoli.android.quest.schedule.agenda.usecase.CreateAgendaItemsUseCase
import mypoli.android.quest.schedule.agenda.usecase.FindAgendaDatesUseCase
import mypoli.android.quest.schedule.calendar.CalendarReducer
import mypoli.android.quest.schedule.calendar.dayview.DayViewPresenter
import mypoli.android.quest.usecase.*
import mypoli.android.quest.view.QuestCompletePresenter
import mypoli.android.rate.AndroidRatePopupScheduler
import mypoli.android.rate.RatePopupScheduler
import mypoli.android.rate.RatePresenter
import mypoli.android.reminder.view.formatter.ReminderTimeFormatter
import mypoli.android.reminder.view.formatter.TimeUnitFormatter
import mypoli.android.reminder.view.picker.ReminderPickerDialogPresenter
import mypoli.android.repeatingquest.list.RepeatingQuestListReducer
import mypoli.android.store.GemStorePresenter
import mypoli.android.store.theme.ThemeStorePresenter
import mypoli.android.store.theme.usecase.BuyThemeUseCase
import mypoli.android.store.theme.usecase.ChangeThemeUseCase
import mypoli.android.store.usecase.PurchaseGemPackUseCase
import mypoli.android.timer.TimerPresenter
import mypoli.android.timer.job.AndroidJobTimerCompleteScheduler
import mypoli.android.timer.job.TimerCompleteScheduler
import mypoli.android.timer.usecase.*
import space.traversal.kapsule.HasModules
import space.traversal.kapsule.Injects
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 9/10/17.
 */
interface RepositoryModule {
    val questRepository: QuestRepository
    val playerRepository: PlayerRepository
}

class FirestoreRepositoryModule : RepositoryModule, Injects<Module> {

    override val questRepository by required {
        FirestoreQuestRepository(
            firestoreDatabase,
            job + CommonPool,
            sharedPreferences
        )
    }

    override val playerRepository
        by required {
            FirestorePlayerRepository(
                firestoreDatabase,
                job + CommonPool,
                sharedPreferences
            )
        }
}

interface AndroidModule {
    val layoutInflater: LayoutInflater

    val sharedPreferences: SharedPreferences

    val reminderTimeFormatter: ReminderTimeFormatter

    val timeUnitFormatter: TimeUnitFormatter

    val calendarFormatter: CalendarFormatter

    val database: Database

    val firestoreDatabase: FirebaseFirestore

    val reminderScheduler: ReminderScheduler

    val timerCompleteScheduler: TimerCompleteScheduler

    val questCompleteScheduler: QuestCompleteScheduler

    val levelUpScheduler: LevelUpScheduler

    val levelDownScheduler: LevelDownScheduler

    val lowerPetStatsScheduler: LowerPetStatsScheduler

    val ratePopupScheduler: RatePopupScheduler

    val job: Job
}

class MainAndroidModule(
    private val context: Context,
    firestoreDb: FirebaseFirestore
) : AndroidModule {
    override val layoutInflater: LayoutInflater get() = LayoutInflater.from(context)

    override val sharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)

    override val reminderTimeFormatter get() = ReminderTimeFormatter(context)

    override val timeUnitFormatter get() = TimeUnitFormatter(context)

    override val calendarFormatter get() = CalendarFormatter(context)

    override val reminderScheduler get() = AndroidJobReminderScheduler()

    override val timerCompleteScheduler get() = AndroidJobTimerCompleteScheduler()

    override val questCompleteScheduler get() = AndroidJobQuestCompleteScheduler()

    override val levelUpScheduler get() = AndroidLevelUpScheduler()

    override val levelDownScheduler get() = AndroidLevelDownScheduler()

    override val lowerPetStatsScheduler get() = AndroidJobLowerPetStatsScheduler()

    override val ratePopupScheduler get() = AndroidRatePopupScheduler()

    override val firestoreDatabase = firestoreDb

    override val database: Database
        get() = Database("myPoli", DatabaseConfiguration.Builder(context).build())

    override val job get() = Job()
}

class MainUseCaseModule : UseCaseModule, Injects<Module> {
    private val questRepository by required { questRepository }
    private val playerRepository by required { playerRepository }
    private val reminderScheduler by required { reminderScheduler }
    private val questCompleteScheduler by required { questCompleteScheduler }
    private val levelUpScheduler by required { levelUpScheduler }
    private val levelDownScheduler by required { levelDownScheduler }
    private val rateDialogScheduler by required { ratePopupScheduler }
    private val timerCompleteScheduler by required { timerCompleteScheduler }

    override val loadScheduleForDateUseCase
        get() = LoadScheduleForDateUseCase(questRepository)
    override val saveQuestUseCase
        get() = SaveQuestUseCase(
            questRepository,
            reminderScheduler
        )
    override val removeQuestUseCase
        get() = RemoveQuestUseCase(
            questRepository,
            timerCompleteScheduler,
            reminderScheduler
        )
    override val undoRemoveQuestUseCase get() = UndoRemovedQuestUseCase(questRepository)
    override val findQuestToRemindUseCase get() = FindQuestsToRemindUseCase(questRepository)
    override val snoozeQuestUseCase get() = SnoozeQuestUseCase(questRepository, reminderScheduler)
    override val completeQuestUseCase
        get() = CompleteQuestUseCase(
            questRepository,
            playerRepository,
            reminderScheduler,
            questCompleteScheduler,
            rateDialogScheduler,
            rewardPlayerUseCase
        )
    override val undoCompletedQuestUseCase
        get() = UndoCompletedQuestUseCase(
            questRepository,
            reminderScheduler,
            removeRewardFromPlayerUseCase
        )
    override val listenForPlayerChangesUseCase
        get() = ListenForPlayerChangesUseCase(
            playerRepository
        )
    override val rewardPlayerUseCase get() = RewardPlayerUseCase(playerRepository, levelUpScheduler)
    override val removeRewardFromPlayerUseCase
        get() = RemoveRewardFromPlayerUseCase(
            playerRepository,
            levelDownScheduler
        )
    override val feedPetUseCase get() = FeedPetUseCase(playerRepository)
    override val revivePetUseCase get() = RevivePetUseCase(playerRepository)
    override val buyPetUseCase get() = BuyPetUseCase(playerRepository)
    override val changePetUseCase get() = ChangePetUseCase(playerRepository)
    override val findPetUseCase get() = FindPetUseCase(playerRepository)
    override val changeThemeUseCase get() = ChangeThemeUseCase(playerRepository)
    override val buyThemeUseCase get() = BuyThemeUseCase(playerRepository)
    override val renamePetUseCase get() = RenamePetUseCase(playerRepository)
    override val buyIconPackUseCase get() = BuyIconPackUseCase(playerRepository)
    override val buyColorPackUseCase get() = BuyColorPackUseCase(playerRepository)
    override val convertCoinsToGemsUseCase get() = ConvertCoinsToGemsUseCase(playerRepository)
    override val comparePetItemsUseCase get() = ComparePetItemsUseCase()
    override val buyPetItemUseCase get() = BuyPetItemUseCase(playerRepository)
    override val equipPetItemUseCase get() = EquipPetItemUseCase(playerRepository)
    override val takeOffPetItemUseCase get() = TakeOffPetItemUseCase(playerRepository)
    override val purchaseGemPackUseCase get() = PurchaseGemPackUseCase(playerRepository)
    override val scheduleChallengeUseCase: ScheduleChallengeUseCase
        get() = ScheduleChallengeUseCase(
            questRepository
        )
    override val buyChallengeUseCase get() = BuyChallengeUseCase(playerRepository)
    override val splitDurationForPomodoroTimerUseCase get() = SplitDurationForPomodoroTimerUseCase()
    override val listenForQuestChangeUseCase get() = ListenForQuestChangeUseCase(questRepository)
    override val completeTimeRangeUseCase
        get() = CompleteTimeRangeUseCase(
            questRepository,
            splitDurationForPomodoroTimerUseCase,
            completeQuestUseCase,
            timerCompleteScheduler
        )
    override val cancelTimerUseCase
        get() = CancelTimerUseCase(
            questRepository,
            timerCompleteScheduler
        )
    override val addPomodoroUseCase
        get() = AddPomodoroUseCase(
            questRepository,
            splitDurationForPomodoroTimerUseCase
        )
    override val removePomodoroUseCase
        get() = RemovePomodoroUseCase(
            questRepository,
            splitDurationForPomodoroTimerUseCase
        )
    override val addTimerToQuestUseCase: AddTimerToQuestUseCase
        get() = AddTimerToQuestUseCase(
            questRepository,
            cancelTimerUseCase,
            timerCompleteScheduler
        )

    override val findAgendaDatesUseCase get() = FindAgendaDatesUseCase(questRepository)
    override val createAgendaItemsUseCase get() = CreateAgendaItemsUseCase()

    override val findPlayerLevelUseCase
        get() = FindPlayerLevelUseCase(playerRepository)

    override val lowerPetStatsUseCase
        get() = LowerPetStatsUseCase(
            questRepository,
            playerRepository
        )
}

interface UseCaseModule {
    val loadScheduleForDateUseCase: LoadScheduleForDateUseCase
    val saveQuestUseCase: SaveQuestUseCase
    val removeQuestUseCase: RemoveQuestUseCase
    val undoRemoveQuestUseCase: UndoRemovedQuestUseCase
    val findQuestToRemindUseCase: FindQuestsToRemindUseCase
    val snoozeQuestUseCase: SnoozeQuestUseCase
    val completeQuestUseCase: CompleteQuestUseCase
    val undoCompletedQuestUseCase: UndoCompletedQuestUseCase
    val listenForPlayerChangesUseCase: ListenForPlayerChangesUseCase
    val listenForQuestChangeUseCase: ListenForQuestChangeUseCase
    val rewardPlayerUseCase: RewardPlayerUseCase
    val removeRewardFromPlayerUseCase: RemoveRewardFromPlayerUseCase
    val feedPetUseCase: FeedPetUseCase
    val revivePetUseCase: RevivePetUseCase
    val buyPetUseCase: BuyPetUseCase
    val changePetUseCase: ChangePetUseCase
    val findPetUseCase: FindPetUseCase
    val changeThemeUseCase: ChangeThemeUseCase
    val buyThemeUseCase: BuyThemeUseCase
    val renamePetUseCase: RenamePetUseCase
    val buyIconPackUseCase: BuyIconPackUseCase
    val buyColorPackUseCase: BuyColorPackUseCase
    val convertCoinsToGemsUseCase: ConvertCoinsToGemsUseCase
    val comparePetItemsUseCase: ComparePetItemsUseCase
    val buyPetItemUseCase: BuyPetItemUseCase
    val equipPetItemUseCase: EquipPetItemUseCase
    val takeOffPetItemUseCase: TakeOffPetItemUseCase
    val purchaseGemPackUseCase: PurchaseGemPackUseCase
    val scheduleChallengeUseCase: ScheduleChallengeUseCase
    val buyChallengeUseCase: BuyChallengeUseCase
    val splitDurationForPomodoroTimerUseCase: SplitDurationForPomodoroTimerUseCase
    val findPlayerLevelUseCase: FindPlayerLevelUseCase
    val lowerPetStatsUseCase: LowerPetStatsUseCase
    val completeTimeRangeUseCase: CompleteTimeRangeUseCase
    val cancelTimerUseCase: CancelTimerUseCase
    val addPomodoroUseCase: AddPomodoroUseCase
    val removePomodoroUseCase: RemovePomodoroUseCase
    val addTimerToQuestUseCase: AddTimerToQuestUseCase
    val findAgendaDatesUseCase: FindAgendaDatesUseCase
    val createAgendaItemsUseCase: CreateAgendaItemsUseCase
}

interface PresenterModule {
    val dayViewPresenter: DayViewPresenter
    val reminderPickerPresenter: ReminderPickerDialogPresenter
    val addQuestPresenter: AddQuestPresenter
    val petPresenter: PetPresenter
    val petDialogPresenter: PetDialogPresenter
    val themeStorePresenter: ThemeStorePresenter
    val colorPickerPresenter: ColorPickerPresenter
    val iconPickerPresenter: IconPickerDialogPresenter
    val currencyConverterPresenter: CurrencyConverterPresenter
    val gemStorePresenter: GemStorePresenter
    val challengeCategoryListPresenter: ChallengeCategoryListPresenter
    val personalizeChallengePresenter: PersonalizeChallengePresenter
    val timerPresenter: TimerPresenter
    val petMessagePresenter: PetMessagePresenter
    val levelUpPresenter: LevelUpPresenter
    val questCompletePresenter: QuestCompletePresenter
    val ratePresenter: RatePresenter
    val completedQuestPresenter: CompletedQuestPresenter
}

class AndroidPresenterModule : PresenterModule, Injects<Module> {

    private val questRepository by required { questRepository }
    private val playerRepository by required { playerRepository }
    private val loadScheduleForDateUseCase by required { loadScheduleForDateUseCase }
    private val saveQuestUseCase by required { saveQuestUseCase }
    private val removeQuestUseCase by required { removeQuestUseCase }
    private val undoRemoveQuestUseCase by required { undoRemoveQuestUseCase }
    private val completeQuestUseCase by required { completeQuestUseCase }
    private val undoCompleteQuestUseCase by required { undoCompletedQuestUseCase }
    private val listenForPlayerChangesUseCase by required { listenForPlayerChangesUseCase }
    private val revivePetUseCase by required { revivePetUseCase }
    private val feedPetUseCase by required { feedPetUseCase }
    private val findPetUseCase by required { findPetUseCase }
    private val renamePetUseCase by required { renamePetUseCase }
    private val changeThemeUseCase by required { changeThemeUseCase }
    private val buyThemeUseCase by required { buyThemeUseCase }
    private val reminderTimeFormatter by required { reminderTimeFormatter }
    private val timeUnitFormatter by required { timeUnitFormatter }
    private val buyIconPackUseCase by required { buyIconPackUseCase }
    private val buyColorPackUseCase by required { buyColorPackUseCase }
    private val convertCoinsToGemsUseCase by required { convertCoinsToGemsUseCase }
    private val comparePetItemsUseCase by required { comparePetItemsUseCase }
    private val buyPetItemUseCase by required { buyPetItemUseCase }
    private val equipPetItemUseCase by required { equipPetItemUseCase }
    private val takeOffPetItemUseCase by required { takeOffPetItemUseCase }
    private val purchaseGemPackUseCase by required { purchaseGemPackUseCase }
    private val scheduleChallengeUseCase by required { scheduleChallengeUseCase }
    private val splitDurationForPomodoroTimerUseCase by required { splitDurationForPomodoroTimerUseCase }
    private val listenForQuestChangeUseCase by required { listenForQuestChangeUseCase }
    private val completeTimeRangeUseCase by required { completeTimeRangeUseCase }
    private val cancelQuestTimerUseCase by required { cancelTimerUseCase }
    private val addPomodoroUseCase by required { addPomodoroUseCase }
    private val removePomodoroUseCase by required { removePomodoroUseCase }
    private val addTimerToQuestUseCase by required { addTimerToQuestUseCase }
    private val job by required { job }
    override val dayViewPresenter
        get() = DayViewPresenter(
            loadScheduleForDateUseCase,
            saveQuestUseCase,
            removeQuestUseCase,
            undoRemoveQuestUseCase,
            completeQuestUseCase,
            undoCompleteQuestUseCase,
            completeTimeRangeUseCase,
            job
        )
    override val reminderPickerPresenter
        get() = ReminderPickerDialogPresenter(
            reminderTimeFormatter,
            timeUnitFormatter,
            findPetUseCase,
            job
        )
    override val addQuestPresenter get() = AddQuestPresenter(saveQuestUseCase, job)
    override val petPresenter
        get() = PetPresenter(
            listenForPlayerChangesUseCase,
            feedPetUseCase,
            renamePetUseCase,
            revivePetUseCase,
            comparePetItemsUseCase,
            buyPetItemUseCase,
            equipPetItemUseCase,
            takeOffPetItemUseCase,
            job
        )
    override val petDialogPresenter get() = PetDialogPresenter(findPetUseCase, job)
    override val themeStorePresenter
        get() = ThemeStorePresenter(
            listenForPlayerChangesUseCase,
            changeThemeUseCase,
            buyThemeUseCase,
            job
        )
    override val colorPickerPresenter
        get() = ColorPickerPresenter(
            listenForPlayerChangesUseCase,
            buyColorPackUseCase,
            job
        )
    override val iconPickerPresenter
        get() = IconPickerDialogPresenter(
            listenForPlayerChangesUseCase,
            buyIconPackUseCase,
            job
        )
    override val currencyConverterPresenter
        get() = CurrencyConverterPresenter(
            listenForPlayerChangesUseCase,
            convertCoinsToGemsUseCase,
            job
        )
    override val gemStorePresenter
        get() = GemStorePresenter(
            purchaseGemPackUseCase,
            listenForPlayerChangesUseCase,
            job
        )
    override val challengeCategoryListPresenter
        get() = ChallengeCategoryListPresenter(
            job
        )
    override val personalizeChallengePresenter
        get() = PersonalizeChallengePresenter(
            scheduleChallengeUseCase,
            job
        )
    override val timerPresenter
        get() = TimerPresenter(
            splitDurationForPomodoroTimerUseCase,
            listenForQuestChangeUseCase,
            addTimerToQuestUseCase,
            completeTimeRangeUseCase,
            cancelQuestTimerUseCase,
            addPomodoroUseCase,
            removePomodoroUseCase,
            job
        )

    override val completedQuestPresenter: CompletedQuestPresenter
        get() = CompletedQuestPresenter(
            questRepository,
            playerRepository,
            splitDurationForPomodoroTimerUseCase,
            job
        )

    override val petMessagePresenter get() = PetMessagePresenter(listenForPlayerChangesUseCase, job)
    override val levelUpPresenter get() = LevelUpPresenter(listenForPlayerChangesUseCase, job)
    override val questCompletePresenter
        get() = QuestCompletePresenter(
            listenForPlayerChangesUseCase,
            job
        )
    override val ratePresenter get() = RatePresenter(listenForPlayerChangesUseCase, job)
}

interface StateStoreModule {
    val stateStore: StateStore<AppState>
}

class AndroidStateStoreModule : StateStoreModule, Injects<Module> {

    override val stateStore by required {
        StateStore(
            initialState = AppState(
                data = mapOf(
                    AppDataState::class.java to AppDataReducer.defaultState()
                )
            ),
            reducers = setOf(
                AppDataReducer,
                HomeReducer,
                CalendarReducer,
                ScheduleReducer,
                AgendaReducer,
                PetStoreReducer,
                AuthReducer,
                RepeatingQuestListReducer,
                ChallengeListForCategoryReducer
            ),
            sideEffects = setOf(
                LoadAllDataSideEffect(),
                AuthSideEffect(),
                AgendaSideEffect(),
                BuyPredefinedChallengeSideEffect(),
                ChangePetSideEffect(),
                BuyPetSideEffect()
            ),
            sideEffectExecutor = CoroutineSideEffectExecutor(job + CommonPool)
        )
    }
}

class Module(
    androidModule: AndroidModule,
    repositoryModule: RepositoryModule,
    useCaseModule: UseCaseModule,
    presenterModule: PresenterModule,
    stateStoreModule: StateStoreModule
) :
    AndroidModule by androidModule,
    RepositoryModule by repositoryModule,
    UseCaseModule by useCaseModule,
    PresenterModule by presenterModule,
    StateStoreModule by stateStoreModule,
    HasModules {
    override val modules =
        setOf(
            androidModule,
            repositoryModule,
            useCaseModule,
            presenterModule,
            stateStoreModule
        )
}