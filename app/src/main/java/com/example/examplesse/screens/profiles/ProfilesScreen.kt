package com.example.examplesse.screens.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        Box(modifier = Modifier.fillMaxSize()) {
            ProfilesView(
                profileUiState = profilesState,
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
            StreamButton(
                active = isStreaming,
                toggleStreaming = viewModel::toggleProfileStreaming,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun StreamButton(
    active: Boolean,
    toggleStreaming: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { toggleStreaming() },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onSecondary
        )
    ) {
        Text(
            text = if (active) "Stop streaming" else "Start streaming",
        )
    }
}

@Composable
fun ProfilesView(
    profileUiState: ProfilesUiState,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    when {
        profileUiState.isLoading -> {
            LoadingView(modifier)
        }
        profileUiState.error != null -> {
            ErrorView(throwable = profileUiState.error, modifier = modifier)
        }
        else -> {
            ProfileList(
                profileList = profileUiState.data,
                navController = navController,
                modifier = modifier
            )
        }
    }
}

@Composable
fun ErrorView(throwable: Throwable, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = throwable.toString())
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ProfileList(
    profileList: List<String>,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(profileList.size) { index ->
            ProfileCard(
                navController = navController,
                profileName = profileList[index]
            )
        }
    }
}

@Composable
private fun ProfileCard(
    navController: NavController,
    profileName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
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
