package br.ufpe.cin.android.podcast

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import java.util.prefs.PreferenceChangeEvent
import java.util.prefs.PreferenceChangeListener


class PrefsMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Carrega um layout que contem um fragmento
        setContentView(R.layout.activity_prefs)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, PreferenceFragment())
            .commit()
    }

    // Fragmento que mostra a preference com username
    class PreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Carrega preferences a partir de um XML
            addPreferencesFromResource(R.xml.fragment_settings)
        }


        private var mListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
        private var refreshPreference: Preference? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)


            // pega a Preference especifica do username
            refreshPreference = preferenceManager.findPreference(REFRESH)

            // Define um listener para atualizar descricao ao modificar preferences
            mListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                refreshPreference!!.summary = ""
            }
            // Pega objeto SharedPreferences gerenciado pelo PreferenceManager para este Fragmento
            val prefs = preferenceManager
                    .sharedPreferences
            // Registra listener no objeto SharedPreferences
            prefs.registerOnSharedPreferenceChangeListener(mListener)
            // Invoca callback manualmente para exibir username atual
            mListener!!.onSharedPreferenceChanged(prefs, REFRESH)

        }

        companion object {
            protected val TAG = "UserPrefsFragment"
            val REFRESH = "refreshrate"
        }
    }
}