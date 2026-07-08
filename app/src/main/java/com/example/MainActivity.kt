package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// ==========================================
// 1. SUPABASE DATA MODELS & API SERVICE
// ==========================================

@JsonClass(generateAdapter = true)
data class Match(
    @Json(name = "tournament_name") val tournamentName: String?,
    @Json(name = "match_stage") val matchStage: String?,
    @Json(name = "match_date") val matchDate: String?,
    @Json(name = "match_time") val matchTime: String?,
    @Json(name = "match_link") val matchLink: String?
)

interface SupabaseService {
    @GET("rest/v1/matches")
    suspend fun getMatches(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<Match>
}

// ==========================================
// 2. VIEW MODEL FOR NATIVE MATCH SCHEDULES
// ==========================================

sealed interface MatchUiState {
    object Loading : MatchUiState
    data class Success(val matches: List<Match>) : MatchUiState
    data class Error(val message: String) : MatchUiState
}

class RedHawksViewModel : ViewModel() {
    private val _matchState = MutableStateFlow<MatchUiState>(MatchUiState.Loading)
    val matchState: StateFlow<MatchUiState> = _matchState

    private val supabaseUrl = "https://rascvphsnuqfrczmzezr.supabase.co/"
    private val supabaseKey = "sb_publishable_DBATsytnRsYWPobOQzgEFQ_vgOBKnEs"

    private val retrofit = Retrofit.Builder()
        .baseUrl(supabaseUrl)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val service = retrofit.create(SupabaseService::class.java)

    init {
        fetchMatches()
    }

    fun fetchMatches() {
        viewModelScope.launch {
            _matchState.value = MatchUiState.Loading
            try {
                val response = service.getMatches(
                    apiKey = supabaseKey,
                    authorization = "Bearer $supabaseKey"
                )
                _matchState.value = MatchUiState.Success(response)
            } catch (e: Exception) {
                _matchState.value = MatchUiState.Error(e.localizedMessage ?: "Network connection failed")
            }
        }
    }
}

// ==========================================
// 3. MAIN ACTIVITY
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onFinish = { showSplash = false })
                } else {
                    MainAppLayout()
                }
            }
        }
    }
}

// ==========================================
// 4. ANIMATED SPLASH SCREEN (EDITORIAL POLISH)
// ==========================================

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val opacity = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Soft spring entry
        launch {
            scale.animateTo(
                targetValue = 1.1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            scale.animateTo(1.0f, animationSpec = tween(300))
        }
        launch {
            opacity.animateTo(1f, animationSpec = tween(800))
        }
        delay(2500) // Beautiful cinematic entrance delay
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Editorial Stylized Circular Crest
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale.value)
                    .shadow(12.dp, CircleShape, spotColor = EditorialPrimary)
                    .clip(CircleShape)
                    .background(EditorialContainer)
                    .border(2.dp, EditorialPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://blogger.googleusercontent.com/img/a/AVvXsEguBtmKtePT3d23QKV0rJyTBdAF-SVgqh6hSuNJy0Tiz6UHiK6ai6Chw63cXHjdEMcu08lnNeRL8iTgJ2d6CcybgWmRQZ8boXU6W0D0rt_jc6GEks2zkPu-d6z7UbZPzYDxImQ2tVNxWkHh8P-tWbn_seXmdRmtE60eqgkTOJV7IM0Y7eiTp5lttx4tXMVE=w200-h200-c-rw")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Red Hawks Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "RED HAWKS",
                color = EditorialText,
                style = MaterialTheme.typography.displayLarge,
                fontSize = 36.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("splash_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "FORGED IN FIRE, BUILT TO CONQUER",
                color = CrimsonRed, // Vibrant high-contrast red
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = EditorialPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==========================================
// 5. HYBRID APP MAIN LAYOUT (TABS & BOTTOM BAR)
// ==========================================

enum class AppTab(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    WEBSITE("website", "Website", Icons.Filled.Web, Icons.Outlined.Web),
    MATCHES("matches", "Matches", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    ROSTER("roster", "Roster", Icons.Filled.People, Icons.Outlined.People)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainAppLayout() {
    var selectedTab by remember { mutableStateOf(AppTab.WEBSITE) }
    val viewModel = remember { RedHawksViewModel() }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = EditorialContainer,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(BorderStroke(1.dp, EditorialBorder))
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) EditorialPrimary else EditorialTextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (isSelected) EditorialPrimary else EditorialTextSecondary,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = EditorialSecondary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                    )
                }
            }
        },
        containerColor = EditorialBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedTab) {
                AppTab.WEBSITE -> WebsiteScreenTab()
                AppTab.MATCHES -> MatchesScreenTab(viewModel)
                AppTab.ROSTER -> RosterScreenTab()
            }
        }
    }
}

