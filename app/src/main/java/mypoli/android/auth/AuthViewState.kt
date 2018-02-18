package mypoli.android.auth

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import mypoli.android.Constants
import mypoli.android.R
import mypoli.android.common.AppState
import mypoli.android.common.BaseViewStateReducer
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 2/5/18.
 */
sealed class AuthAction : Action {
    object Load : AuthAction()
    data class LoadSignUp(val isGuest: Boolean) : AuthAction()
    data class CompleteUserAuth(val user: FirebaseUser, val username: String) : AuthAction()
    data class SignUp(val username: String, val provider: AuthViewState.Provider) : AuthAction()
    data class Login(val provider: AuthViewState.Provider) : AuthAction()
    data class UsernameValidationFailed(val error: AuthViewState.ValidationError) : AuthAction()
    data class StartSignUp(val provider: AuthViewState.Provider) : AuthAction()
    object AccountsLinked : AuthAction()
    object PlayerCreated : AuthAction()
    object PlayerLoggedIn : AuthAction()
    object SwitchViewType : AuthAction()
    object DeleteAccount : AuthAction()
    object SignOutAccount : AuthAction()
    object GuestPlayerLoggedIn : AuthAction()
}

object AuthReducer : BaseViewStateReducer<AuthViewState>() {

    override val stateKey = key<AuthViewState>()

    override fun reduce(state: AppState, subState: AuthViewState, action: Action) =
        when (action) {
            is AuthAction.LoadSignUp -> {
                subState.copy(
                    type = AuthViewState.StateType.SHOW_SIGN_UP,
                    isGuest = action.isGuest
                )
            }

            is AuthAction.StartSignUp -> {
                val type = when (action.provider) {
                    AuthViewState.Provider.GOOGLE ->
                        AuthViewState.StateType.GOOGLE_AUTH_STARTED

                    AuthViewState.Provider.FACEBOOK ->
                        AuthViewState.StateType.FACEBOOK_AUTH_STARTED

                    AuthViewState.Provider.GUEST ->
                        AuthViewState.StateType.GUEST_AUTH_STARTED
                }

                subState.copy(type = type)
            }

            is AuthAction.UsernameValidationFailed -> {
                subState.copy(
                    type = AuthViewState.StateType.USERNAME_VALIDATION_ERROR,
                    usernameValidationError = action.error
                )
            }

            is AuthAction.Login -> {
                val type = when (action.provider) {
                    AuthViewState.Provider.GOOGLE ->
                        AuthViewState.StateType.GOOGLE_AUTH_STARTED

                    AuthViewState.Provider.FACEBOOK ->
                        AuthViewState.StateType.FACEBOOK_AUTH_STARTED

                    AuthViewState.Provider.GUEST ->
                        throw IllegalArgumentException("Guest can't log in")
                }
                subState.copy(
                    type = type
                )
            }

            AuthAction.PlayerCreated -> {
                subState.copy(
                    type = AuthViewState.StateType.PLAYER_CREATED
                )
            }

            AuthAction.PlayerLoggedIn -> {
                subState.copy(
                    type = AuthViewState.StateType.PLAYER_LOGGED_IN
                )
            }

            AuthAction.GuestPlayerLoggedIn -> {
                subState.copy(
                    type = AuthViewState.StateType.GUEST_PLAYER_LOGGED_IN
                )
            }

            AuthAction.SwitchViewType -> {
                val isLogin = !subState.isLogin

                val type = if (isLogin)
                    AuthViewState.StateType.SHOW_LOGIN
                else
                    AuthViewState.StateType.SHOW_SIGN_UP

                subState.copy(
                    type = type,
                    isLogin = isLogin
                )
            }

            AuthAction.AccountsLinked -> {
                subState.copy(
                    type = AuthViewState.StateType.ACCOUNTS_LINKED
                )
            }

            AuthAction.DeleteAccount -> {
                subState.copy(
                    type = AuthViewState.StateType.DELETE_ACCOUNT
                )
            }

            AuthAction.SignOutAccount -> {
                subState.copy(
                    type = AuthViewState.StateType.SIGN_OUT_ACCOUNT
                )
            }

            else -> subState

        }

    override fun defaultState() =
        AuthViewState(
            type = AuthViewState.StateType.LOADING,
            usernameValidationError = null,
            isLogin = false,
            isGuest = false
        )

}

data class AuthViewState(
    val type: AuthViewState.StateType,
    val usernameValidationError: ValidationError?,
    val isLogin: Boolean,
    val isGuest: Boolean
) : ViewState {
    enum class StateType {
        IDLE,
        LOADING,
        SHOW_SIGN_UP,
        SHOW_LOGIN,
        GOOGLE_AUTH_STARTED,
        FACEBOOK_AUTH_STARTED,
        GUEST_AUTH_STARTED,
        START_GOOGLE_LOGIN,
        START_FACEBOOK_LOGIN,
        USERNAME_VALIDATION_ERROR,
        PLAYER_CREATED,
        PLAYER_LOGGED_IN,
        GUEST_PLAYER_LOGGED_IN,
        ACCOUNTS_LINKED,
        DELETE_ACCOUNT,
        SIGN_OUT_ACCOUNT
    }

    enum class ValidationError {
        EMPTY_USERNAME,
        EXISTING_USERNAME,
        INVALID_FORMAT,
        INVALID_LENGTH
    }

    enum class Provider {
        FACEBOOK, GOOGLE, GUEST
    }
}

fun AuthViewState.usernameErrorMessage(context: Context) =
    usernameValidationError?.let {
        when (it) {
            AuthViewState.ValidationError.EMPTY_USERNAME -> context.getString(R.string.username_is_empty)
            AuthViewState.ValidationError.EXISTING_USERNAME -> context.getString(R.string.username_is_taken)
            AuthViewState.ValidationError.INVALID_FORMAT -> context.getString(R.string.username_wrong_format)
            AuthViewState.ValidationError.INVALID_LENGTH -> context.getString(
                R.string.username_wrong_length,
                Constants.USERNAME_MIN_LENGTH,
                Constants.USERNAME_MAX_LENGTH
            )
        }
    }