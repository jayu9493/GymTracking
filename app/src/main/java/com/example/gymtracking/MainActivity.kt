package com.example.gymtracking

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.gymtracking.model.ExerciseDefinition
import com.example.gymtracking.model.WorkoutSession
import com.example.gymtracking.ui.theme.GymTrackingTheme
import com.example.gymtracking.ui.theme.TrophyGold
import com.example.gymtracking.ui.theme.getCategoryColor
import com.example.gymtracking.viewmodel.WorkoutViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymTrackingTheme {
                GymTrackingApp()
            }
        }
    }
}

@Composable
fun EmptyStateView(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymTrackingApp(viewModel: WorkoutViewModel = viewModel()) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var moreSubScreen by remember { mutableStateOf("menu") }
    var sharingSession by remember { mutableStateOf<WorkoutSession?>(null) }
    var showAddWorkoutDialog by remember { mutableStateOf(false) }
    var showAddExerciseDefDialog by remember { mutableStateOf(false) }
    var editingExerciseDef by remember { mutableStateOf<ExerciseDefinition?>(null) }
    var editingSet by remember { mutableStateOf<Pair<String, com.example.gymtracking.model.WorkoutSet>?>(null) }
    var showAddWeightDialog by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf("") }
    
    // Timer state
    var showTimer by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(60) }

    val context = LocalContext.current
    val view = LocalView.current

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            isImporting = true
            viewModel.bulkImportExercises(
                rootUri = it,
                onProgress = { status -> importStatus = status },
                onComplete = {
                    isImporting = false
                    importStatus = ""
                    Toast.makeText(context, "Import Complete!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { 
                        currentDestination = it
                        if (it != AppDestinations.MORE) moreSubScreen = "menu"
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            when {
                                currentDestination == AppDestinations.MORE && moreSubScreen == "directory" -> "Exercise Library"
                                currentDestination == AppDestinations.MORE && moreSubScreen == "weight" -> "Body Weight"
                                currentDestination == AppDestinations.MORE && moreSubScreen == "selfie" -> "Gym Selfies"
                                else -> currentDestination.label
                            },
                            fontWeight = FontWeight.Black
                        )
                    },
                    navigationIcon = {
                        if (currentDestination == AppDestinations.MORE && moreSubScreen != "menu") {
                            IconButton(onClick = { moreSubScreen = "menu" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (currentDestination == AppDestinations.MORE && moreSubScreen == "directory") {
                            IconButton(onClick = { folderLauncher.launch(null) }) {
                                Icon(Icons.Default.Folder, contentDescription = "Bulk Import")
                            }
                        }
                        IconButton(onClick = { captureAndShare(context, view) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Screen")
                        }
                        IconButton(onClick = { shareCSV(context) }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export CSV Data")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = currentDestination == AppDestinations.HOME || currentDestination == AppDestinations.TRACKER || (currentDestination == AppDestinations.MORE && (moreSubScreen == "directory" || moreSubScreen == "weight")),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = {
                            when (currentDestination) {
                                AppDestinations.HOME, AppDestinations.TRACKER -> showAddWorkoutDialog = true
                                AppDestinations.MORE -> {
                                    if (moreSubScreen == "directory") showAddExerciseDefDialog = true
                                    else if (moreSubScreen == "weight") showAddWeightDialog = true
                                }
                                else -> {}
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                androidx.compose.animation.AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        (fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) +
                            slideInHorizontally(animationSpec = androidx.compose.animation.core.tween(250)) { it / 6 }) togetherWith
                            fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                    }, label = "screenTransition"
                ) { destination ->
                    when (destination) {
                        AppDestinations.HOME -> HomeScreen(
                            sessions = viewModel.sessions,
                            bodyWeights = viewModel.bodyWeights,
                            onNavigateToLogs = { currentDestination = AppDestinations.TRACKER },
                            onLogWorkout = { showAddWorkoutDialog = true },
                            onShareSession = { sharingSession = it },
                            viewModel = viewModel
                        )
                        AppDestinations.TRACKER -> TrackerScreen(
                            sessions = viewModel.sessions,
                            onSetClick = { setId, set -> editingSet = setId to set },
                            onShareSession = { sharingSession = it },
                            viewModel = viewModel
                        )
                        AppDestinations.STATS -> StatsScreen(viewModel)
                        AppDestinations.MORE -> MoreHub(
                            moreSubScreen = moreSubScreen,
                            onSubScreenChange = { moreSubScreen = it },
                            viewModel = viewModel,
                            onEditExerciseDef = { editingExerciseDef = it },
                            context = context
                        )
                    }
                }

                if (isImporting) {
                    Dialog(onDismissRequest = {}) {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Importing Exercises...", fontWeight = FontWeight.Bold)
                                Text(importStatus, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddWorkoutDialog) {
        AddWorkoutDialog(
            exerciseDefinitions = viewModel.exerciseDefinitions,
            viewModel = viewModel,
            onDismiss = { showAddWorkoutDialog = false },
            onConfirm = { date, definition, reps, weight, timeMinutes, distanceKm ->
                viewModel.addExercise(date, definition, reps, weight, timeMinutes, distanceKm)
                showAddWorkoutDialog = false
                showTimer = true
            },
            onAddNewExercise = {
                showAddWorkoutDialog = false
                showAddExerciseDefDialog = true
            }
        )
    }

    if (showAddExerciseDefDialog) {
        AddExerciseDefinitionDialog(
            existingCategories = viewModel.exerciseDefinitions.map { it.category }.distinct(),
            onDismiss = { showAddExerciseDefDialog = false },
            onConfirm = { name, category, imageUri ->
                viewModel.addExerciseDefinition(name, category, imageUri)
                showAddExerciseDefDialog = false
            }
        )
    }

    editingExerciseDef?.let { def ->
        EditExerciseDefinitionDialog(
            definition = def,
            existingCategories = viewModel.exerciseDefinitions.map { it.category }.distinct(),
            onDismiss = { editingExerciseDef = null },
            onConfirm = { newName, newCategory, newUri ->
                viewModel.updateExerciseDefinition(def.name, newName, newCategory, newUri)
                editingExerciseDef = null
            }
        )
    }

    editingSet?.let { (setId, set) ->
        EditSetDialog(
            initialReps = set.reps,
            initialWeight = set.weight,
            initialTimeMinutes = set.timeMinutes,
            initialDistanceKm = set.distanceKm,
            onDismiss = { editingSet = null },
            onDelete = {
                viewModel.deleteSet(setId)
                editingSet = null
            },
            onConfirm = { reps, weight, timeMins, distKm ->
                viewModel.updateSet(setId, reps, weight, timeMins, distKm)
                editingSet = null
            }
        )
    }

    if (showAddWeightDialog) {
        AddWeightDialog(
            onDismiss = { showAddWeightDialog = false },
            onConfirm = { weight ->
                viewModel.addBodyWeight(LocalDate.now(), weight)
                showAddWeightDialog = false
            }
        )
    }

    if (showTimer) {
        RestTimerDialog(
            initialSeconds = timerSeconds,
            onDismiss = { showTimer = false },
            onTimeChange = { timerSeconds = it }
        )
    }

    sharingSession?.let { session ->
        ShareDayDialog(
            session = session,
            viewModel = viewModel,
            onDismiss = { sharingSession = null }
        )
    }
}

@Composable
fun TrackerScreen(
    sessions: List<WorkoutSession>,
    onSetClick: (String, com.example.gymtracking.model.WorkoutSet) -> Unit,
    onShareSession: (WorkoutSession) -> Unit,
    viewModel: WorkoutViewModel
) {
    if (sessions.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.DateRange,
            message = "Your fitness journey starts here.\nTap + to choose an exercise and log your first set!"
        )
    } else {
        val totalSets = remember(sessions) {
            sessions.sumOf { s -> s.exercises.sumOf { e -> e.sets.size } }
        }
        val uniqueExercises = remember(sessions) {
            sessions.flatMap { s -> s.exercises.map { it.name } }.distinct().size
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Aesthetic Hero Dashboard Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(26.dp)),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Progress Dashboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    "Your Fitness Stats",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(10.dp).size(22.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HeroStatBox(
                                label = "Workouts",
                                value = "${sessions.size}",
                                modifier = Modifier.weight(1f)
                            )
                            HeroStatBox(
                                label = "Total Sets",
                                value = "$totalSets",
                                modifier = Modifier.weight(1f)
                            )
                            HeroStatBox(
                                label = "Exercises",
                                value = "$uniqueExercises",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            items(sessions, key = { it.id }) { session ->
                SessionCard(session, onSetClick, onShareSession, viewModel)
            }
        }
    }
}

@Composable
fun HeroStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SessionCard(
    session: WorkoutSession,
    onSetClick: (String, com.example.gymtracking.model.WorkoutSet) -> Unit,
    onShareSession: (WorkoutSession) -> Unit,
    viewModel: WorkoutViewModel
) {
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
            else add(GifDecoder.Factory())
        }
        .build()

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        session.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = session.date.format(DateTimeFormatter.ofPattern("EEEE")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = session.date.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                var showDeleteDialog by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Workout",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { onShareSession(session) },
                    modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share Day",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Workout?") },
                        text = { Text("This entire workout session and its logged sets will be permanently deleted.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteSession(session)
                                    showDeleteDialog = false
                                }
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            session.exercises.forEach { exercise ->
                val pb = viewModel.getPersonalBest(exercise.name)
                ExerciseSection(exercise, imageLoader, onSetClick, pb)
            }
        }
    }
}

@Composable
fun ExerciseSection(
    exercise: com.example.gymtracking.model.ExerciseLog,
    imageLoader: ImageLoader,
    onSetClick: (String, com.example.gymtracking.model.WorkoutSet) -> Unit,
    personalBest: Float
) {
    val categoryColor = getCategoryColor(exercise.category)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black
                    )
                    if (personalBest > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = TrophyGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "PB: ${if (personalBest % 1f == 0f) personalBest.toInt() else personalBest} kg",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8860B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = categoryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = exercise.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                
                Spacer(Modifier.height(10.dp))
                
                exercise.sets.forEachIndexed { index, set ->
                    val isCardioSet = set.timeMinutes > 0 || set.distanceKm > 0f || exercise.category.equals("Cardio", ignoreCase = true)
                    val isPB = !isCardioSet && set.weight >= personalBest && personalBest > 0
                    Surface(
                        modifier = Modifier.padding(vertical = 3.dp).clickable { onSetClick(set.id, set) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPB) TrophyGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = if (isPB) androidx.compose.foundation.BorderStroke(1.dp, TrophyGold) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isCardioSet) "Session ${index + 1}" else "Set ${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            if (isCardioSet) {
                                if (set.timeMinutes > 0) {
                                    Text("⏱ ${set.timeMinutes} mins", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(14.dp))
                                }
                                if (set.distanceKm > 0f) {
                                    Text("📍 ${set.distanceKm} km", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                                }
                            } else {
                                Text("${set.reps} reps", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(14.dp))
                                Text("${set.weight} kg", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
            
            if (exercise.gifUrl != null) {
                Spacer(Modifier.width(12.dp))
                AsyncImage(
                    model = exercise.gifUrl,
                    contentDescription = null,
                    imageLoader = imageLoader,
                    modifier = Modifier.size(86.dp).clip(RoundedCornerShape(14.dp)).background(Color.DarkGray),
                    contentScale = ContentScale.Crop
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 14.dp), thickness = 0.75.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectoryScreen(
    definitions: List<ExerciseDefinition>,
    onDelete: (ExerciseDefinition) -> Unit,
    onEdit: (ExerciseDefinition) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("All") + definitions.map { it.category }.distinct()
    val filteredDefinitions = remember(definitions, selectedCategory, searchQuery) {
        definitions
            .filter { selectedCategory == "All" || it.category == selectedCategory }
            .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
    }

    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
            else add(GifDecoder.Factory())
        }
        .build()

    var definitionToDelete by remember { mutableStateOf<ExerciseDefinition?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search exercises") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )

        LazyRow(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredDefinitions, key = { it.name }) { definition ->
                ExerciseGridItem(definition, imageLoader, onDelete = { definitionToDelete = it }, onEdit) { fullScreenImage = definition.gifUrl }
            }
        }
    }

    definitionToDelete?.let { def ->
        AlertDialog(
            onDismissRequest = { definitionToDelete = null },
            title = { Text("Delete Exercise?") },
            text = { Text("Are you sure you want to delete \"${def.name}\" from your exercise library?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(def)
                    definitionToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { definitionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (fullScreenImage != null) {
        Dialog(
            onDismissRequest = { fullScreenImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { fullScreenImage = null }) {
                AsyncImage(
                    model = fullScreenImage,
                    contentDescription = null,
                    imageLoader = imageLoader,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fullScreenImage = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseGridItem(
    definition: ExerciseDefinition,
    imageLoader: ImageLoader,
    onDelete: (ExerciseDefinition) -> Unit,
    onEdit: (ExerciseDefinition) -> Unit,
    onView: () -> Unit
) {
    val categoryColor = getCategoryColor(definition.category)
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = { onView() },
            onLongClick = { onEdit(definition) }
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box {
            Column {
                if (definition.gifUrl != null) {
                    AsyncImage(
                        model = definition.gifUrl,
                        contentDescription = definition.name,
                        imageLoader = imageLoader,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(125.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(categoryColor.copy(alpha = 0.35f), categoryColor.copy(alpha = 0.12f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = categoryColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.padding(14.dp).fillMaxSize()
                            )
                        }
                    }
                }
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = definition.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = categoryColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = definition.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = { onDelete(definition) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(0.85f), CircleShape)
                    .size(30.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun BodyWeightScreen(weights: List<com.example.gymtracking.data.BodyWeightEntity>) {
    if (weights.isEmpty()) {
        EmptyStateView(Icons.Default.Person, "No weight records found.\nTrack your weight regularly to monitor progress.")
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Weight History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                }
                items(weights) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(entry.date, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.weight(1f))
                            Text("${entry.weight} kg", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            // Weight Graph at Bottom
            if (weights.size >= 2) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(180.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Weight Trend", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val dataPoints = weights.sortedBy { it.date }.map { it.weight }
                        val maxVal = dataPoints.maxOrNull() ?: 1f
                        val minVal = dataPoints.minOrNull() ?: 0f
                        val range = if (maxVal == minVal) 1f else maxVal - minVal
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val spacing = w / (dataPoints.size - 1)
                            val points = dataPoints.mapIndexed { i, value ->
                                Offset(i * spacing, h - ((value - minVal) / range * h))
                            }
                            for (i in 0 until points.size - 1) {
                                drawLine(
                                    brush = Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))),
                                    start = points[i], end = points[i + 1],
                                    strokeWidth = 5f, cap = StrokeCap.Round
                                )
                            }
                            points.forEachIndexed { i, pt ->
                                drawCircle(
                                    color = if (i == points.size - 1) Color(0xFF06B6D4) else Color(0xFF4F46E5),
                                    radius = if (i == points.size - 1) 6f else 4f,
                                    center = pt
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddWeightDialog(onDismiss: () -> Unit, onConfirm: (Float) -> Unit) {
    var weight by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Body Weight") },
        text = {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { weight.toFloatOrNull()?.let { onConfirm(it) } }) {
                Text("Save")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseDefinitionDialog(
    definition: ExerciseDefinition,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(definition.name) }
    var category by remember { mutableStateOf(definition.category) }
    var expanded by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Exercise", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                Column {
                    OutlinedTextField(
                        value = category, 
                        onValueChange = { category = it }, 
                        label = { Text("Category (Type to add new)") }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        existingCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { category = cat; expanded = false }
                            )
                        }
                    }
                }

                Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(if (imageUri == null) "Change GIF/Photo" else "New Image Selected")
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && category.isNotBlank()) onConfirm(name, category, imageUri) }) { Text("Save Changes") }
        }
    )
}

@Composable
fun EditSetDialog(
    initialReps: Int,
    initialWeight: Float,
    initialTimeMinutes: Int = 0,
    initialDistanceKm: Float = 0f,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (Int, Float, Int, Float) -> Unit
) {
    val isCardioSet = initialTimeMinutes > 0 || initialDistanceKm > 0f
    var reps by remember { mutableStateOf(initialReps.toString()) }
    var weight by remember { mutableStateOf(initialWeight.toString()) }
    var timeMinutes by remember { mutableStateOf(initialTimeMinutes.toString()) }
    var distanceKm by remember { mutableStateOf(initialDistanceKm.toString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Set?") },
            text = { Text("Are you sure you want to delete this set?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCardioSet) "Edit Cardio Session" else "Edit Set") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCardioSet) {
                    OutlinedTextField(value = timeMinutes, onValueChange = { timeMinutes = it }, label = { Text("Duration (Minutes)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = distanceKm, onValueChange = { distanceKm = it }, label = { Text("Distance (Km)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                } else {
                    OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    reps.toIntOrNull() ?: 0,
                    weight.toFloatOrNull() ?: 0f,
                    timeMinutes.toIntOrNull() ?: 0,
                    distanceKm.toFloatOrNull() ?: 0f
                )
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = { showDeleteConfirm = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkoutDialog(
    exerciseDefinitions: List<ExerciseDefinition>,
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, ExerciseDefinition, Int, Float, Int, Float) -> Unit,
    onAddNewExercise: () -> Unit
) {
    var selectedDefinition by remember { mutableStateOf<ExerciseDefinition?>(null) }
    var showExerciseChooser by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("40") }
    var timeMinutes by remember { mutableStateOf("20") }
    var distanceKm by remember { mutableStateOf("3.0") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isCardio = remember(selectedDefinition) {
        val def = selectedDefinition
        if (def == null) false
        else {
            val name = def.name
            val cat = def.category
            val keywords = listOf("treadmill", "cycling", "cycle", "bike", "running", "run", "elliptical", "cardio", "stair", "rowing", "walk", "sprint")
            cat.equals("Cardio", ignoreCase = true) || keywords.any { name.contains(it, ignoreCase = true) }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showExerciseChooser) {
        ExerciseChooserDialog(
            exerciseDefinitions = exerciseDefinitions,
            viewModel = viewModel,
            onDismiss = { showExerciseChooser = false },
            onSelect = { chosen ->
                selectedDefinition = chosen
                showExerciseChooser = false
                val (lastWeight, lastReps) = viewModel.getLastUsedWeightAndReps(chosen.name)
                if (lastWeight > 0f) {
                    weight = if (lastWeight % 1f == 0f) lastWeight.toInt().toString() else lastWeight.toString()
                }
                if (lastReps > 0) {
                    reps = lastReps.toString()
                }
            },
            onAddNewExercise = {
                showExerciseChooser = false
                onAddNewExercise()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Log Workout Set",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Record weight and repetitions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Date Selector Badge
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("MMM dd")),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Exercise Chooser Trigger Card
                Text("EXERCISE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                if (selectedDefinition == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showExerciseChooser = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Choose Exercise",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Tap to open searchable exercise chooser",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                } else {
                    val def = selectedDefinition!!
                    val categoryColor = getCategoryColor(def.category)
                    val pb = viewModel.getPersonalBest(def.name)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, categoryColor.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(categoryColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = categoryColor)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    def.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = categoryColor.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            def.category,
                                            color = categoryColor,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (pb > 0f) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            color = TrophyGold.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "PB: ${if (pb % 1f == 0f) pb.toInt() else pb} kg",
                                                color = Color(0xFFB8860B),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = { showExerciseChooser = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Change", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (isCardio) {
                    // Cardio Section: Duration (Minutes)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DURATION (MINUTES)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = timeMinutes,
                            onValueChange = { timeMinutes = it },
                            label = { Text("Time in Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(10 to "10m", 15 to "15m", 20 to "20m", 30 to "30m", 45 to "45m").forEach { (mins, label) ->
                                AssistChip(
                                    onClick = { timeMinutes = mins.toString() },
                                    label = { Text(label, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Cardio Section: Distance (Km)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DISTANCE (KM) OR INTENSITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = distanceKm,
                            onValueChange = { distanceKm = it },
                            label = { Text("Distance in Km (optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1f to "1 km", 3f to "3 km", 5f to "5 km", 10f to "10 km").forEach { (km, label) ->
                                AssistChip(
                                    onClick = { distanceKm = if (km % 1f == 0f) km.toInt().toString() else km.toString() },
                                    label = { Text(label, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    // Weight Section with Quick Steppers
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("WEIGHT (KG)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Weight in Kg") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(-5f to "-5", -1f to "-1", 1f to "+1", 5f to "+5").forEach { (delta, label) ->
                                AssistChip(
                                    onClick = {
                                        val cur = weight.toFloatOrNull() ?: 0f
                                        val next = (cur + delta).coerceAtLeast(0f)
                                        weight = if (next % 1f == 0f) next.toInt().toString() else next.toString()
                                    },
                                    label = { Text(label, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Reps Section with Quick Steppers
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("REPETITIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = reps,
                            onValueChange = { reps = it },
                            label = { Text("Number of Reps") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(-1 to "-1", 1 to "+1", 8 to "8", 10 to "10", 12 to "12").forEach { (value, label) ->
                                AssistChip(
                                    onClick = {
                                        if (label.startsWith("+") || label.startsWith("-")) {
                                            val cur = reps.toIntOrNull() ?: 10
                                            reps = (cur + value).coerceAtLeast(1).toString()
                                        } else {
                                            reps = value.toString()
                                        }
                                    },
                                    label = { Text(label, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val def = selectedDefinition
                            if (def != null) {
                                if (isCardio) {
                                    val t = timeMinutes.toIntOrNull() ?: 0
                                    val d = distanceKm.toFloatOrNull() ?: 0f
                                    if (t > 0 || d > 0f) {
                                        onConfirm(selectedDate, def, 1, 0f, t, d)
                                    }
                                } else {
                                    val r = reps.toIntOrNull() ?: 0
                                    val w = weight.toFloatOrNull() ?: 0f
                                    if (r > 0) {
                                        onConfirm(selectedDate, def, r, w, 0, 0f)
                                    }
                                }
                            }
                        },
                        enabled = selectedDefinition != null && (
                            if (isCardio) ((timeMinutes.toIntOrNull() ?: 0) > 0 || (distanceKm.toFloatOrNull() ?: 0f) > 0f)
                            else (reps.toIntOrNull() ?: 0) > 0
                        ),
                        modifier = Modifier.weight(1.5f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save Set", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseChooserDialog(
    exerciseDefinitions: List<ExerciseDefinition>,
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit,
    onSelect: (ExerciseDefinition) -> Unit,
    onAddNewExercise: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember(exerciseDefinitions) {
        listOf("All") + exerciseDefinitions.map { it.category }.distinct().sorted()
    }

    val filteredExercises = remember(exerciseDefinitions, searchQuery, selectedCategory) {
        exerciseDefinitions.filter { def ->
            (selectedCategory == "All" || def.category.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() || def.name.contains(searchQuery, ignoreCase = true) || def.category.contains(searchQuery, ignoreCase = true))
        }.sortedBy { it.name }
    }

    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
            else add(GifDecoder.Factory())
        }
        .build()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Select Exercise",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Fast search & category filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Instant Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search exercises (e.g. Bench Press)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    cat,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Exercises List
                if (filteredExercises.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No exercises found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tap below to create this exercise in your library",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredExercises, key = { it.name }) { def ->
                            val pb = viewModel.getPersonalBest(def.name)
                            ExerciseChooserRow(
                                definition = def,
                                personalBest = pb,
                                imageLoader = imageLoader,
                                onClick = { onSelect(def) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Add New Exercise Quick Action
                Button(
                    onClick = onAddNewExercise,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add New Exercise to Library", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun ExerciseChooserRow(
    definition: ExerciseDefinition,
    personalBest: Float,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(definition.category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (definition.gifUrl != null) {
                AsyncImage(
                    model = definition.gifUrl,
                    contentDescription = null,
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = definition.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = categoryColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = definition.category,
                            color = categoryColor,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (personalBest > 0f) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = TrophyGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "PB: ${if (personalBest % 1f == 0f) personalBest.toInt() else personalBest} kg",
                                color = Color(0xFFB8860B),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Icon(
                Icons.Default.CheckCircleOutline,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDefinitionDialog(
    existingCategories: List<String>,
    onDismiss: () -> Unit, 
    onConfirm: (String, String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Exercise Type", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Exercise Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                Column {
                    OutlinedTextField(
                        value = category, 
                        onValueChange = { category = it }, 
                        label = { Text("Category (Type to add new)") }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        existingCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { category = cat; expanded = false }
                            )
                        }
                    }
                }
                
                Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (imageUri == null) "Select GIF/Photo" else "Change Image")
                    }
                }
                
                if (imageUri != null) {
                    AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).align(Alignment.CenterHorizontally), contentScale = ContentScale.Crop)
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && category.isNotBlank()) onConfirm(name, category, imageUri) }, shape = RoundedCornerShape(12.dp)) { Text("Save Exercise") }
        }
    )
}

@Composable
fun RestTimerDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onTimeChange: (Int) -> Unit
) {
    var remaining by remember { mutableStateOf(initialSeconds) }
    var isRunning by remember { mutableStateOf(true) }
    val progress = (remaining.toFloat() / initialSeconds).coerceIn(0f, 1f)

    LaunchedEffect(isRunning) {
        while (isRunning && remaining > 0) {
            delay(1000L)
            remaining--
        }
        isRunning = false
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Rest Timer", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${remaining}s",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initialSeconds.toString(),
                        onValueChange = {
                            val new = it.toIntOrNull() ?: initialSeconds
                            onTimeChange(new)
                        },
                        label = { Text("Sec") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                    Button(onClick = {
                        remaining = initialSeconds
                        isRunning = true
                    }) {
                        Text("Restart")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDismiss() }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text("Skip") }
        }
    )
}

@Composable
fun HomeScreen(
    sessions: List<WorkoutSession>,
    bodyWeights: List<com.example.gymtracking.data.BodyWeightEntity>,
    onNavigateToLogs: () -> Unit,
    onLogWorkout: () -> Unit,
    onShareSession: (WorkoutSession) -> Unit,
    viewModel: WorkoutViewModel
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }

    val totalSets = remember(sessions) { sessions.sumOf { s -> s.exercises.sumOf { e -> e.sets.size } } }
    val uniqueExercises = remember(sessions) { sessions.flatMap { s -> s.exercises.map { e -> e.name } }.distinct().size }
    val activeThisWeek = remember(sessions) { sessions.filter { !it.date.isBefore(LocalDate.now().minusDays(6)) }.map { it.date }.distinct().size }
    val latestSession = remember(sessions) { sessions.maxByOrNull { it.date } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Welcome Back! 💪",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Consistency is what transforms average into excellence.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item {
            Text(
                "JOURNEY OVERVIEW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroStatBox(
                    label = "Total Workouts",
                    value = "${sessions.size}",
                    modifier = Modifier.weight(1f)
                )
                HeroStatBox(
                    label = "Total Sets",
                    value = "$totalSets",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroStatBox(
                    label = "Exercises Tracked",
                    value = "$uniqueExercises",
                    modifier = Modifier.weight(1f)
                )
                HeroStatBox(
                    label = "Current Weight",
                    value = if (bodyWeights.isNotEmpty()) "${bodyWeights.first().weight} kg" else "N/A",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(10.dp).size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Weekly Consistency",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "$activeThisWeek of 7 Days Active This Week",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { (activeThisWeek / 7f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                }
            }
        }

        item {
            Text(
                "LATEST WORKOUT SUMMARY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(8.dp))

            if (latestSession != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    latestSession.date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = latestSession.date.format(DateTimeFormatter.ofPattern("EEEE")),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = latestSession.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onShareSession(latestSession) },
                                modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share Latest Day",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        latestSession.exercises.take(4).forEach { exercise ->
                            val pb = viewModel.getPersonalBest(exercise.name)
                            ExerciseSection(exercise, imageLoader, onSetClick = { _, _ -> }, personalBest = pb)
                        }
                        if (latestSession.exercises.size > 4) {
                            Text(
                                "+ ${latestSession.exercises.size - 4} more exercises logged",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onNavigateToLogs,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("All Logs", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onShareSession(latestSession) },
                                modifier = Modifier.weight(1.3f).height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share Summary", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            } else {
                EmptyStateView(
                    icon = Icons.Default.FitnessCenter,
                    message = "No workouts logged yet!\nTap '+' below to log your very first session."
                )
            }
        }

        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
fun ShareDayDialog(
    session: WorkoutSession,
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Share Day Summary 🚀",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Clean summary · No GIFs · Discord ready",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Scrollable preview — takes available space, buttons always visible below
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                ) {
                    ShareableDayCardContent(session, viewModel)
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            generateWorkoutShareCard(context, session, viewModel)
                        },
                        modifier = Modifier.weight(1.5f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export & Share", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ShareableDayCardContent(
    session: WorkoutSession,
    viewModel: WorkoutViewModel
) {
    val categories = remember(session) { session.exercises.map { it.category }.distinct() }
    val categoryTitle = remember(categories) {
        when (categories.size) {
            0 -> "Workout"
            1 -> categories.first()
            2 -> "${categories[0]} & ${categories[1]}"
            else -> categories.dropLast(1).joinToString(", ") + " & " + categories.last()
        }
    }
    val totalSets = remember(session) { session.exercises.sumOf { it.sets.size } }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column {
            // Date
            Text(
                text = session.date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            // Category title
            Text(
                text = categoryTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${session.exercises.size} exercises · $totalSets sets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            session.exercises.forEach { exercise ->
                val pb = viewModel.getPersonalBest(exercise.name)
                ExerciseSectionNoGif(exercise, pb)
            }
        }
    }
}

@Composable
fun ExerciseSectionNoGif(
    exercise: com.example.gymtracking.model.ExerciseLog,
    personalBest: Float
) {
    val categoryColor = getCategoryColor(exercise.category)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                color = categoryColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = exercise.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = categoryColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            if (personalBest > 0) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = TrophyGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "PB: ${if (personalBest % 1f == 0f) personalBest.toInt() else personalBest} kg",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB8860B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        exercise.sets.forEachIndexed { index, set ->
            val isCardioSet = set.timeMinutes > 0 || set.distanceKm > 0f || exercise.category.equals("Cardio", ignoreCase = true)
            val isPB = !isCardioSet && set.weight >= personalBest && personalBest > 0
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isPB) TrophyGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isPB) androidx.compose.foundation.BorderStroke(1.dp, TrophyGold) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isCardioSet) "Session ${index + 1}" else "Set ${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    if (isCardioSet) {
                        if (set.timeMinutes > 0) {
                            Text("⏱ ${set.timeMinutes} mins", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(14.dp))
                        }
                        if (set.distanceKm > 0f) {
                            Text("📍 ${set.distanceKm} km", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Text("${set.reps} reps", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(14.dp))
                        Text("${set.weight} kg", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 10.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

fun shareCSV(context: Context) {
    val csvFile = File(context.filesDir, "workout_history.csv")
    if (!csvFile.exists()) {
        Toast.makeText(context, "No data to export yet", Toast.LENGTH_SHORT).show()
        return
    }

    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Workout History"))
}

fun captureAndShare(context: Context, view: android.view.View) {
    if (view.width <= 0 || view.height <= 0) {
        Toast.makeText(context, "Cannot capture screen now", Toast.LENGTH_SHORT).show()
        return
    }

    val width = view.width
    val height = view.height
    val footerHeight = 180
    val bitmap = Bitmap.createBitmap(width, height + footerHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    
    val paint = android.graphics.Paint()
    paint.color = android.graphics.Color.parseColor("#121418")
    canvas.drawRect(0f, 0f, width.toFloat(), (height + footerHeight).toFloat(), paint)
    
    view.draw(canvas)
    
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 48f
    paint.isAntiAlias = true
    paint.textAlign = android.graphics.Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Tracked with GymTracking App 🏆", (width / 2).toFloat(), (height + 80).toFloat(), paint)
    
    paint.textSize = 28f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    paint.color = android.graphics.Color.GRAY
    canvas.drawText("Consistency is the key to progress.", (width / 2).toFloat(), (height + 130).toFloat(), paint)

    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val newFile = File(cachePath, "workout_progress_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(newFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)
        if (contentUri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Workout"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to share photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MoreHub(
    moreSubScreen: String,
    onSubScreenChange: (String) -> Unit,
    viewModel: WorkoutViewModel,
    onEditExerciseDef: (ExerciseDefinition) -> Unit,
    context: android.content.Context
) {
    androidx.compose.animation.AnimatedContent(
        targetState = moreSubScreen,
        transitionSpec = {
            if (targetState == "menu") {
                (fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) +
                    slideInHorizontally { -it / 4 }) togetherWith
                    fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
            } else {
                (fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) +
                    slideInHorizontally { it / 4 }) togetherWith
                    fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
            }
        }, label = "moreTransition"
    ) { subScreen ->
        when (subScreen) {
            "menu" -> MoreMenuScreen(
                onNavigate = onSubScreenChange,
                context = context
            )
            "directory" -> DirectoryScreen(
                definitions = viewModel.exerciseDefinitions,
                onDelete = { viewModel.deleteExerciseDefinition(it) },
                onEdit = onEditExerciseDef
            )
            "weight" -> BodyWeightScreen(viewModel.bodyWeights)
            "selfie" -> GymSelfieScreen(context)
        }
    }
}

@Composable
fun MoreMenuScreen(
    onNavigate: (String) -> Unit,
    context: android.content.Context
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "More",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            MoreMenuItem(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Exercise Library",
                subtitle = "Browse and manage exercises",
                onClick = { onNavigate("directory") }
            )
        }
        item {
            MoreMenuItem(
                icon = Icons.Default.Person,
                title = "Body Weight",
                subtitle = "Track your weight over time",
                onClick = { onNavigate("weight") }
            )
        }
        item {
            MoreMenuItem(
                icon = Icons.Default.Face,
                title = "Gym Selfies",
                subtitle = "Capture your daily progress",
                onClick = { onNavigate("selfie") }
            )
        }
        item {
            MoreMenuItem(
                icon = Icons.Default.FileDownload,
                title = "Export CSV",
                subtitle = "Download your workout data",
                onClick = { shareCSV(context) }
            )
        }
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GymSelfieScreen(context: android.content.Context) {
    val selfieDir = remember {
        val dir = java.io.File(context.filesDir, "gym_selfies")
        if (!dir.exists()) dir.mkdirs()
        dir
    }
    
    var selfies by remember {
        mutableStateOf(
            selfieDir.listFiles()?.filter { it.extension in listOf("jpg", "jpeg", "png") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val dateStr = java.time.LocalDate.now().toString()
                val outFile = java.io.File(selfieDir, "selfie_${dateStr}_${System.currentTimeMillis()}.jpg")
                val outputStream = java.io.FileOutputStream(outFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                selfies = selfieDir.listFiles()?.filter { f -> f.extension in listOf("jpg", "jpeg", "png") }
                    ?.sortedByDescending { f -> f.lastModified() } ?: emptyList()
                Toast.makeText(context, "Selfie saved!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save selfie", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    var selfieToDelete by remember { mutableStateOf<java.io.File?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with add button
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Gym Selfies", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("${selfies.size} photos saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(
                onClick = { launcher.launch("image/*") },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Selfie", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Selfie")
            }
        }
        
        if (selfies.isEmpty()) {
            EmptyStateView(Icons.Default.Face, "No selfies yet.\nCapture your gym progress!")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(selfies.size) { index ->
                    val file = selfies[index]
                    var showFullScreen by remember { mutableStateOf(false) }
                    
                    AsyncImage(
                        model = file,
                        contentDescription = "Selfie from ${file.name.substringAfter("selfie_").substringBefore("_")}",
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = { showFullScreen = true },
                                onLongClick = { selfieToDelete = file }
                            ),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Full-screen viewer
                    if (showFullScreen) {
                        Dialog(
                            onDismissRequest = { showFullScreen = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                    .clickable { showFullScreen = false },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "Full screen selfie",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
            
            selfieToDelete?.let { fileToDelete ->
                AlertDialog(
                    onDismissRequest = { selfieToDelete = null },
                    title = { Text("Delete Selfie?") },
                    text = { Text("This photo will be permanently removed.") },
                    confirmButton = {
                        TextButton(onClick = {
                            fileToDelete.delete()
                            selfies = selfieDir.listFiles()?.filter { f -> f.extension in listOf("jpg", "jpeg", "png") }
                                ?.sortedByDescending { f -> f.lastModified() } ?: emptyList()
                            selfieToDelete = null
                            Toast.makeText(context, "Selfie deleted", Toast.LENGTH_SHORT).show()
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { selfieToDelete = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TRACKER("Logs", Icons.Default.DateRange),
    STATS("Insights", Icons.Default.Info),
    MORE("More", Icons.Default.Menu),
}

fun getCategoryColorInt(category: String): Int {
    return when (category.trim().lowercase()) {
        "chest" -> android.graphics.Color.parseColor("#EF4444")
        "back" -> android.graphics.Color.parseColor("#3B82F6")
        "legs" -> android.graphics.Color.parseColor("#10B981")
        "arms", "biceps", "triceps" -> android.graphics.Color.parseColor("#F59E0B")
        "shoulders" -> android.graphics.Color.parseColor("#8B5CF6")
        "core", "abs" -> android.graphics.Color.parseColor("#EC4899")
        "cardio" -> android.graphics.Color.parseColor("#06B6D4")
        else -> android.graphics.Color.parseColor("#6366F1")
    }
}

fun generateWorkoutShareCard(
    context: Context,
    session: WorkoutSession,
    viewModel: WorkoutViewModel
) {
    val cardWidth = 1080
    val padH = 64f
    val padV = 56f

    // --- Categories & Stats ---
    val categories = session.exercises.map { it.category }.distinct()
    val categoryTitle = when (categories.size) {
        0 -> "WORKOUT"
        1 -> categories.first().uppercase()
        2 -> "${categories[0].uppercase()} & ${categories[1].uppercase()}"
        else -> categories.dropLast(1).joinToString(", ") { it.uppercase() } + " & " + categories.last().uppercase()
    }
    val totalSets = session.exercises.sumOf { it.sets.size }
    val dateText = session.date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"))
    val statsText = "${session.exercises.size} exercises  ·  $totalSets sets"

    // --- Pre-calculate total height ---
    val accentBarHeight = 6f
    val headerHeight = padV + accentBarHeight + 24f + 44f + 16f + 60f + 12f + 36f + 32f + 2f + 28f
    // = padV + accent + gap + date + gap + title + gap + stats + gap + divider + gap

    var exercisesHeight = 0f
    session.exercises.forEach { ex ->
        exercisesHeight += 52f + 16f  // name row + gap before sets
        exercisesHeight += ex.sets.size * 60f  // each set row
        exercisesHeight += 36f  // gap after exercise
    }

    val totalHeight = (headerHeight + exercisesHeight + padV).toInt()

    // --- Create Canvas ---
    val bitmap = Bitmap.createBitmap(cardWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    // --- Background ---
    val bgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#0D0F12")
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, cardWidth.toFloat(), totalHeight.toFloat(), bgPaint)

    // --- Gradient accent bar at top ---
    val gradientPaint = android.graphics.Paint().apply {
        shader = android.graphics.LinearGradient(
            0f, 0f, cardWidth.toFloat(), 0f,
            android.graphics.Color.parseColor("#4F46E5"),
            android.graphics.Color.parseColor("#06B6D4"),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, cardWidth.toFloat(), accentBarHeight, gradientPaint)

    var y = padV + accentBarHeight + 24f

    // --- Date ---
    val datePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#94A3B8")
        textSize = 36f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    canvas.drawText(dateText, padH, y + 36f, datePaint)
    y += 44f + 16f

    // --- Category Title ---
    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 52f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(categoryTitle, padH, y + 52f, titlePaint)
    y += 60f + 12f

    // --- Stats ---
    val statsPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#64748B")
        textSize = 30f
        isAntiAlias = true
    }
    canvas.drawText(statsText, padH, y + 30f, statsPaint)
    y += 36f + 32f

    // --- Divider ---
    val divPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#1E293B")
        strokeWidth = 2f
    }
    canvas.drawLine(padH, y, cardWidth - padH, y, divPaint)
    y += 2f + 28f

    // --- Exercises ---
    session.exercises.forEach { exercise ->
        val catColorInt = getCategoryColorInt(exercise.category)
        val pb = viewModel.getPersonalBest(exercise.name)
        val isCardioExercise = exercise.category.equals("Cardio", ignoreCase = true)

        // Calculate exercise block height for accent bar
        val exerciseBlockHeight = 52f + 16f + exercise.sets.size * 60f

        // Left accent bar (category-colored vertical stripe)
        val accentRect = android.graphics.RectF(padH, y, padH + 6f, y + exerciseBlockHeight)
        val accentPaint = android.graphics.Paint().apply {
            color = catColorInt
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRoundRect(accentRect, 3f, 3f, accentPaint)

        // Exercise name
        val namePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 38f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(exercise.name, padH + 24f, y + 38f, namePaint)

        // Category badge (right-aligned)
        val catBadgePaint = android.graphics.Paint().apply {
            color = catColorInt
            textSize = 26f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        canvas.drawText(exercise.category, cardWidth.toFloat() - padH, y + 34f, catBadgePaint)

        y += 52f + 16f  // name row height + spacing

        // --- Sets ---
        exercise.sets.forEachIndexed { index, set ->
            val isCardioSet = set.timeMinutes > 0 || set.distanceKm > 0f || isCardioExercise
            val isPB = !isCardioSet && pb > 0 && set.weight >= pb

            // PB row background highlight
            if (isPB) {
                val rowBgPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#2D2305")
                    style = android.graphics.Paint.Style.FILL
                }
                val rowRect = android.graphics.RectF(padH + 24f, y, cardWidth.toFloat() - padH, y + 48f)
                canvas.drawRoundRect(rowRect, 10f, 10f, rowBgPaint)

                // Gold left edge on PB row
                val pbEdgePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#FFB800")
                    style = android.graphics.Paint.Style.FILL
                }
                canvas.drawRoundRect(
                    android.graphics.RectF(padH + 24f, y, padH + 30f, y + 48f),
                    3f, 3f, pbEdgePaint
                )
            }

            // Set label
            val setLabelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#94A3B8")
                textSize = 28f
                isAntiAlias = true
            }
            val setLabel = if (isCardioSet) "Session ${index + 1}" else "Set ${index + 1}"
            canvas.drawText(setLabel, padH + 42f, y + 33f, setLabelPaint)

            if (isCardioSet) {
                // Cardio: duration and distance
                val valPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 28f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                val parts = mutableListOf<String>()
                if (set.timeMinutes > 0) parts.add("${set.timeMinutes} mins")
                if (set.distanceKm > 0f) parts.add("${set.distanceKm} km")
                canvas.drawText(parts.joinToString("  ·  "), cardWidth.toFloat() - padH - 16f, y + 33f, valPaint)
            } else {
                // Strength: reps + weight
                val repsPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#CBD5E1")
                    textSize = 28f
                    isAntiAlias = true
                }
                canvas.drawText("${set.reps} reps", padH + 220f, y + 33f, repsPaint)

                val weightColor = if (isPB) android.graphics.Color.parseColor("#FFB800") else android.graphics.Color.WHITE
                val weightPaint = android.graphics.Paint().apply {
                    color = weightColor
                    textSize = 30f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                val weightStr = if (set.weight % 1f == 0f) "${set.weight.toInt()} kg" else "${set.weight} kg"
                val pbSuffix = if (isPB) "  PB" else ""
                canvas.drawText("$weightStr$pbSuffix", cardWidth.toFloat() - padH - 16f, y + 33f, weightPaint)
            }

            y += 60f
        }

        y += 36f // spacing after exercise
    }

    // --- Save & Share ---
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "workout_${session.date}_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Workout"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
