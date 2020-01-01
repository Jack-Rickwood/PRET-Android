package com.example.pretandroid

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import java.util.*
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    // A global boolean which is set to true after the PRET-master.zip download has finished.
    var downloadCompleteFlag = false

    // Main code execution loop.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Requests permissions.
        requestPermissions()

        // Creates a variable storing the folder where pret will be downloaded, and checks if the folder is not already created.
        val pretFolder = File(this.getExternalFilesDir(null)!!.absolutePath + "/PRET Files")
        if (!pretFolder.isDirectory) {
            // Creates the dialog builder object.
            Log.d("FS: ", "${pretFolder.absolutePath} not already created.")
            val downloadPRETConfirm = AlertDialog.Builder(this)
            downloadPRETConfirm.setTitle(R.string.pret_download_confirm_title)
            downloadPRETConfirm.setMessage(R.string.pret_download_confirm_message)
            downloadPRETConfirm.setCancelable(false)

            // Following code is executed if the user presses the positive button.
            downloadPRETConfirm.setPositiveButton("Yes") { dialog, _ ->
                // Creates the folder where pret will be downloaded.
                dialog.cancel()
                pretFolder.mkdir()
                Log.d("FS: ", "${pretFolder.absolutePath} created.")

                // Creates the dialogs layout (LinearLayout) and changes some styling options.
                val downloadPRETLayout = LinearLayout(this)
                downloadPRETLayout.orientation = LinearLayout.HORIZONTAL
                val layoutPadding = 30
                downloadPRETLayout.setPadding(layoutPadding, layoutPadding, layoutPadding, layoutPadding)
                downloadPRETLayout.gravity = Gravity.CENTER
                var layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layoutParams.gravity = Gravity.CENTER
                downloadPRETLayout.layoutParams = layoutParams

                // Creates a progress bar object and changes some styling options.
                val progressBar = ProgressBar(this)
                progressBar.isIndeterminate = true
                progressBar.setPadding(0, 0, layoutPadding, 0)
                progressBar.layoutParams = layoutParams

                // Creates a text view object and changes some styling options.
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                layoutParams.gravity = Gravity.CENTER
                val loadingText = TextView(this)
                loadingText.text = getString(R.string.pret_download_title)
                loadingText.setTextColor(getColor(R.color.colorOptionsMenuText))
                loadingText.textSize = 20.toFloat()
                loadingText.layoutParams = layoutParams

                // Adds the progress bar and loading text objects to our layout.
                downloadPRETLayout.addView(progressBar)
                downloadPRETLayout.addView(loadingText)

                // Creates and displays the downloading dialog.
                val downloadPRETDialog = AlertDialog.Builder(this)
                downloadPRETDialog.setCancelable(false)
                downloadPRETDialog.setView(downloadPRETLayout)
                downloadPRETDialog.create()
                downloadPRETDialog.show()

                // Downloads pret to the folder we created earlier
                val repoURL = "https://github.com/RUB-NDS/PRET/archive/master.zip"
                fileDownload(repoURL)

                // val pretArchive = File(pretFolder.absolutePath + "PRET-master.zip")
            }

            // Closes the app if the negative button is pressed.
            downloadPRETConfirm.setNegativeButton("Exit") { _, _ ->
                exitProcess(0)
            }

            // Displays the layout.
            downloadPRETConfirm.create()
            downloadPRETConfirm.show()
        }

        /*
        val sharedPrefs = this.getSharedPreferences(getString(R.string.saved_printers_filename), Context.MODE_PRIVATE)
        val sharedPrefsId = sharedPrefs.all.map { it.key }
        for (i in sharedPrefsId) {
            val gson = Gson()
            val jsonText = sharedPrefs.getString(i, null)
            val printerInfo = gson.fromJson(jsonText, Array<String>::class.java)
            Log.w("Debug", i + ": " + printerInfo[0] + ", " + printerInfo[1])
        }
        */

        // Executes addPrinterDialog() if the floating action button (+) is pressed.
        fab.setOnClickListener { view ->
            addPrinterDialog(this)
        }
    }

    // Expands the options menu when the 3 dots are pressed.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Detects which options menu item was selected, and executes the appropriate code.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // If options menu item 'Clear Printers' selected, call clearPrinters()
            R.id.action_clear_printers -> {
                clearPrinters(this)
                Log.d("UI: ", "Option 'Clear Printers' selected.")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Requests permissions.
    private fun requestPermissions() {
        // If not already granted, requests for the 'WRITE_EXTERNAL_STORAGE' permission.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            Log.d("Permissions: ", "Requesting 'WRITE_EXTERNAL_STORAGE' permission.")
        }
        // If not already granted, requests for the 'INTERNET' permission.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 1)
            Log.d("Permissions: ", "Requesting 'INTERNET' permission.")
        }
    }

    // Exits the application if the user denies a permission.
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            1 ->
                // Checks if user has denied 'WRITE_EXTERNAL_STORAGE' permission.
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Log.d("[ERROR] Permissions: ", "Denied 'WRITE_EXTERNAL_STORAGE' permission, exiting...")
                    exitProcess(0)
                }
            2 ->
                // Checks if user has denied 'INTERNET' permission.
                if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                    Log.d("[ERROR] Permissions: ", "Denied 'INTERNET' permission, exiting...")
                    exitProcess(0)
                }
        }
    }

    // Starts downloading PRET-master.zip
    private fun fileDownload(fileURL: String) {
        // Creates the download request object.
        val downloadRequest = DownloadManager.Request(Uri.parse(fileURL))
        downloadRequest.setTitle("PRET-Master.zip")
        downloadRequest.setDescription("PRET Repo Archive")
        downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        // Sets the download location.
        downloadRequest.setDestinationInExternalFilesDir(applicationContext, "/PRET Files", "PRET-master.zip")

        // Creates the download manager object.
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

        // Starts the download and registers a BroadcastReceiver for when the download is complete.
        downloadManager?.enqueue(downloadRequest)
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        Log.d("Download: ", "PRET-master.zip download started.")
    }

    // BroadcastReceiver for when PRET-master.zip download is complete.
    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            downloadCompleteFlag = true
            Log.d("Download: ", "PRET-master.zip download complete.")
        }
    }

    // Shows a dialog to add printer information to shared preferences.
    private fun addPrinterDialog(context: Context) {
        // Creates the dialogs layout (LinearLayout) and changes some styling options.
        val dialogLayout = LinearLayout(context)
        dialogLayout.orientation = LinearLayout.VERTICAL
        dialogLayout.setPadding(
            resources.getDimensionPixelOffset(R.dimen.dp_19),
            0,
            resources.getDimensionPixelOffset(R.dimen.dp_19),
            0
        )

        // Creates the text input object for the name of the printer and adds it to the dialogs layout.
        val printerName = EditText(context)
        printerName.hint = "Printer Name"
        dialogLayout.addView(printerName)

        // Creates the text input object for the ip of the printer and adds it to the dialogs layout.
        val printerIP = EditText(context)
        printerIP.hint = "Printer IP"
        printerIP.inputType = InputType.TYPE_CLASS_PHONE
        dialogLayout.addView(printerIP)

        // Creates the dropdown object for the language of the printer and adds it to the dialogs layout.
        val printerLangDropdown = Spinner(context)
        val printerLangDropdownItems = arrayOf("PS", "PJL", "PCL")
        val printerLangDropdownAdapter = ArrayAdapter<String>(context, R.layout.lang_dropdown, R.id.lang_dropdown_text, printerLangDropdownItems)
        printerLangDropdown.adapter = printerLangDropdownAdapter
        dialogLayout.addView(printerLangDropdown)

        // Creates the dialog builder object.
        val addPrinterDialog = AlertDialog.Builder(context)
        addPrinterDialog.setTitle(R.string.printer_new_title)
        addPrinterDialog.setView(dialogLayout)
        addPrinterDialog.setMessage(R.string.printer_new_message)

        // Saves the printer information to shared preferences if the positive button is pressed.
        addPrinterDialog.setPositiveButton("Add") { dialog, _ ->
            if (printerName.text.toString() != "" && printerIP.text.toString() != "") {
                savePrinter(printerName.text.toString(), printerIP.text.toString(), printerLangDropdown.selectedItem.toString(), context)
                Log.d("UI: ", "Add printer dialog accepted.")
            } else {
                dialog.cancel()
                Toast.makeText(context, "Please enter a value for 'Printer Name' and 'Printer IP'", Toast.LENGTH_SHORT).show()
                Log.d("UI: ", "Add printer dialog canceled.")
            }
        }

        // Closes the dialog if the negative button is pressed.
        addPrinterDialog.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
            Log.d("UI: ", "Add printer dialog canceled.")
        }

        // Displays the layout.
        addPrinterDialog.create()
        addPrinterDialog.show()
        Log.d("UI: ", "Add printer dialog displayed.")
    }

    // Saves given printer information to shared preferences.
    private fun savePrinter(printerName: String, printerIP: String, printerLang: String, context: Context) {
        // Creates the shared preference editor objects.
        val sharedPrefsEditor = context.getSharedPreferences(getString(R.string.saved_printers_filename), Context.MODE_PRIVATE).edit()

        // Converts the array of printer information to json to allow storing in shared preferences (arrays cant be stored in shared preferences).
        val gson = Gson()
        val printerInfo = arrayOf(printerIP, printerLang)
        val printerInfoJson = gson.toJson(printerInfo)

        // Stores the json to shared preferences with the key being the name of the printer.
        sharedPrefsEditor.putString(printerName, printerInfoJson)
        sharedPrefsEditor.apply()
        Log.d("SharedPreferences: ", "Printer with info: ($printerName: $printerIP, $printerLang) saved to shared preferences.")
    }

    // Clears all printer information from shared preferences.
    private fun clearPrinters(context: Context) {
        val sharedPrefsEditor = context.getSharedPreferences(getString(R.string.saved_printers_filename), Context.MODE_PRIVATE).edit()
        sharedPrefsEditor.clear()
        sharedPrefsEditor.apply()
        Log.d("SharedPreferences: ", "Printer info was cleared.")
    }
}
