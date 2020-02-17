/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.service;

// import model classes
import com.tari.android.wallet.model.Model;
import com.tari.android.wallet.service.TariWalletServiceListener;

interface TariWalletService {

    /**
    * Registers new wallet listener.
    * Registered listener will be unregistered on death.
    */
    boolean registerListener(TariWalletServiceListener listener);

    /**
    * Unregisters wallet listener.
    */
    boolean unregisterListener(TariWalletServiceListener listener);

    String getPublicKeyHexString();

    String getLogFilePath();

    BalanceInfo getBalanceInfo();

    List<Contact> getContacts();

    List<User> getRecentTxUsers(int maxCount);

    List<CompletedTx> getCompletedTxs();

    CompletedTx getCompletedTxById(in TxId id);

    List<PendingInboundTx> getPendingInboundTxs();
    PendingInboundTx getPendingInboundTxById(in TxId id);

    List<PendingOutboundTx> getPendingOutboundTxs();
    PendingOutboundTx getPendingOutboundTxById(in TxId id);

    PublicKey getPublicKeyForEmojiId(String emojiId);

    boolean sendTari(
        in User contact,
        in MicroTari amount,
        in MicroTari fee,
        String message
    );

    void requestTestnetTari();

}