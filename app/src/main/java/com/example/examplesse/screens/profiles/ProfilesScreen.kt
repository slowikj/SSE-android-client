package com.example.examplesse.screens.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.examplesse.screens.Screen

@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel,
    navController: NavController
) {
    val scaffoldState = rememberScaffoldState()
    val profilesState by viewModel.profiles.collectAsState(ProfilesUiState())
    val isStreaming by viewModel.isStreaming.collectAsState(true)

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.width(100.dp))
            StreamButton(
                active = isStreaming,
                toggleStreaming = viewModel::toggleProfileStreaming
            )
            ProfilesView(
                profileUiState = profilesState,
                navController = navController
            )
        }
    }
}

@Composable
fun StreamButton(
    active: Boolean,
    toggleStreaming: () -> Unit
) {
    Button(onClick = { toggleStreaming() }) {
        Text(if (active) "Stop streaming" else "Start streaming")
    }
}

@Composable
fun ProfilesView(profileUiState: ProfilesUiState, navController: NavController) {
    when {
        profileUiState.isLoading -> {
            LoadingView()
        }
        profileUiState.error != null -> {
            ErrorView(profileUiState.error)
        }
        else -> {
            ProfileList(
                profileUiState.data,
                navController = navController
            )
        }
    }
}

@Composable
fun ErrorView(throwable: Throwable) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = throwable.toString())
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ProfileList(
    profileList: List<String>,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(profileList.size) { index ->
            ProfileCard(navController, profileList[index])
        }
    }
}

@Composable
private fun ProfileCard(
    navController: NavController,
    profileName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable {
                navController.navigate(Screen.Details.withArgs(profileName))
            },
        shape = RoundedCornerShape(15.dp),
        elevation = 5.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = profileName)
        }
    }
}
