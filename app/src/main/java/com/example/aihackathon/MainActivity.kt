package com.example.aihackathon

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.aihackathon.ui.theme.AIHackathonTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    val phoneNumber = "5195910448"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIHackathonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    var spokenText by remember { mutableStateOf("") }
                    var responseState by remember { mutableStateOf<MessageResponse?>(null) }


                    var showDialog by remember { mutableStateOf(false) }

                    var textAreaText by remember { mutableStateOf("") }

                    var showErrorDialog by remember { mutableStateOf(false) }

                    var showInProgress by remember { mutableStateOf(false) }


                    val options = listOf("email", "sms")

                    var selectedOption by remember { mutableStateOf(options[0]) }

                    // Launcher for the RecognizerIntent
                    val speechLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val data: Intent? = result.data
                            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                            if (!matches.isNullOrEmpty()) {
                                spokenText = matches[0] // take first result
                            }
                        }
                    }

                    val context = LocalContext.current

                    var hasPermission by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.SEND_SMS
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val smslauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        hasPermission = granted

                        if (granted) {
                            sendSMS(context, phoneNumber, textAreaText)
                        }
                    }

                    LaunchedEffect(spokenText) {
                        if (!spokenText.isEmpty()) {
                            showInProgress = true
                            RetrofitClient.instance.postRequest(
                                MessageRequest(
                                    request = spokenText
                                )
                            ).enqueue(object : retrofit2.Callback<MessageResponse> {
                                override fun onResponse(
                                    call: retrofit2.Call<MessageResponse>,
                                    response: retrofit2.Response<MessageResponse>
                                ) {
                                    if (response.isSuccessful) {
                                        responseState = response.body()
                                        textAreaText = responseState?.message ?: ""
                                        showDialog = true
                                        selectedOption = responseState?.type ?: "email"
                                        Log.d("AIRESPONSE-onResponse", "success")
                                    } else {
                                        Log.d("AIRESPONSE-onResponse", "Error")
                                        showErrorDialog = true
                                    }
                                    showInProgress = false
                                }

                                override fun onFailure(call: retrofit2.Call<MessageResponse>, t: Throwable) {
                                    Log.d("AIRESPONSE-onFailure", "Failed")
                                    showErrorDialog = true
                                    showInProgress = false
                                }
                            })
                            spokenText = ""
                        }
                    }



                    Greeting(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        textAreaText = textAreaText,
                        showDialog = showDialog,
                        responseState = responseState,
                        showErrorDialog = showErrorDialog,
                        showInProgress = showInProgress,
                        options = options,
                        selectedOption = selectedOption,
                        onSelected = {
                            selectedOption = it
                        },
                        onClickButton = {
                            responseState = null

                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something...")
                            }
                            speechLauncher.launch(intent)
                        },
                        onShowDialogChanged = {
                            showDialog = it
                        },
                        onShowTextAreaTextChanged = {
                            textAreaText = it
                        },
                        onShowErrorDialogChanged = {
                            showErrorDialog = it
                        },
                        onClickSendEmail = {
                            sendEmail(context, arrayOf("nan@gobridgit.com"),"Subject of email",textAreaText)
                        },
                        onClickSendSMS = { text ->
                            if (hasPermission) {
                                sendSMS(context, phoneNumber, text)
                            } else {
                                smslauncher.launch(Manifest.permission.SEND_SMS)

                            }
                        })
                }
            }
        }
    }

    fun sendEmail(
        context: Context,
        to: Array<String>,
        subject: String,
        body: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822" // email MIME type
            putExtra(Intent.EXTRA_EMAIL, to)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No email client find!", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSMS(context: Context, phoneNumber: String, text: String) {
        if (!text.isEmpty()) {
            SmsManager.getDefault().sendTextMessage(
                phoneNumber, // recipient
                null,
                text, // message
                null,
                null
            )
            Toast.makeText(context, "SMS Sent!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Send empty message? No way!", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(
    modifier: Modifier,
    showDialog: Boolean = false,
    showErrorDialog: Boolean = false,
    responseState: MessageResponse? = null,
    textAreaText: String = "",
    showInProgress: Boolean = false,
    options: List<String>,
    selectedOption: String,
    onSelected: (String) -> Unit = {},
    onClickButton: () -> Unit = {},
    onShowDialogChanged: (Boolean) -> Unit = {},
    onShowTextAreaTextChanged: (String) -> Unit = {},
    onShowErrorDialogChanged: (Boolean) -> Unit = {},
    onClickSendSMS: (String) -> Unit = {},
    onClickSendEmail: (String) -> Unit = {},
) {

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                onClickButton()
            },
            shape = CircleShape
        ) {
            Text("Click")
        }
    }

    if (responseState != null && showDialog) {
        val id = responseState.route.split("/").last()

        val nameIdMap = mapOf(
            "1" to "Project1",
            "2" to "Project2",
            "3" to "Project3"
        )

        BasicAlertDialog(
            onDismissRequest = {
            },
            content = {
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Title
                        Text(
                            text = "Do you want to send broadcast to ${nameIdMap[id]}?",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column {
                            options.forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelected(option) }
                                        .padding(8.dp)
                                ) {
                                    RadioButton(
                                        selected = (option == selectedOption),
                                        onClick = null
                                    )
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = textAreaText,
                            onValueChange = { onShowTextAreaTextChanged(it) },
                            label = { Text("Enter your text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp), // make it taller like a textarea
                            maxLines = Int.MAX_VALUE, // allow multiple lines
                            singleLine = false
                        )

                        // Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {

                            TextButton(onClick = { onShowDialogChanged(false) }) {
                                Text("Cancel")
                            }

                            TextButton(onClick = {
                                if (selectedOption == "sms") {
                                    onClickSendSMS(textAreaText)
                                } else {
                                    onClickSendEmail(textAreaText)
                                }
                                onShowDialogChanged(false)
                            }) {
                                Text("OK")
                            }

                        }
                    }
                }
            })
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Error") },
            text = { Text("Can't parse input, Please try again") },
            confirmButton = {
                TextButton(
                    onClick = { onShowErrorDialogChanged(false) }
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showInProgress) {
        InProgressDialog()
    }
}

@Composable
fun InProgressDialog(text: String = "Loading") {
    Dialog(
        onDismissRequest = {},
        DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        InProgressBox(text = text)
    }
}


@Composable
fun InProgressBox(
    modifier: Modifier = Modifier,
    text: String = "Loading"
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
    ) {
        Box(
            modifier = modifier.background(Color.White),
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp, 24.dp)
                    .widthIn(120.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.padding(start = 25.dp))
                Text(text = text, color = Color.Black)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AIHackathonTheme {
        Greeting(
            modifier = Modifier.fillMaxSize(),
            responseState = MessageResponse(
                route = "broadcast/project/1",
                message = "This is a sample extra text.",
                type = "email"
            ),
            showDialog = true,
            options = listOf("Email", "SMS"),
            selectedOption = "Email"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreviewError() {
    AIHackathonTheme {
        Greeting(
            modifier = Modifier.fillMaxSize(),
            responseState = MessageResponse(
                route = "broadcast/project/1",
                message = "This is a sample extra text.",
                type = "email"
            ),
            showErrorDialog = true,
            options = listOf("Email", "SMS"),
            selectedOption = "Email"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreviewInProgress() {
    AIHackathonTheme {
        Greeting(
            modifier = Modifier.fillMaxSize(),
            responseState = MessageResponse(
                route = "broadcast/project/1",
                message = "This is a sample extra text.",
                type = "email"
            ),
            showInProgress = true,
            options = listOf("Email", "SMS"),
            selectedOption = "Email"
        )
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreviewDone() {
    AIHackathonTheme {
        Greeting(
            modifier = Modifier.fillMaxSize(),
            responseState = MessageResponse(
                route = "broadcast/project/1",
                message = "This is a sample extra text.",
                type = "email"
            ),
            showInProgress = false,
            options = listOf("Email", "SMS"),
            selectedOption = "Email"
        )
    }
}