// ==========================================
// Tab 1: SECURE & POWERFUL WEBVIEW TAB
// ==========================================

@Composable
fun WebsiteScreenTab() {
    val context = LocalContext.current
    val bloggerUrl = "https://redhawksofficial.blogspot.com/"
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var progressVal by remember { mutableStateOf(0) }

    // Utility network checker
    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    LaunchedEffect(key1 = true) {
        hasError = !isNetworkAvailable()
    }

    // Intercept back actions to go back in web history securely
    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialBg)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // App top header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EditorialContainer)
                .border(BorderStroke(1.dp, EditorialBorder))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, EditorialPrimary, CircleShape)
                ) {
                    AsyncImage(
                        model = "https://blogger.googleusercontent.com/img/a/AVvXsEguBtmKtePT3d23QKV0rJyTBdAF-SVgqh6hSuNJy0Tiz6UHiK6ai6Chw63cXHjdEMcu08lnNeRL8iTgJ2d6CcybgWmRQZ8boXU6W0D0rt_jc6GEks2zkPu-d6z7UbZPzYDxImQ2tVNxWkHh8P-tWbn_seXmdRmtE60eqgkTOJV7IM0Y7eiTp5lttx4tXMVE=w200-h200-c-rw",
                        contentDescription = "RHK Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "RED HAWKS ESPORTS",
                        color = EditorialText,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Official Web Hub",
                        color = EditorialPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick action controls
            Row {
                IconButton(onClick = {
                    webViewInstance?.reload()
                }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reload", tint = EditorialPrimary)
                }
                IconButton(onClick = {
                    val currentUrl = webViewInstance?.url ?: bloggerUrl
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, currentUrl)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Hub Link"))
                }, modifier = Modifier.testTag("share_button")) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = EditorialPrimary)
                }
            }
        }

        // Custom slim progress bar
        if (isLoading && !hasError) {
            LinearProgressIndicator(
                progress = progressVal / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = EditorialPrimary,
                trackColor = EditorialBg
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasError) {
                // Highly polished Offline Screen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiOff,
                        contentDescription = "Offline",
                        tint = EditorialPrimary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CONNECTION FAILED",
                        color = EditorialText,
                        style = MaterialTheme.typography.headlineLarge,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Red Hawks hub requires an active internet connection to download matches and community updates.",
                        color = EditorialTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (isNetworkAvailable()) {
                                hasError = false
                                webViewInstance?.loadUrl(bloggerUrl)
                            } else {
                                Toast.makeText(context, "Still offline. Please check connection.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialPrimary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("retry_button")
                    ) {
                        Text(text = "TRY AGAIN", color = AccentWhite, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Production-grade customized WebView with DOM support and Deep-Linking
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadsImagesAutomatically = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                supportZoom()
                                builtInZoomControls = true
                                displayZoomControls = false
                                userAgentString = "RedHawksAndroidApp/1.0"
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    super.onReceivedError(view, errorCode, description, failingUrl)
                                    // Soft-handle internet disconnects
                                    if (!isNetworkAvailable()) {
                                        hasError = true
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    if (url == null) return false
                                    // Deep-link third party platforms to native apps automatically!
                                    if (url.startsWith("mailto:") || url.startsWith("tel:") || 
                                        url.contains("facebook.com") || url.contains("youtube.com") || 
                                        url.contains("instagram.com") || url.contains("discord.gg") || url.contains("tiktok.com")) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            ctx.startActivity(intent)
                                            return true
                                        } catch (e: Exception) {
                                            // Fallback
                                        }
                                    }
                                    return false
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    progressVal = newProgress
                                }
                            }

                            loadUrl(bloggerUrl)
                            webViewInstance = this
                        }
                    },
                    update = {
                        webViewInstance = it
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ==========================================
// Tab 2: NATIVE SUPABASE MATCH SCHEDULES SCREEN
// ==========================================

@Composable
fun MatchesScreenTab(viewModel: RedHawksViewModel) {
    val uiState by viewModel.matchState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialBg)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Tab Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EditorialContainer)
                .border(BorderStroke(1.dp, EditorialBorder))
                .padding(vertical = 16.dp, horizontal = 20.dp)
        ) {
            Column {
                Text(
                    text = "LIVE SCHEDULES",
                    color = EditorialPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Track official Red Hawks tournaments and streaming links",
                    color = EditorialTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (uiState) {
                is MatchUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = EditorialPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "FETCHING ESPORTS FEED...", color = EditorialTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is MatchUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = "Error", tint = EditorialPrimary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = (uiState as MatchUiState.Error).message, color = EditorialText, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.fetchMatches() },
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialPrimary)
                        ) {
                            Text(text = "RETRY FEED", color = AccentWhite)
                        }
                    }
                }
                is MatchUiState.Success -> {
                    val matches = (uiState as MatchUiState.Success).matches
                    if (matches.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Inbox, contentDescription = "No matches", tint = EditorialBorder, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "No active schedules planned today.", color = EditorialTextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(matches) { match ->
                                MatchItemCard(match = match, onClickWatch = { link ->
                                    if (!link.isNullOrEmpty() && link != "#schedule") {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "Streaming schedule link coming soon!", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getYouTubeId(url: String?): String? {
    if (url.isNullOrEmpty()) return null
    return try {
        val regExp = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:[^\\/\\n\\s]+\\/\\S+\\/|(?:v|e(?:mbed)?)\\/|\\S*?[?&]v=|(?:live|shorts)\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})"
        val pattern = java.util.regex.Pattern.compile(regExp)
        val matcher = pattern.matcher(url)
        if (matcher.find()) matcher.group(1) else null
    } catch (e: Exception) {
        null
    }
}

@Composable
fun MatchItemCard(match: Match, onClickWatch: (String?) -> Unit) {
    val stage = match.matchStage ?: "STAGES"
    val date = match.matchDate ?: "TBA"
    val time = match.matchTime ?: "TBA"
    val tournament = match.tournamentName ?: "Red Hawks Open Tournament"
    val link = match.matchLink
    val ytId = getYouTubeId(link)

    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EditorialContainer),
        border = BorderStroke(1.dp, EditorialBorder),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .testTag("match_card_${tournament.replace(" ", "_")}")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Underlay YouTube video thumbnail if available for high-quality Blogger-style visualization
            if (!ytId.isNullOrEmpty()) {
                AsyncImage(
                    model = "https://img.youtube.com/vi/$ytId/hqdefault.jpg",
                    contentDescription = "Match Thumbnail Backdrop",
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.18f), // subtle blend with light background
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                // Linear gradient fade overlay to ensure text contrast and a polished aesthetic
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    EditorialContainer.copy(alpha = 0.4f),
                                    EditorialContainer
                                )
                            )
                        )
                )
            } else {
                // Editorial graphic accent pattern underlay when no video is found
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    EditorialSecondary.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row (Status & Stage Badges)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stage Badge
                    Box(
                        modifier = Modifier
                            .background(EditorialSecondary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, EditorialPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = EditorialTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stage.uppercase(),
                                color = EditorialTertiary,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Simulated live/upcoming status indicator with pulsing red glow, like blogger live schedules!
                    val isLiveSoon = link != null && link.contains("youtube")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isLiveSoon) Color(0xFFFFEBEE) else EditorialSecondary.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isLiveSoon) CrimsonRed.copy(alpha = pulseAlpha) else EditorialBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        // Pulsing status dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isLiveSoon) CrimsonRed else AccentGold)
                                .alpha(if (isLiveSoon) pulseAlpha else 1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLiveSoon) "LIVE NOW" else "SCHEDULED",
                            color = if (isLiveSoon) DarkCrimson else EditorialText,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful, bold display editorial header for the tournament name
                Text(
                    text = tournament,
                    color = EditorialText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Metadata Rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(EditorialBg, RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, EditorialBorder.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Date",
                            tint = EditorialPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = date,
                            color = EditorialText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Time Card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(EditorialBg, RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, EditorialBorder.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "Time",
                            tint = EditorialPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = time,
                            color = EditorialText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // High Contrast Premium Button matching Blogger Action Button style
                Button(
                    onClick = { onClickWatch(link) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EditorialPrimary,
                        contentColor = AccentWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 6.dp
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (!ytId.isNullOrEmpty()) Icons.Filled.PlayCircle else Icons.Filled.Bolt,
                            contentDescription = "Watch/Join",
                            tint = AccentWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!ytId.isNullOrEmpty()) "WATCH ON YOUTUBE" else "JOIN TOURNAMENT HUB",
                            color = AccentWhite,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// Tab 3: HIGH FIDELITY ROSTER & LEADERS TAB
// ==========================================

data class Player(val name: String, val role: String, val imageUrl: String, val fb: String, val ig: String, val yt: String)
data class Leader(val name: String, val role: String, val imageUrl: String, val fb: String)
data class Achievement(val title: String, val stage: String, val date: String, val imageUrl: String)

@Composable
fun RosterScreenTab() {
    val context = LocalContext.current

    val roster = listOf(
        Player("BLADE", "RUSHER", "https://blogger.googleusercontent.com/img/a/AVvXsEj74bsPWmazgKibGCDptrOfy1WZtRsEhAbIGoFMiknVyTkl4eOiMnPFgZ00ovsIaVVEAXJwbIlPa8ZZXSqKS3ZCdb4xpxGwM5f91Q1WfHboZR4xkqYa4c85ZoOk_lIsKS6jya5OPVyT7lnCRSw69cdqjZwaNIk__wIvUopW5VljDvPNJPqRn9Y9isYWXN-b=w200-h200-c-rw", "https://www.facebook.com/profile.php?id=100084440170962", "https://www.instagram.com/rhk_blade/", "https://www.youtube.com/@Blade_On_Top"),
        Player("FLICKER", "SNIPER", "https://blogger.googleusercontent.com/img/a/AVvXsEiefN9GKZxrtvN0YFU46_n255MHFR5nhFJdUgeGKva3x-qvyybFjN91_igp5s9I0EsUrQ-guu9p_ghbu1MkLgjJTbATKnQdGzGmIpt--ORkuWA4WytPf0K6bkjri1hfuJGs8sBm3t9W4V_cBwLesZxhpTnBwMOIJ3O2sGL41iCoLIkZ_dCEY_QdZTDc3F3B=w200-h200-c-rw", "https://www.facebook.com/Flickerxz7", "https://www.instagram.com/flickerz7", "https://www.youtube.com/@Flickerz7"),
        Player("NEJAD", "BOMBER", "https://blogger.googleusercontent.com/img/a/AVvXsEiSKZ1vU_MnKAPXjZRjPb7u_TEpa8QMrQ11fhewu379EeRh8jKKWzkiH7U08asDtBviSx2XWqiwvUPjBkwbXHnBdbGAkxe48PkR1s8u1CPG-PtTqaZiu2GErgjsA7m9TBvbzRGIIswAWfTluWxcAuBhiXYFML1kTKYZYnkZVqxxpa7YF5J_1bKb3EhancjX=w200-h200-c-rw", "https://www.facebook.com/NEJAD69/", "https://www.instagram.com/rhk_nejad69/", "https://www.youtube.com/@NejadSix9ine"),
        Player("KUTTUSH", "ALL ROUNDER", "https://blogger.googleusercontent.com/img/a/AVvXsEjSLU0Ed_oP2ZVn3PdJXSJ1MyV36cak6F56ZolNm-0W7zYEo4mzWOORiJTbYTy9j7O0D4olFj_W0tj1fWZrp5amcPY_nAngOR9_XLKj-5cWS5rrBgJkM_VxTxhecVZOIjYd7eKu7vE-QrUogthUz2WmuHCiB7zOS0nyu6sFPWjjownmbKg3towSnqJuWxr0=w200-h200-c-rw", "https://www.facebook.com/profile.php?id=61590850416734", "https://www.instagram.com/beingkuttush/", "https://www.youtube.com/@KUTTUSH-u5r"),
        Player("KAFI", "SUPPORTER", "https://blogger.googleusercontent.com/img/a/AVvXsEijU7WyzHffdVt1YDR9rXRWXrPM0fTHW1JFjMNl1GWIi-Q73Hvj2mkW1DzpGm4_ydLvrkTzRHBosnC3xLhs6hePqXzxhOK_G5pC4hOJF8D2Gv1cSN9ANZNWM-qVxNPsnK-gtfgvluYbKiUTv5oMZ7xJFhF8QioCt-OmbMRXXjE9viwdKxzbpQqdNPdZzxRL=w200-h200-c-rw", "https://www.facebook.com/kaafi.kashfi/", "https://www.instagram.com/md_kaafi/", "https://www.youtube.com/@comfeeOfficial"),
        Player("RAHAT", "ASSAULTER", "https://blogger.googleusercontent.com/img/a/AVvXsEjHb54FRHRXIW6bd20lhtID0z7FaMLon60XO1FziPYQmqsuvGWxkVz5NEhKuwNPiqG51BO3PlVf7IaT0ZtsvO5jaOuzJhC1tQEdu01dCeRlw3wxGk4Qh_pdhZjzpjmNKu4WYJ8uWbZ8Kgh7LyyfBLEwnWZpSiOmZgd2PnbJSom4vKIx3MGiQaZMNYZPAwOf=w200-h200-c-rw", "https://www.facebook.com/rahat.rahman.5891004/", "https://www.instagram.com/rahatrhk/", "https://www.youtube.com/@RahatFF-m8h")
    )

    val leaders = listOf(
        Leader("Rahat Ahmed Rifat", "Founder", "https://blogger.googleusercontent.com/img/a/AVvXsEifFBUCgKMHGimvJPx_H5hUllEzISO1Kiyd4unf_ebebgA9wjyzZroblqKv4sLUA4qOUPdJtfAdxHLwLFETNaet67r4ulZiS9fu8b4kUXELcEh5V7TczPGrC41C8vIfqjmauzd54kGeKKbaQQLyzzTF3pnXq2niP8zhKmPM09vkwGy1NmWcGfIi_DYNju1Y=w200-h200-c-rw", "https://www.facebook.com/rifatnyc/"),
        Leader("Shahriar Arefin Shishier", "Team Manager", "https://blogger.googleusercontent.com/img/a/AVvXsEgsTqRf48HnUcsPnKptvk_Wk_JjXoG1oeScmr-HlywbN_ukWVV5TOrr3_utRCShsjXLP1K6XF6EmMwZrxFfgX-FlwO-8PLc6bT9Gb4zliQSo-94pmx9KxcEIYNgVvUWEAGQgj-2h2hEMKEhRtI6G9yXhVah07PFPaB1Xfn9IbmncpYcYvXBzTp-q8L-lo11=w200-h200-c-rw", "https://www.facebook.com/shahriararefin.shishier")
    )

    val achievements = listOf(
        Achievement("Free Fire World Series - Bangladesh 2026 Spring", "RUNNER UP", "June 6, 2026", "https://blogger.googleusercontent.com/img/a/AVvXsEiXSKBiM9g3NxizocTGo6sPDnhlN_ye5LPfz4exZdwvFB7ZXm0tvzU3Yn79mVADbpvCanWVD2eDmp7pxVD3n6FSNtToq5lFLG4jCdjTYvRVa0Aa2fUrLXawoJ8hMsnPYZ2lW7Os0Y4RFNKv_oC7oBr6yQU3Rqf1FjP58AzuTmH66nSTGjKYf1y6l5_D0P-7=s200-rw"),
        Achievement("FF Bangladesh Pro League Season 1", "CHAMPIONS", "February 13, 2026", "https://blogger.googleusercontent.com/img/a/AVvXsEhTHElU185zZqL2Q7T7VJHcOFIJDD3Wn5i4DiGXDyBHNJpzrbR-VRz81UWs-IJqXDWaflr019u8-poIYmBSPXDi7qwtCILw14raUK-2alWetVZiRuFOR96BTq-0QX-UQZJop6wxk8a4zofR-K-fSj8wh1B4KklCPLSJNMVs60DF86R4YsBF51xJ6ca0UDSD=s200-rw"),
        Achievement("FF World Series - Bangladesh 2025", "CHAMPIONS", "October 4, 2025", "https://blogger.googleusercontent.com/img/a/AVvXsEjTB2iklqNodRuZ_G1lwz-wEHW54tlP_TevFD9c9dgokFotjftxoignWH0QUpt1PVxZHyWVLqkYnD7rbMSjih1H-5gOt7seayK1309FAhJ3_dhpL4WDT4Z-MYF8ibTfp-QDYTgbZI5sy6g82-N9OsXfQPnOQ6ZPrRPQnIZG7intDEQOyJtnPzyZAH1OxJxd=s200-rw"),
        Achievement("Lidoma Endless Series 2025", "CHAMPIONS", "August 21, 2025", "https://blogger.googleusercontent.com/img/a/AVvXsEgV90oSnPeRCdBNu6cgY1oNPmlcg1d8Ql9Huh6LZYsdl3Yp88dgC6OAwl90yd_uBF4n6F2LZ8InZr1Fn5Ol_Gh-65DcjohEVYp3TVU6yxssCJwszKChRVSrw1dCewDPqeg15Mqc0Pu8ntHvBFspUVoQHHH8RRIOnQDHtie7Y7e8xwfqx_XFOUcLArqNxQKg=s200-rw"),
        Achievement("Free Fire Bangladesh Championship 2025", "CHAMPIONS", "June 20, 2025", "https://blogger.googleusercontent.com/img/a/AVvXsEigKmcJW2cQnXEPTqschIPt_ocvPRNRXT5B2X4psK-yYdZLCF9PE1yT-36FrWPpQ2gjB-aTx46Z2_1SRILdyiV94ROyDiwptH_6WZJufXc_rXCMeMaaSegqcC48QoZGA_Q731Uptnxt-543sSeHssYJ1a21J_fYBW8D0KwKKLUQHnpKR-MyMcO6Y_Yykx3Z=s200-rw")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialBg)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App Title Section (Editorial Hub look)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EditorialContainer, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, EditorialBorder))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "ORGANIZATION HUB",
                        color = EditorialPrimary,
                        style = MaterialTheme.typography.headlineLarge,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Official Red Hawks roster, leadership, and trophy room",
                        color = EditorialTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 1. ACHIEVEMENTS SECTIONS (horizontal list)
        item {
            Column {
                Text(
                    text = "🏆 TROPHY CABINET",
                    color = EditorialPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(achievements) { ach ->
                        AchievementCard(ach)
                    }
                }
            }
        }

        // 2. MAIN ROSTER SECTION
        item {
            Text(
                text = "🎮 ACTIVE PRO ROSTER",
                color = EditorialPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(roster.chunked(2)) { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    PlayerCard(player = pair[0], onAction = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    })
                }
                if (pair.size > 1) {
                    Box(modifier = Modifier.weight(1f)) {
                        PlayerCard(player = pair[1], onAction = { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        })
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // 3. LEADERSHIP SECTION
        item {
            Text(
                text = "👑 LEADERSHIP TEAM",
                color = EditorialPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }

        items(leaders) { leader ->
            LeaderCard(leader = leader, onFbClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(leader.fb)))
            })
        }
    }
}

@Composable
fun AchievementCard(ach: Achievement) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EditorialContainer),
        border = BorderStroke(1.dp, EditorialBorder),
        modifier = Modifier
            .width(240.dp)
            .height(180.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High contrast Gold Badge for achievements
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)) // Bright amber container
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = ach.stage,
                        color = Color(0xFF92400E), // Rich dark amber for maximum readability
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Filled.MilitaryTech,
                    contentDescription = "Badge",
                    tint = AccentGold,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = ach.title,
                    color = EditorialText,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ach.date,
                    color = EditorialTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PlayerCard(player: Player, onAction: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EditorialContainer),
        border = BorderStroke(1.dp, EditorialBorder),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .testTag("player_${player.name}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(EditorialBg)
                    .border(2.dp, EditorialPrimary, CircleShape)
            ) {
                AsyncImage(
                    model = player.imageUrl,
                    contentDescription = player.name,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = player.name,
                color = EditorialText,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = player.role,
                color = CrimsonRed, // Vibrant high-contrast red for player roles
                style = MaterialTheme.typography.labelLarge,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action social icons row with high-contrast icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(
                    imageVector = Icons.Filled.Facebook,
                    contentDescription = "FB",
                    tint = Color(0xFF1877F2), // Facebook Blue
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onAction(player.fb) }
                )
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "IG",
                    tint = Color(0xFFE1306C), // Instagram Pink
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onAction(player.ig) }
                )
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = "YT",
                    tint = CrimsonRed, // Youtube Red
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onAction(player.yt) }
                )
            }
        }
    }
}

@Composable
fun LeaderCard(leader: Leader, onFbClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EditorialContainer),
        border = BorderStroke(1.dp, EditorialBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(1.dp, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EditorialBg)
                        .border(1.5.dp, EditorialPrimary, CircleShape)
                ) {
                    AsyncImage(
                        model = leader.imageUrl,
                        contentDescription = leader.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = leader.name,
                        color = EditorialText,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 15.sp
                    )
                    Text(
                        text = leader.role,
                        color = EditorialTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(onClick = onFbClick) {
                Icon(
                    imageVector = Icons.Filled.Link,
                    contentDescription = "Contact Founder",
                    tint = EditorialPrimary
                )
            }
        }
    }
}

