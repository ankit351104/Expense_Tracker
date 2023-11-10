package com.example.spends
import android.annotation.SuppressLint
import android.util.Log
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.layout.RowScopeInstance.align
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Date
import java.util.jar.Manifest

class MainActivity : ComponentActivity() {
    private val requestCode = 123
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Request the SMS permission before accessing SMS messages
            requestSmsPermission()
            SpendsTrackUI()
        }
    }

    private @Composable
    fun ExposedDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, function: () -> Unit) {

    }

    //    @OptIn(ExperimentalMaterial3Api::class)
//    @Composable
//    fun SpendsTrackUI() {
////    val context = LocalContext.current
//        val period = arrayOf("Today", "This Week", "This Month", "Past 3 Months", "Past 6 Months")
//        var expanded by remember { mutableStateOf(false) }
//        var selectedItemIndex by remember { mutableStateOf(0) }
//        val context = LocalContext.current
//        var messages by remember { mutableStateOf(listOf<String>()) }
//        ExposedDropdownMenuBox(
//            expanded = expanded,
//            onExpandedChange = { expanded = !expanded },
//            modifier = Modifier.padding(16.dp),
//        ) {
//            TextField(
//                value = period[selectedItemIndex],
//                onValueChange = {},
//                readOnly = true,
//                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
//                modifier = Modifier.menuAnchor()
//            )
//
//            ExposedDropdownMenu(
//                expanded = expanded,
//                onDismissRequest = { expanded = false }
//            ) {
//                period.forEachIndexed { index, item ->
//                    DropdownMenuItem(
//                        text = {
//                            Text(
//                                text = item,
//                                fontWeight = if (index == selectedItemIndex) FontWeight.Bold else null
//                            )
//                        },
//                        onClick = {
//                            selectedItemIndex = index
//                            expanded = false
//                            Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
//                            readSMSMessagesByPeriod(context,index)
//                            messages.forEach { message -> Log.d("SMSReader", message) }
//                        }
//                    )
//                }
//            }
//        }
//    }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendsTrackUI() {
    val period = arrayOf("Today", "This Week", "This Month", "Past 3 Months", "Past 6 Months", "Past 1 Year")
    var expanded by remember { mutableStateOf(false) }
    var selectedItemIndex by remember { mutableStateOf(0) }
    val context = LocalContext.current
    var messages by remember { mutableStateOf(listOf<String>()) }
    var totalSpend by remember { mutableStateOf("") } // Initialize totalSpend

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.padding(16.dp),
    ) {
        TextField(
            value = period[selectedItemIndex],
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            period.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            fontWeight = if (index == selectedItemIndex) FontWeight.Bold else null
                        )
                    },
                    onClick = {
                        selectedItemIndex = index
                        expanded = false
                        Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
                        val spend = readSMSMessagesByPeriod(context, index)
                        totalSpend = spend // Update totalSpend
                        messages.forEach { message -> Log.d("SMSReader", message) }
                    }
                )
            }
        }
    }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = "Total Spend: $totalSpend",
                textAlign = TextAlign.Center, // make text center horizontal
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.Bold,
            )
        }


    }

    private fun requestSmsPermission() {
        val permission = android.Manifest.permission.READ_SMS
        val granted = PackageManager.PERMISSION_GRANTED
        if (ContextCompat.checkSelfPermission(this, permission) != granted) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        }
    }
}


