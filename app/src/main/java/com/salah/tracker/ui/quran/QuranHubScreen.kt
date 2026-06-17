package com.salah.tracker.ui.quran

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salah.tracker.data.database.entities.QuranLog
import com.salah.tracker.data.model.Surah
import com.salah.tracker.data.model.SurahData
import com.salah.tracker.ui.theme.MissedGray
import com.salah.tracker.viewmodel.QuranViewModel
import com.salah.tracker.viewmodel.Verse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranHubScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefsOrNull by viewModel.preferences.collectAsState()
    val prefs = prefsOrNull ?: com.salah.tracker.data.database.entities.UserPreferences()
    var selectedTab by remember { mutableStateOf(0) } // 0: Tracker, 1: Recite Quran
    var activeSurah by remember { mutableStateOf<Surah?>(null) } // Non-null when reading a specific Surah

    if (activeSurah != null) {
        // Fullscreen Surah Reader
        SurahReaderScreen(
            viewModel = viewModel,
            surah = activeSurah!!,
            onBack = { activeSurah = null }
        )
    } else {
        // Main Quran Tracker Hub with Tabs
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Quran Hub",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Recitation Tracker", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Recite Quran", fontWeight = FontWeight.Bold) }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (selectedTab == 0) {
                        RecitationTrackerTab(viewModel = viewModel)
                    } else {
                        ReciteQuranTab(
                            onSelectSurah = { surah ->
                                viewModel.loadSurahVerses(context, surah.number, prefs.quranScript, prefs.showEnglishTranslation, prefs.showUrduTranslation)
                                activeSurah = surah
                            }
                        )
                    }
                }
            }
        }
    }
}

