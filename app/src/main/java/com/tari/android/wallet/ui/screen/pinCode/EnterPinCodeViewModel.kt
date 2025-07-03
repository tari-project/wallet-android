package com.tari.android.wallet.ui.screen.pinCode

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.data.sharedPrefs.security.LoginAttemptDto
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.util.extension.addTo
import java.time.LocalDateTime
import java.time.OffsetDateTime

class EnterPinCodeViewModel : CommonViewModel() {

    val behavior = MutableLiveData<PinCodeScreenBehavior>()

    val stashedPin = MutableLiveData<String>()

    val subtitleText = behavior.map {
        val id = when (it) {
            PinCodeScreenBehavior.Create -> R.string.pin_code_subtitle_create
            PinCodeScreenBehavior.CreateConfirm -> R.string.pin_code_subtitle_create_confirm
            PinCodeScreenBehavior.ChangeNew -> R.string.pin_code_subtitle_change_new
            PinCodeScreenBehavior.ChangeNewConfirm -> R.string.pin_code_subtitle_change_new_confirm
            PinCodeScreenBehavior.Auth -> R.string.pin_code_subtitle_auth
            PinCodeScreenBehavior.FeatureAuth -> R.string.pin_code_subtitle_feature_auth
            else -> R.string.common_continue
        }
        resourceManager.getString(id)
    }

    val toolbarIsVisible = behavior.map {
        it != PinCodeScreenBehavior.Auth && it != PinCodeScreenBehavior.FeatureAuth
    }

    val buttonIsVisible = behavior.map {
        false
    }

    val currentNums = MutableLiveData("")

    val errorMessage = MutableLiveData("")

    val buttonTitle = behavior.map {
        val id = when (it) {
            PinCodeScreenBehavior.Create -> R.string.common_continue
            PinCodeScreenBehavior.CreateConfirm -> R.string.common_confirm
            PinCodeScreenBehavior.ChangeNew -> R.string.common_continue
            PinCodeScreenBehavior.ChangeNewConfirm -> R.string.common_confirm
            PinCodeScreenBehavior.Auth -> R.string.common_confirm
            else -> R.string.common_continue
        }
        resourceManager.getString(id)
    }

    val successfullyAuth = SingleLiveEvent<Unit>()

    val nextEnterTime = MutableLiveData<LocalDateTime>()

    init {
        component.inject(this)

        doFraudLogic()
        securityPrefRepository.updateNotifier.subscribe(
            /* onNext = */ { doFraudLogic() },
            /* onError = */ { logger.d("Error updating fraud logic", it) },
        ).addTo(compositeDisposable)
    }

    private fun doFraudLogic() {
        val attempts = securityPrefRepository.attempts
        val lastTimeInMills = attempts.lastOrNull()?.timeInMills
        val localDateTime = LocalDateTime.ofEpochSecond((lastTimeInMills ?: 0) / 1000, 0, OffsetDateTime.now().offset)
        val nextDateTime = when (attempts.size) {
            in 0..3 -> LocalDateTime.now().minusMinutes(1)
            4 -> localDateTime.plusMinutes(1)
            5 -> localDateTime.plusMinutes(5)
            6 -> localDateTime.plusMinutes(60)
            else -> localDateTime.plusMinutes(60 * 5)
        }
        nextEnterTime.postValue(nextDateTime)
    }

    fun init(behavior: PinCodeScreenBehavior, stashedPin: String? = null) {
        this.behavior.value = behavior
        this.stashedPin.value = stashedPin
    }

    fun addNum(num: String) {
        if ((currentNums.value?.length ?: 0) >= 6) {
            if (errorMessage.value.orEmpty().isNotEmpty()) {
                errorMessage.value = ""
                currentNums.value = num
            }
            return
        }
        currentNums.value += num
        if (currentNums.value.orEmpty().length == 6) {
            doMainAction()
        }
    }

    fun removeLast() {
        currentNums.value = currentNums.value?.dropLast(1)
    }

    fun doMainAction() {
        when (behavior.value) {
            PinCodeScreenBehavior.Create -> createPinCode()
            PinCodeScreenBehavior.CreateConfirm -> createPinCodeConfirm()
            PinCodeScreenBehavior.ChangeNew -> changeNewPinCode()
            PinCodeScreenBehavior.ChangeNewConfirm -> changeNewPinCodeConfirm()
            PinCodeScreenBehavior.Auth -> authPinCode()
            PinCodeScreenBehavior.FeatureAuth -> authPinCode()
            null -> Unit
        }
    }

    private fun createPinCode() {
        tariNavigator.navigate(Navigation.EnterPinCode(PinCodeScreenBehavior.CreateConfirm, currentNums.value))
    }

    private fun createPinCodeConfirm() {
        if (currentNums.value != stashedPin.value) {
            onBackPressed()
            return
        }
        securityPrefRepository.pinCode = currentNums.value.orEmpty()
        tariNavigator.navigate(Navigation.Auth.BackAfterAuth)
    }

    private fun changeNewPinCode() {
        tariNavigator.navigate(Navigation.EnterPinCode(PinCodeScreenBehavior.ChangeNewConfirm, currentNums.value))
    }

    private fun changeNewPinCodeConfirm() {
        if (currentNums.value != stashedPin.value) {
            onBackPressed()
            return
        }
        securityPrefRepository.pinCode = currentNums.value.orEmpty()
        tariNavigator.navigate(Navigation.Auth.BackAfterAuth)
    }

    private fun authPinCode() {
        val isSuccessfully = currentNums.value == securityPrefRepository.pinCode

        securityPrefRepository.saveAttempt(LoginAttemptDto(System.currentTimeMillis(), isSuccessfully))

        if (!isSuccessfully) {
            errorMessage.value = resourceManager.getString(R.string.pin_code_error_message)
            return
        }
        successfullyAuth.postValue(Unit)
    }
}