package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.addBaseNode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator.OnionAddressValidator
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator.PublicHexValidator
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator.Validator
import javax.inject.Inject

internal class AddCustomBaseNodeViewModel : CommonViewModel() {
    @Inject
    lateinit var baseNodeSharedRepository: BaseNodeSharedRepository

    @Inject
    lateinit var baseNodes: BaseNodes

    private val publicHexValidator = PublicHexValidator()
    private val onionValidator = OnionAddressValidator()

    //todo binding edit textItem
    private val _nameText = MutableLiveData<String>()
    val nameText: LiveData<String> = _nameText

    private val _onionAddressText = MutableLiveData<String>()
    val onionAddressText: LiveData<String> = _onionAddressText

    private val _publicHexText = MutableLiveData<String>()
    val publicHexText: LiveData<String> = _publicHexText

    private val _publicKeyHexValidationState = MutableLiveData<Validator.State>(Validator.State.Neutral)
    val publicKeyHexValidation: LiveData<Validator.State> = _publicKeyHexValidationState

    private val _addressValidationState = MutableLiveData<Validator.State>(Validator.State.Neutral)
    val addressValidationState: LiveData<Validator.State> = _addressValidationState

    init {
        component?.inject(this)
    }

    fun saveCustomNode() {
        validate()

        if (addressValidationState.value!! != Validator.State.Valid || addressValidationState.value!! != Validator.State.Valid) return

        val publicKeyHex = publicHexText.value.orEmpty()
        val address = onionAddressText.value.orEmpty()
        try {
            val baseNodeDto = BaseNodeDto(nameText.value.orEmpty(), publicKeyHex, address, true)
            baseNodeSharedRepository.addUserBaseNode(baseNodeDto)
            baseNodes.setBaseNode(baseNodeDto)
            _backPressed.postValue(Unit)
        } catch (e: Throwable) {
            baseNodes.setNextBaseNode()
            addBaseNodePeerFailed()
        }
    }

    private fun validate() {
        _addressValidationState.value = onionValidator.validate(_onionAddressText.value.orEmpty())
        _publicKeyHexValidationState.value = publicHexValidator.validate(publicHexText.value.orEmpty())
    }

    fun onNameChanged(text: String) = _nameText.postValue(text)

    fun onPublicKeyHexChanged(text: String) = _publicHexText.postValue(text)

    fun onAddressChanged(text: String) = _onionAddressText.postValue(text)

    private fun addBaseNodePeerFailed() {
        _errorDialog.postValue(
            ErrorDialogArgs(
                resourceManager.getString(R.string.common_error_title),
                resourceManager.getString(R.string.debug_edit_base_node_failed)
            )
        )
    }
}