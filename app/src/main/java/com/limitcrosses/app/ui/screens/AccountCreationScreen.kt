package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.core.model.SocialHandles
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCreationScreen(
    inviteCode: String,
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onRegistered: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var fbHandle by remember { mutableStateOf("") }
    var igHandle by remember { mutableStateOf("") }
    var snapHandle by remember { mutableStateOf("") }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_title_register), color = CosmicTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = CosmicTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Invite Verified Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CosmicSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicSuccess.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CosmicSuccess)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Verified Invite: $inviteCode",
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary
                        )
                        Text(
                            text = "Assigned initial allowance: ${uiState.assignedDefaultLimit} minutes/day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Account Fields
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(stringResource(R.string.auth_name_label)) },
                placeholder = { Text(stringResource(R.string.auth_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicPrimary,
                    unfocusedBorderColor = CosmicCardBorder,
                    focusedTextColor = CosmicTextPrimary,
                    unfocusedTextColor = CosmicTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email_label)) },
                placeholder = { Text(stringResource(R.string.auth_email_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicPrimary,
                    unfocusedBorderColor = CosmicCardBorder,
                    focusedTextColor = CosmicTextPrimary,
                    unfocusedTextColor = CosmicTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password_label)) },
                placeholder = { Text(stringResource(R.string.auth_password_hint)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicPrimary,
                    unfocusedBorderColor = CosmicCardBorder,
                    focusedTextColor = CosmicTextPrimary,
                    unfocusedTextColor = CosmicTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Optional Social Handles Section
            Text(
                text = stringResource(R.string.auth_optional_handles_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CosmicSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = CosmicTextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.auth_optional_handles_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = fbHandle,
                onValueChange = { fbHandle = it },
                label = { Text(stringResource(R.string.auth_facebook_handle)) },
                placeholder = { Text("@username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicPrimary,
                    unfocusedBorderColor = CosmicCardBorder,
                    focusedTextColor = CosmicTextPrimary,
                    unfocusedTextColor = CosmicTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = igHandle,
                onValueChange = { igHandle = it },
                label = { Text(stringResource(R.string.auth_instagram_handle)) },
                placeholder = { Text("@username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicPrimary,
                    unfocusedBorderColor = CosmicCardBorder,
                    focusedTextColor = CosmicTextPrimary,
                    unfocusedTextColor = CosmicTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = snapHandle,
                onValueChange = { snapHandle = it },
                label = { Text(stringResource(R.string.auth_snapchat_handle)) },
                placeholder = { Text("@username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicPrimary,
                    unfocusedBorderColor = CosmicCardBorder,
                    focusedTextColor = CosmicTextPrimary,
                    unfocusedTextColor = CosmicTextPrimary
                )
            )

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage ?: "",
                    color = CosmicDanger,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    viewModel.register(
                        fullName = fullName,
                        email = email,
                        passwordPlain = password,
                        inviteCode = inviteCode,
                        socialHandles = SocialHandles(
                            facebook = fbHandle.takeIf { it.isNotBlank() },
                            instagram = igHandle.takeIf { it.isNotBlank() },
                            snapchat = snapHandle.takeIf { it.isNotBlank() }
                        )
                    ) {
                        onRegistered()
                    }
                },
                enabled = fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(
                        text = stringResource(R.string.auth_btn_create_account),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