@SuppressLint("Range")
//@Composable
fun readSMSMessagesByPeriod(context: Context, selectedItemIndex: Int): String {
    val contentResolver: ContentResolver = context.contentResolver
    val currentDate = Date()
    val calendar = Calendar.getInstance()
    val uri: Uri = Telephony.Sms.CONTENT_URI
    val messages = mutableListOf<String>()
    val keywords = setOf("debited", "withdrawn", "online purchase","sent", "debit")
//    var totalSpend = 0.0
//    val decimalValues = mutableListOf<Double>()
    val decimalValues = mutableSetOf<Double>()
    val decimalRegex = Regex("([-+]?(\\d+\\.\\d{1,2}|\\d*\\.\\d{1,2}))")
    val rsPattern = Regex("Rs\\.\\s*(\\d+(\\.\\d+)?)")
//    val decimalRegex = Regex("([-+]?(\\d+\\.\\d{1,2}|\\d*\\.\\d{1,2})|Rs\\.\\s*\\d+(\\.\\d+)?)")

    val cursor = when (selectedItemIndex) {
        0 -> {
            // Today
            calendar.time = currentDate
            val midnight = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val selection = "${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(midnight.timeInMillis.toString())

            contentResolver.query(uri, null, selection, selectionArgs, null)
        }

        1 -> {
            // This Week
            calendar.time = currentDate
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            val startOfWeek = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val selection = "${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(startOfWeek.timeInMillis.toString())

            contentResolver.query(uri, null, selection, selectionArgs, null)
        }

        2 -> {
            // This Month
            calendar.time = currentDate
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val selection = "${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(startOfMonth.timeInMillis.toString())

            contentResolver.query(uri, null, selection, selectionArgs, null)
        }

        3 -> {
            // Past 3 Months
            calendar.time = currentDate
            calendar.add(Calendar.MONTH, -3)
            val startOf3MonthsAgo = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val selection = "${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(startOf3MonthsAgo.timeInMillis.toString())

            contentResolver.query(uri, null, selection, selectionArgs, null)
        }

        4 -> {
            // Past 6 Months
            calendar.time = currentDate
            calendar.add(Calendar.MONTH, -6)
            val startOf6MonthsAgo = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val selection = "${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(startOf6MonthsAgo.timeInMillis.toString())

            contentResolver.query(uri, null, selection, selectionArgs, null)
        }
        5 -> {
            // Past 6 Months
            calendar.time = currentDate
            calendar.add(Calendar.MONTH, -12)
            val startOf12MonthsAgo = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val selection = "${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(startOf12MonthsAgo.timeInMillis.toString())

            contentResolver.query(uri, null, selection, selectionArgs, null)
        }

        else -> null
    }

    // Process the retrieved SMS messages for the selected time period
    cursor?.use { cursor ->
        while (cursor.moveToNext()) {
            val address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS))
            val message = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY))
            // Add the SMS message to the list
            // Check if the message contains any of the keywords
            if (keywords.any { keyword -> message.contains(keyword, ignoreCase = true) }) {
                // Add the SMS message to the list
//                messages.add("Address: $address\nMessage: $message")
//                val matches = Regex("([-+]?(\\d+\\.?\\d*|\\d*\\.?\\d+))").findAll(message)
//                for (match in matches) {
//                    val value = match.value.toDoubleOrNull()
//                    if (value != null) {
//                        totalSpend += value
//                    }
//                }
                if (keywords.any { keyword -> message.contains(keyword, ignoreCase = true) }) {
                    // Extract one or two decimal values from the message
                    decimalRegex.findAll(message).forEach { matchResult ->
                        val value = matchResult.value.toDoubleOrNull()
                        if (value != null) {
                            decimalValues.add(value)
                        }
                    }
                    // Check for the specific message pattern "Rs. XXXX"
//                    val rsPattern = Regex("Rs\\.\\s*(\\d+(\\.\\d+)?)") // This regex matches "Rs. XXXX" and captures "XXXX"
//                    val rsMatch = rsPattern.find(message)
//                    val amountString = rsMatch?.groups?.get(1)?.value
//                    val amountValue = amountString?.toDoubleOrNull()
//
//                    if (amountValue != null) {
//                        decimalValues.add(amountValue)
//                    }
                    Log.d("SMSReader", message)

                }
//            messages.add("Address: $address\nMessage: $message")
//            Log.d("SMSReader", message)
                // Process the SMS message here
                // You can display, save, or perform any other action with the SMS data.
            }
        }

    }
    val ans = decimalValues.toList()
    val totalSpend = ans.sum()
    val formattedTotalSpend = String.format("%.2f", totalSpend)
    return formattedTotalSpend

}