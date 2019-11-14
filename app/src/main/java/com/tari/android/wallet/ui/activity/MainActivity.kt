package com.tari.android.wallet.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    /**
     * Native methods.
     */
    private external fun privateKeyStringJNI(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Wallet library: try to create a byte vector.
        Logger.i("Try to create a private string through the wallet library.")
        // Simple native method call.
        val jniString = privateKeyStringJNI()
        Logger.i("Private key: %s", jniString)
        val wr = WeakReference<MainActivity>(this)
        sample_text.post { wr.get()?.setSampleText(jniString) }
    }

    private fun setSampleText(string: String) {
        sample_text.text = string
    }

    companion object {

        // Static initializer: used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
