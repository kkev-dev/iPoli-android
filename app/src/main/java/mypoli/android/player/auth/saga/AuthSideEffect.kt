package mypoli.android.player.auth.saga

import android.annotation.SuppressLint
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import mypoli.android.Constants
import mypoli.android.auth.AuthAction
import mypoli.android.auth.AuthState
import mypoli.android.common.AppSideEffect
import mypoli.android.common.AppState
import mypoli.android.common.LoadDataAction
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.Dispatcher
import mypoli.android.player.AuthProvider
import mypoli.android.player.Player
import mypoli.android.player.persistence.PlayerRepository
import space.traversal.kapsule.required
import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 02/07/2018.
 */
class AuthSideEffect : AppSideEffect {

    private val playerRepository by required { playerRepository }
    private val sharedPreferences by required { sharedPreferences }
    private val petStatsChangeScheduler by required { lowerPetStatsScheduler }

    override fun canHandle(action: Action) = action is AuthAction

    override suspend fun doExecute(action: Action, state: AppState, dispatcher: Dispatcher) {

        when (action) {
            AuthAction.Load -> {
                dispatcher.dispatch(AuthAction.LoadSignUp(playerRepository.hasPlayer()))
            }

            is AuthAction.CompleteUserAuth -> {
                val user = action.user
                val username = action.username

                val metadata = user.metadata
                val isNewUser = metadata == null || metadata.creationTimestamp == metadata.lastSignInTimestamp

                if (state.authState.isLogin) {
                    when {
                        !isNewUser && playerRepository.hasPlayer() ->{
                            //TODO: delete anonymous account
                            val anonymousPlayerId =
                                sharedPreferences.getString(Constants.KEY_PLAYER_ID, null)
                            savePlayerId(user)
                            dispatcher.dispatch(LoadDataAction.ChangePlayer(anonymousPlayerId))
                            dispatcher.dispatch(AuthAction.GuestPlayerLoggedIn)
                        }
                        playerRepository.hasPlayer() -> {
                            mergeFromAnonymous(dispatcher)
                        }
                        isNewUser -> {
                            handleMissingRegisteredUser(dispatcher)
                        }
                        else -> loginExistingPlayer(user, dispatcher)
                    }

                } else {
                    when {
                        playerRepository.hasPlayer() -> {
                            updatePlayerAuthProvider(user)
                            playerRepository.addUsername(username)
                            dispatcher.dispatch(AuthAction.AccountsLinked)
                        }
                        isNewUser -> {
                            createNewPlayer(user, username, dispatcher)
                        }
                        else -> loginExistingPlayer(user, dispatcher)
                    }
                }
            }

            is AuthAction.SignUp -> {
                val username = action.username

                when (action.provider) {
                    AuthState.Provider.FACEBOOK -> {
                        val usernameValidationError =
                            findUsernameValidationError(username, playerRepository)
                        if (usernameValidationError != null) {
                            dispatcher.dispatch(
                                AuthAction.UsernameValidationFailed(
                                    usernameValidationError
                                )
                            )
                            return
                        }

                        dispatcher.dispatch(AuthAction.StartSignUp(AuthState.Provider.FACEBOOK))
                    }

                    AuthState.Provider.GOOGLE -> {
                        val usernameValidationError =
                            findUsernameValidationError(username, playerRepository)
                        if (usernameValidationError != null) {
                            dispatcher.dispatch(
                                AuthAction.UsernameValidationFailed(
                                    usernameValidationError
                                )
                            )
                            return
                        }

                        dispatcher.dispatch(AuthAction.StartSignUp(AuthState.Provider.GOOGLE))
                    }

                    AuthState.Provider.GUEST -> {
                        dispatcher.dispatch(AuthAction.StartSignUp(AuthState.Provider.GUEST))
                    }
                }
            }
        }
    }

    private fun handleMissingRegisteredUser(dispatcher: Dispatcher) {
        dispatcher.dispatch(AuthAction.DeleteAccount)
    }

    private fun mergeFromAnonymous(dispatcher: Dispatcher) {
        dispatcher.dispatch(AuthAction.SignOutAccount)
    }

