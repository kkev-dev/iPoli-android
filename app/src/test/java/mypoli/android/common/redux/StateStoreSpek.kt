package mypoli.android.common.redux

import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should be equal to`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 01/21/2018.
 */
object StateStoreSpek : Spek({

    describe("StateStore") {

        class TestState(data: Map<Class<*>, State>) : CompositeState<TestState>(data) {
            override fun createWithData(stateData: Map<Class<*>, State>): TestState {
                return TestState(stateData)
            }

        }

        class TestAction : Action

        class TestSideEffectExecutor : SideEffectExecutor<TestState> {
            override fun execute(
                sideEffect: SideEffect<TestState>,
                action: Action,
                state: TestState,
                dispatcher: Dispatcher
            ) {
                runBlocking {
                    sideEffect.execute(action, state, dispatcher)
                }

            }
        }

        class StopMiddleware : MiddleWare<TestState> {
            override fun execute(
                state: TestState,
                dispatcher: Dispatcher,
                action: Action
            ) = MiddleWare.Result.Stop
        }

        var executeCount = 0

        beforeEachTest {
            executeCount = 0
        }

        data class SubState(val dummy: String = "") : State

        val testReducer = object : Reducer<TestState, SubState> {

            override val stateKey: Class<SubState>
                get() = SubState::class.java

            override fun reduce(state: TestState, subState: SubState, action: Action): SubState {
                executeCount++
                return subState
            }

            override fun defaultState() = SubState()
        }

        fun createStore(
            middleware: Set<MiddleWare<TestState>> = setOf(),
            sideEffects: Set<SideEffect<TestState>> = setOf()
        ) =
            StateStore(
                TestState(mapOf(SubState::class.java to SubState())),
                setOf(testReducer),
                sideEffects = sideEffects,
                sideEffectExecutor = TestSideEffectExecutor(),
                middleware = middleware
            )

        it("should call the reducer with no middleware") {
            createStore().dispatch(TestAction())
            executeCount.`should be equal to`(1)
        }

        it("should not call reducer with stopping middleware") {
            createStore(setOf(StopMiddleware())).dispatch(TestAction())

            executeCount.`should be equal to`(0)
        }

        it("should call subscriber on subscribe") {

            var stateChangeCount = 0

            val subscriber = object : StateStore.StateChangeSubscriber<TestState> {
                override fun onStateChanged(newState: TestState) {
                    stateChangeCount++
                }
            }

            createStore().subscribe(subscriber)

            stateChangeCount.`should be equal to`(1)
        }

        it("should call subscriber on dispatch") {

            var stateChangeCount = 0

            val subscriber = object : StateStore.StateChangeSubscriber<TestState> {
                override fun onStateChanged(newState: TestState) {
                    stateChangeCount++
                }
            }

            val store = createStore()
            store.subscribe(subscriber)
            stateChangeCount = 0
            store.dispatch(TestAction())

            stateChangeCount.`should be equal to`(1)
        }

        it("should call SideEffect") {


            class SideEffectAction : Action

            var sideEffectCalls = 0

            val sideEffect = object : SideEffect<TestState> {
                override suspend fun execute(
                    action: Action,
                    state: TestState,
                    dispatcher: Dispatcher
                ) {
                    sideEffectCalls++
                }

                override fun canHandle(action: Action) =
                    action is SideEffectAction

            }

            val store = createStore(sideEffects = setOf(sideEffect))

            store.dispatch(SideEffectAction())

            sideEffectCalls.`should be equal to`(1)
        }

        it("should not call SideEffect when it can't handle Action") {

            class SideEffectAction : Action

            var sideEffectCalls = 0

            val sideEffect = object : SideEffect<TestState> {
                override suspend fun execute(
                    action: Action,
                    state: TestState,
                    dispatcher: Dispatcher
                ) {
                    sideEffectCalls++
                }

                override fun canHandle(action: Action) = false

            }

            val store = createStore(sideEffects = setOf(sideEffect))

            store.dispatch(SideEffectAction())

            sideEffectCalls.`should be equal to`(0)
        }

    }
})