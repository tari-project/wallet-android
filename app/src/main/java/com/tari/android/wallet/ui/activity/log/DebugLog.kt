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
package com.tari.android.wallet.ui.activity.log

import com.tari.android.wallet.ui.activity.BaseActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.EditText
import butterknife.BindView
import butterknife.OnClick
import com.tari.android.wallet.R
import java.io.File
import java.io.InputStream


/**
 * Debug screen activity.
 *
 * @author The Tari Development Team
 */
class DebugLogActivity() : BaseActivity() {

    override val contentViewId = R.layout.debug_log

    @OnClick(R.id.debug_log_btn_back)
    fun onBackButtonPressed(view: View) {
        super.onBackPressed()
    }

    @BindView(R.id.debug_log_multiline_edit)
    lateinit var multilineEdit : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        multilineEdit.text.clear()
        multilineEdit.setHorizontallyScrolling(true)
        multilineEdit.movementMethod = ScrollingMovementMethod()
        val log = intent.getStringExtra("log")
        var lineList = mutableListOf<String>()
        if (File(log).exists()) {
            val inputStream: InputStream = File(log).inputStream()
            inputStream.bufferedReader().useLines { lines -> lines.forEach { lineList.add(it)} }
        } else
        {
            lineList.add("No log available")
        }
        lineList.forEach{multilineEdit.append(it+"\n")}
    }
}