// TAB 1: Recitation Tracker
@Composable
fun RecitationTrackerTab(viewModel: QuranViewModel) {
    val logs by viewModel.quranLogs.collectAsState()
    val currentPage by viewModel.currentReadPage.collectAsState()
    val overallPercentage by viewModel.overallProgressPercentage.collectAsState()
    val currentJuzState by viewModel.currentJuzState.collectAsState()

    var showLogDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall progress card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            "Overall Quran Progress",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            "Page $currentPage / 604",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = (currentPage.toFloat() / 604f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${String.format("%.1f", overallPercentage)}% Completed",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                )
            }
        }

        // Current Juz card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Juz ${currentJuzState.juzNumber}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Page range: ${currentJuzState.startPage} - ${currentJuzState.endPage}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = (currentJuzState.completionPercentage / 100.0).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.background,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${currentJuzState.completionPercentage.toInt()}% of Juz ${currentJuzState.juzNumber} completed",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Add Log Button
        Button(
            onClick = { showLogDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Log Recitation Session")
        }

        // Recent Logs Section
        Text(
            "Recitation History",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No recitation logged yet. Record your progress to begin.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    logs.forEachIndexed { index, log ->
                        QuranLogItem(
                            log = log,
                            onDelete = { viewModel.deleteLog(log) }
                        )
                        if (index < logs.lastIndex) {
                            Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        QuranLogDialog(
            currentPage = currentPage,
            onDismiss = { showLogDialog = false },
            onSave = { surah, startAyah, endAyah, startPage, endPage ->
                viewModel.logRecitation(surah, startAyah, endAyah, startPage, endPage)
                showLogDialog = false
            }
        )
    }
}

// TAB 2: Recite Quran Surah Directory
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciteQuranTab(
    onSelectSurah: (Surah) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredSurahs = remember(searchQuery) {
        SurahData.surahs.filter { surah ->
            surah.nameEnglish.contains(searchQuery, ignoreCase = true) ||
            surah.nameArabic.contains(searchQuery) ||
            surah.number.toString() == searchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Surah...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Surahs List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredSurahs) { surah ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSurah(surah) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Surah number circular badge
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${surah.number}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                surah.nameEnglish,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "${surah.revelationType} • ${surah.verseCount} verses",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            )
                        }

                        Text(
                            surah.nameArabic,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// FULL SCREEN SURAH READER
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderScreen(
    viewModel: QuranViewModel,
    surah: Surah,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val verses by viewModel.activeSurahVerses.collectAsState()
    val isLoading by viewModel.isLoadingSurah.collectAsState()
    val prefsOrNull by viewModel.preferences.collectAsState()
    val prefs = prefsOrNull ?: com.salah.tracker.data.database.entities.UserPreferences()
    var arabicFontSize by remember { mutableStateOf(28) } // adjustable Arabic font size
    var showReaderSettings by remember { mutableStateOf(false) }

    LaunchedEffect(surah.number, prefs.quranScript, prefs.showEnglishTranslation, prefs.showUrduTranslation) {
        viewModel.loadSurahVerses(context, surah.number, prefs.quranScript, prefs.showEnglishTranslation, prefs.showUrduTranslation)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            surah.nameEnglish,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            surah.nameArabic,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { showReaderSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // 1. Beautiful Bismillah Header (except At-Tawbah, Surah 9)
                    if (surah.number != 9) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // 2. Verses List
                    items(verses) { verse ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Right-aligned Arabic Text
                                Text(
                                    text = verse.arabicText,
                                    fontSize = arabicFontSize.sp,
                                    lineHeight = (arabicFontSize * 1.5).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = LocalTextStyle.current.copy(
                                        textDirection = TextDirection.Rtl
                                    )
                                )

                                // Right-aligned Urdu Translation
                                if (prefs.showUrduTranslation && !verse.urduText.isNullOrEmpty()) {
                                    Divider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                                    Text(
                                        text = verse.urduText,
                                        fontSize = (arabicFontSize * 0.7).sp,
                                        lineHeight = (arabicFontSize * 1.1).sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = LocalTextStyle.current.copy(
                                            textDirection = TextDirection.Rtl
                                        )
                                    )
                                }

                                // Left-aligned English Translation
                                if (prefs.showEnglishTranslation && !verse.englishText.isNullOrEmpty()) {
                                    Divider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                                    Text(
                                        text = verse.englishText,
                                        fontSize = (arabicFontSize * 0.65).sp,
                                        lineHeight = (arabicFontSize * 1.0).sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Left,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Verse Number Indicator
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                "Verse ${verse.verse}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReaderSettings) {
        AlertDialog(
            onDismissRequest = { showReaderSettings = false },
            title = { Text("Quran Reader Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    // Font size control
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Arabic Font Size: $arabicFontSize", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (arabicFontSize > 20) arabicFontSize -= 2 }) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { if (arabicFontSize < 48) arabicFontSize += 2 }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Divider()

                    // Script type selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Arabic Script Style", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        var expandedScript by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedScript = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (prefs.quranScript == "INDOPAK") "Indo-Pak Script" else "Uthmani Script")
                            }
                            DropdownMenu(
                                expanded = expandedScript,
                                onDismissRequest = { expandedScript = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Uthmani Script (Global)") },
                                    onClick = {
                                        viewModel.updateQuranPreferences("UTHMANI", prefs.showEnglishTranslation, prefs.showUrduTranslation)
                                        expandedScript = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Indo-Pak Script (South Asia)") },
                                    onClick = {
                                        viewModel.updateQuranPreferences("INDOPAK", prefs.showEnglishTranslation, prefs.showUrduTranslation)
                                        expandedScript = false
                                    }
                                )
                            }
                        }
                    }

                    Divider()

                    // Translations checkboxes
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Translations", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateQuranPreferences(
                                        prefs.quranScript,
                                        !prefs.showEnglishTranslation,
                                        prefs.showUrduTranslation
                                    )
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = prefs.showEnglishTranslation,
                                onCheckedChange = {
                                    viewModel.updateQuranPreferences(
                                        prefs.quranScript,
                                        it,
                                        prefs.showUrduTranslation
                                    )
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("English Translation")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateQuranPreferences(
                                        prefs.quranScript,
                                        prefs.showEnglishTranslation,
                                        !prefs.showUrduTranslation
                                    )
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = prefs.showUrduTranslation,
                                onCheckedChange = {
                                    viewModel.updateQuranPreferences(
                                        prefs.quranScript,
                                        prefs.showEnglishTranslation,
                                        it
                                    )
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Urdu Translation")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showReaderSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun QuranLogItem(
    log: QuranLog,
    onDelete: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, hh:mm a")
    val dateStr = Instant.ofEpochMilli(log.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(formatter)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                log.surah,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Pages ${log.startPage} - ${log.endPage} (${log.pagesRead} pages) • Ayah ${log.startAyah}-${log.endAyah}",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            )
            Text(
                dateStr,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            )
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = MissedGray)
        }
    }
}

@Composable
fun QuranLogDialog(
    currentPage: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int, Int) -> Unit
) {
    var surah by remember { mutableStateOf("") }
    var startAyah by remember { mutableStateOf("") }
    var endAyah by remember { mutableStateOf("") }
    var startPage by remember { mutableStateOf(if (currentPage >= 604) "1" else "${currentPage + 1}") }
    var endPage by remember { mutableStateOf(if (currentPage >= 604) "1" else "${currentPage + 1}") }

    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Recitation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = surah,
                    onValueChange = { surah = it },
                    label = { Text("Surah Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startAyah,
                        onValueChange = { startAyah = it },
                        label = { Text("Start Ayah") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endAyah,
                        onValueChange = { endAyah = it },
                        label = { Text("End Ayah") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startPage,
                        onValueChange = { startPage = it },
                        label = { Text("Start Page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endPage,
                        onValueChange = { endPage = it },
                        label = { Text("End Page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorText.isNotEmpty()) {
                    Text(errorText, color = MissedGray, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (surah.isBlank()) {
                        errorText = "Please enter Surah Name"
                        return@Button
                    }
                    val sA = startAyah.toIntOrNull() ?: 1
                    val eA = endAyah.toIntOrNull() ?: 1
                    val sP = startPage.toIntOrNull() ?: 1
                    val eP = endPage.toIntOrNull() ?: 1

                    if (sP <= 0 || eP <= 0 || sP > 604 || eP > 604) {
                        errorText = "Pages must be between 1 and 604"
                        return@Button
                    }
                    if (eP < sP) {
                        errorText = "End Page cannot be smaller than Start Page"
                        return@Button
                    }

                    onSave(surah, sA, eA, sP, eP)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
