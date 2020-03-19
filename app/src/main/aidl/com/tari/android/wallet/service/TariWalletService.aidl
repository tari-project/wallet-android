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

    String getPublicKeyHexString(out WalletError error);

    BalanceInfo getBalanceInfo(out WalletError error);

    List<Contact> getContacts(out WalletError error);

    List<User> getRecentTxUsers(int maxCount, out WalletError error);

    List<CompletedTx> getCompletedTxs(out WalletError error);
    CompletedTx getCompletedTxById(in TxId id, out WalletError error);

    List<PendingInboundTx> getPendingInboundTxs(out WalletError error);
    PendingInboundTx getPendingInboundTxById(in TxId id, out WalletError error);

    List<PendingOutboundTx> getPendingOutboundTxs(out WalletError error);
    PendingOutboundTx getPendingOutboundTxById(in TxId id, out WalletError error);

    boolean sendTari(
        in User contact,
        in MicroTari amount,
        in MicroTari fee,
        String message,
        out WalletError error
    );

    void requestTestnetTari(out WalletError error);

    void updateContactAlias(in PublicKey contactPublicKey, in String contactAlias, out WalletError error);

    /**
    * Two functions below to get the public key from emoji id and public key hex string
    * do not accept out error parameters - they will just return null if a public key
    * cannot be constructed from input parameters.
    */
    PublicKey getPublicKeyFromEmojiId(in String emojiId);
    PublicKey getPublicKeyFromHexString(in String publicKeyHex);

}