    private fun updatePlayerAuthProvider(
        user: FirebaseUser
    ) {
        val authProviders =
            user.providerData.filter { it.providerId != FirebaseAuthProvider.PROVIDER_ID }
        require(authProviders.size == 1)
        val authProvider = authProviders.first()

        val playerAuthProvider = if (authProvider.providerId == FacebookAuthProvider.PROVIDER_ID) {
            AuthProvider.Facebook(
                authProvider.uid,
                displayName = user.displayName!!,
                email = user.email!!,
                imageUrl = user.photoUrl!!
            )
        } else if (authProvider.providerId == GoogleAuthProvider.PROVIDER_ID) {
            AuthProvider.Google(
                authProvider.uid,
                displayName = user.displayName!!,
                email = user.email!!,
                imageUrl = user.photoUrl!!
            )
        } else {
            throw IllegalStateException("Unknown Auth provider")
        }

        val player = playerRepository.find()
        playerRepository.save(
            player!!.copy(
                authProvider = playerAuthProvider
            )
        )
    }

    private fun createNewPlayer(
        user: FirebaseUser,
        username: String,
        dispatcher: Dispatcher
    ) {

        val authProvider = if (user.providerData.size == 1) {
            user.providerData.first()
        } else {
            val authProviders =
                user.providerData.filter { it.providerId != FirebaseAuthProvider.PROVIDER_ID }
            require(authProviders.size == 1)
            authProviders.first()
        }

        val playerAuthProvider = when {
            authProvider.providerId == FacebookAuthProvider.PROVIDER_ID -> AuthProvider.Facebook(
                authProvider.uid,
                displayName = user.displayName!!,
                email = user.email!!,
                imageUrl = user.photoUrl!!
            )
            authProvider.providerId == GoogleAuthProvider.PROVIDER_ID -> AuthProvider.Google(
                authProvider.uid,
                displayName = user.displayName!!,
                email = user.email!!,
                imageUrl = user.photoUrl!!
            )
            authProvider.providerId == FirebaseAuthProvider.PROVIDER_ID -> AuthProvider.Guest(
                authProvider.uid
            )
            else -> throw IllegalStateException("Unknown Auth provider")
        }

        val player = Player(
            authProvider = playerAuthProvider,
            username = if (playerAuthProvider !is AuthProvider.Guest) username else "",
            displayName = if (user.displayName != null) user.displayName!! else "",
            schemaVersion = Constants.SCHEMA_VERSION
        )

        playerRepository.create(player, user.uid)
        savePlayerId(user)

        if (authProvider.providerId != FirebaseAuthProvider.PROVIDER_ID) {
            playerRepository.addUsername(username)
        }

        prepareAppStart(dispatcher)
        dispatcher.dispatch(AuthAction.PlayerCreated)

    }

    private fun loginExistingPlayer(user: FirebaseUser, dispatcher: Dispatcher) {
        savePlayerId(user)
        prepareAppStart(dispatcher)
        dispatcher.dispatch(AuthAction.PlayerLoggedIn)
    }

    @SuppressLint("ApplySharedPref")
    private fun savePlayerId(user: FirebaseUser) {
        sharedPreferences.edit().putString(Constants.KEY_PLAYER_ID, user.uid).commit()
    }

    private fun prepareAppStart(dispatcher: Dispatcher) {
        dispatcher.dispatch(LoadDataAction.All)
        petStatsChangeScheduler.schedule()
    }

    private fun findUsernameValidationError(
        username: String,
        playerRepository: PlayerRepository
    ): AuthState.ValidationError? {

        if (username.trim().isEmpty()) {
            return AuthState.ValidationError.EMPTY_USERNAME
        }

        if (username.length < Constants.USERNAME_MIN_LENGTH || username.length > Constants.USERNAME_MAX_LENGTH) {
            return AuthState.ValidationError.INVALID_LENGTH
        }

        val asciiEncoder = Charset.forName("US-ASCII").newEncoder()
        if (!asciiEncoder.canEncode(username)) {
            return AuthState.ValidationError.INVALID_FORMAT
        }

        val p = Pattern.compile("^\\w+$")
        if (!p.matcher(username).matches()) {
            return AuthState.ValidationError.INVALID_FORMAT
        }

        if (!playerRepository.isUsernameAvailable(username)) {
            return AuthState.ValidationError.EXISTING_USERNAME
        }

        return null
    }
}