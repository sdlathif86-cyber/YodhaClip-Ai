package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.ExportedDownloadEntity
import com.example.data.StudioSettings
import com.example.data.StudioStorageBreakdown
import com.example.data.VideoClipEntity
import com.example.data.VideoProjectEntity
import com.example.ui.components.StudioAmbientBackground
import com.example.ui.screens.ClipsScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveStreamShortsScreen
import com.example.ui.screens.LongVideoShortsScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UploadScreen
import com.example.ui.screens.YouTubeShortsScreen
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.MidnightPlum
import com.example.ui.theme.RoyalViolet

const val ROUTE_HOME = "home"
const val ROUTE_UPLOAD = "upload"
const val ROUTE_LONG_VIDEO_SHORTS = "long_video_shorts"
const val ROUTE_LIVE_STREAM_SHORTS = "live_stream_shorts"
const val ROUTE_YOUTUBE_SHORTS = "youtube_shorts"
const val ROUTE_DOWNLOADS = "downloads"
const val ROUTE_PROJECTS = "projects"
const val ROUTE_CLIPS = "clips"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_PROJECT_DETAIL = "project_detail/{projectId}"

private data class StudioNavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val MobileBottomDestinations = listOf(
    StudioNavDestination(
        route = ROUTE_HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    StudioNavDestination(
        route = ROUTE_UPLOAD,
        labelRes = R.string.nav_upload,
        selectedIcon = Icons.Rounded.CloudUpload,
        unselectedIcon = Icons.Outlined.CloudUpload,
        testTag = "nav_upload"
    ),
    StudioNavDestination(
        route = ROUTE_PROJECTS,
        labelRes = R.string.nav_projects,
        selectedIcon = Icons.Rounded.FolderOpen,
        unselectedIcon = Icons.Outlined.FolderOpen,
        testTag = "nav_projects"
    ),
    StudioNavDestination(
        route = ROUTE_CLIPS,
        labelRes = R.string.nav_clips,
        selectedIcon = Icons.Rounded.ContentCut,
        unselectedIcon = Icons.Outlined.ContentCut,
        testTag = "nav_clips"
    ),
    StudioNavDestination(
        route = ROUTE_SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)

private val RailDestinations = listOf(
    MobileBottomDestinations[0],
    StudioNavDestination(
        route = ROUTE_LONG_VIDEO_SHORTS,
        labelRes = R.string.nav_long_shorts,
        selectedIcon = Icons.Rounded.MovieCreation,
        unselectedIcon = Icons.Outlined.MovieCreation,
        testTag = "nav_long_shorts"
    ),
    StudioNavDestination(
        route = ROUTE_LIVE_STREAM_SHORTS,
        labelRes = R.string.nav_live_shorts,
        selectedIcon = Icons.Rounded.LiveTv,
        unselectedIcon = Icons.Outlined.LiveTv,
        testTag = "nav_live_shorts"
    ),
    StudioNavDestination(
        route = ROUTE_YOUTUBE_SHORTS,
        labelRes = R.string.nav_yt_shorts,
        selectedIcon = Icons.Rounded.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_yt_shorts"
    ),
    MobileBottomDestinations[1],
    MobileBottomDestinations[2],
    MobileBottomDestinations[3],
    StudioNavDestination(
        route = ROUTE_DOWNLOADS,
        labelRes = R.string.nav_downloads,
        selectedIcon = Icons.Rounded.DownloadDone,
        unselectedIcon = Icons.Outlined.DownloadDone,
        testTag = "nav_downloads"
    ),
    MobileBottomDestinations[4]
)

@Composable
fun YodhaClipApp(
    viewModel: YodhaViewModel = viewModel(),
    longVideoViewModel: LongVideoShortsViewModel = viewModel(),
    liveStreamViewModel: LiveStreamShortsViewModel = viewModel(),
    youtubeShortsViewModel: YouTubeShortsViewModel = viewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val clips by viewModel.clips.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val storageBreakdown by viewModel.storageBreakdown.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val exportingClipId by viewModel.exportingClipId.collectAsStateWithLifecycle()
    val exportClipProgress by viewModel.exportClipProgress.collectAsStateWithLifecycle()

    val longState by longVideoViewModel.uiState.collectAsStateWithLifecycle()
    val liveState by liveStreamViewModel.uiState.collectAsStateWithLifecycle()
    val ytState by youtubeShortsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbar()
    }

    LaunchedEffect(longState.statusMessage) {
        val msg = longState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        longVideoViewModel.consumeStatusMessage()
    }

    LaunchedEffect(liveState.statusMessage) {
        val msg = liveState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        liveStreamViewModel.consumeStatusMessage()
    }

    LaunchedEffect(ytState.statusMessage) {
        val msg = ytState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        youtubeShortsViewModel.consumeStatusMessage()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

    val navigateTopLevel: (String) -> Unit = { destinationRoute ->
        navController.navigate(destinationRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openModuleRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    val openProjectDetail: (Long) -> Unit = { projectId ->
        navController.navigate("project_detail/$projectId") {
            launchSingleTop = true
        }
    }

    StudioAmbientBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            actionColor = CyberMagenta,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        StudioBottomNavigationBar(
                            currentRoute = currentRoute,
                            onNavigate = navigateTopLevel
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        StudioNavigationRail(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (route in setOf(
                                        ROUTE_LONG_VIDEO_SHORTS,
                                        ROUTE_LIVE_STREAM_SHORTS,
                                        ROUTE_YOUTUBE_SHORTS,
                                        ROUTE_DOWNLOADS
                                    )
                                ) {
                                    openModuleRoute(route)
                                } else {
                                    navigateTopLevel(route)
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        YodhaNavHost(
                            navController = navController,
                            viewModel = viewModel,
                            longVideoViewModel = longVideoViewModel,
                            liveStreamViewModel = liveStreamViewModel,
                            youtubeShortsViewModel = youtubeShortsViewModel,
                            projects = projects,
                            clips = clips,
                            downloads = downloads,
                            settings = settings,
                            uploadState = uploadState,
                            longState = longState,
                            liveState = liveState,
                            ytState = ytState,
                            cacheSizeBytes = cacheSizeBytes,
                            storageBreakdown = storageBreakdown,
                            exportingClipId = exportingClipId,
                            exportClipProgress = exportClipProgress,
                            onNavigateTopLevel = navigateTopLevel,
                            onOpenModuleRoute = openModuleRoute,
                            onOpenProject = openProjectDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val activeTopRoute = when {
        currentRoute.startsWith("project_detail") -> ROUTE_PROJECTS
        currentRoute in setOf(
            ROUTE_LONG_VIDEO_SHORTS,
            ROUTE_LIVE_STREAM_SHORTS,
            ROUTE_YOUTUBE_SHORTS,
            ROUTE_DOWNLOADS
        ) -> ROUTE_HOME
        else -> currentRoute
    }

    NavigationBar(
        containerColor = MidnightPlum.copy(alpha = 0.96f),
        tonalElevation = 8.dp
    ) {
        MobileBottomDestinations.forEach { destination ->
            val selected = activeTopRoute == destination.route
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = RoyalViolet.copy(alpha = 0.85f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(destination.testTag)
            )
        }
    }
}

@Composable
private fun StudioNavigationRail(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val activeTopRoute = if (currentRoute.startsWith("project_detail")) {
        ROUTE_PROJECTS
    } else {
        currentRoute
    }

    NavigationRail(
        containerColor = MidnightPlum.copy(alpha = 0.94f),
        header = {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(listOf(RoyalViolet, CyberMagenta)),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_yodhaclip_logo_1795160688407),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    ) {
        RailDestinations.forEach { destination ->
            val selected = activeTopRoute == destination.route
            val label = stringResource(destination.labelRes)
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = RoyalViolet.copy(alpha = 0.85f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(destination.testTag)
            )
        }
    }
}

@Composable
private fun YodhaNavHost(
    navController: NavHostController,
    viewModel: YodhaViewModel,
    longVideoViewModel: LongVideoShortsViewModel,
    liveStreamViewModel: LiveStreamShortsViewModel,
    youtubeShortsViewModel: YouTubeShortsViewModel,
    projects: List<VideoProjectEntity>,
    clips: List<VideoClipEntity>,
    downloads: List<ExportedDownloadEntity>,
    settings: StudioSettings,
    uploadState: UploadStudioState,
    longState: LongVideoShortsUiState,
    liveState: LiveStreamShortsUiState,
    ytState: YouTubeShortsUiState,
    cacheSizeBytes: Long,
    storageBreakdown: StudioStorageBreakdown,
    exportingClipId: Long?,
    exportClipProgress: Int,
    onNavigateTopLevel: (String) -> Unit,
    onOpenModuleRoute: (String) -> Unit,
    onOpenProject: (Long) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                projects = projects,
                clips = clips,
                downloadCount = downloads.size,
                settings = settings,
                onNavigateToUpload = { onNavigateTopLevel(ROUTE_UPLOAD) },
                onNavigateToLongVideoShorts = { onOpenModuleRoute(ROUTE_LONG_VIDEO_SHORTS) },
                onNavigateToLiveStreamShorts = { onOpenModuleRoute(ROUTE_LIVE_STREAM_SHORTS) },
                onNavigateToYouTubeShorts = { onOpenModuleRoute(ROUTE_YOUTUBE_SHORTS) },
                onNavigateToDownloads = { onOpenModuleRoute(ROUTE_DOWNLOADS) },
                onNavigateToProjects = { onNavigateTopLevel(ROUTE_PROJECTS) },
                onNavigateToClips = { onNavigateTopLevel(ROUTE_CLIPS) },
                onNavigateToSettings = { onNavigateTopLevel(ROUTE_SETTINGS) },
                onOpenProject = onOpenProject
            )
        }

        composable(ROUTE_LONG_VIDEO_SHORTS) {
            LongVideoShortsScreen(
                state = longState,
                onBack = { navController.popBackStack() },
                onVideoPicked = longVideoViewModel::onVideoPicked,
                onClearVideo = longVideoViewModel::clearSelectedVideo,
                onTranscriptChange = longVideoViewModel::onTranscriptTextChange,
                onTranscriptFilePicked = longVideoViewModel::onTranscriptFilePicked,
                onGenerateTranscript = longVideoViewModel::generateAiTranscript,
                onMomentCountChange = longVideoViewModel::onRequestedMomentCountChange,
                onSubtitleLanguageChange = longVideoViewModel::onSubtitleLanguageChange,
                onConvertTo9x16Change = longVideoViewModel::onConvertTo9x16Change,
                onSmartCropChange = longVideoViewModel::onDefaultSmartCropChange,
                onDetectMoments = longVideoViewModel::detectBestMomentsWithAi,
                onToggleClipSelection = longVideoViewModel::toggleClipSelection,
                onSelectAllClips = longVideoViewModel::selectAllClips,
                onClearSelection = longVideoViewModel::clearClipSelection,
                onUpdateClipProposal = { id, title, s, e, crop, tags, hook, seoDesc, thumbText, captionLines ->
                    longVideoViewModel.updateClipProposal(
                        clipId = id,
                        newTitle = title,
                        newStartMs = s,
                        newEndMs = e,
                        newCropMode = crop,
                        newHashtags = tags,
                        newOpeningHook = hook,
                        newSeoDescription = seoDesc,
                        newThumbnailText = thumbText,
                        newSubtitleLines = captionLines,
                        subtitleLanguage = longState.subtitleLanguage
                    )
                },
                onRegenerateClipMetadata = longVideoViewModel::regenerateClipMetadata,
                onExportSingleMp4 = { proposal ->
                    longVideoViewModel.exportSingleClipMp4(
                        proposal = proposal,
                        exportQuality = settings.exportQuality
                    ) { viewModel.refreshCacheSize() }
                },
                onExportSelectedBatchMp4 = {
                    longVideoViewModel.exportSelectedBatchMp4(
                        exportQuality = settings.exportQuality
                    ) { viewModel.refreshCacheSize() }
                },
                onExportSubtitlesSrt = { proposal ->
                    longVideoViewModel.exportSubtitlesSrt(proposal) { viewModel.refreshCacheSize() }
                },
                onSaveSelectedToLibrary = {
                    longVideoViewModel.saveSelectedToStudioLibrary { savedProjectId ->
                        viewModel.refreshCacheSize()
                        onOpenProject(savedProjectId)
                    }
                },
                onOpenDownloads = { onOpenModuleRoute(ROUTE_DOWNLOADS) },
                onInputModeChange = longVideoViewModel::onInputModeChange,
                onYouTubeUrlChange = longVideoViewModel::onYouTubeUrlChange,
                onYouTubeOwnershipConfirmed = longVideoViewModel::onYouTubeOwnershipConfirmed,
                onAnalyzeYouTubeLink = longVideoViewModel::analyzeYouTubeLink,
                onTargetDurationChange = longVideoViewModel::onTargetDurationChange,
                onRemovePausesChange = longVideoViewModel::onRemovePausesChange,
                onBackgroundMusicPicked = longVideoViewModel::onBackgroundMusicPicked,
                onClearBackgroundMusic = longVideoViewModel::clearBackgroundMusic,
                onShowMessage = longVideoViewModel::showStatus
            )
        }

        composable(ROUTE_LIVE_STREAM_SHORTS) {
            LiveStreamShortsScreen(
                state = liveState,
                onBack = { navController.popBackStack() },
                onStreamUrlChange = liveStreamViewModel::onStreamUrlChange,
                onStreamTitleChange = liveStreamViewModel::onStreamTitleChange,
                onAuthorizationChange = liveStreamViewModel::onAuthorizationConfirmedChange,
                onPresetChange = liveStreamViewModel::onPresetChange,
                onRollingWindowChange = liveStreamViewModel::onRollingWindowChange,
                onAutoDetectChange = liveStreamViewModel::onAutoDetectChange,
                onSubtitleLanguageChange = liveStreamViewModel::onSubtitleLanguageChange,
                onConvertTo9x16Change = liveStreamViewModel::onConvertTo9x16Change,
                onLiveContextNoteChange = liveStreamViewModel::onLiveContextNoteChange,
                onConnectStream = liveStreamViewModel::connectAuthorizedStream,
                onDisconnectStream = { liveStreamViewModel.disconnectStream(silent = false) },
                onPlayerLiveReady = liveStreamViewModel::onPlayerPlaybackLiveReady,
                onPlayerStreamError = liveStreamViewModel::onPlayerPlaybackError,
                onScanLiveMoment = liveStreamViewModel::scanLiveMomentWithAi,
                onApproveClip = { clip ->
                    liveStreamViewModel.approveAndSaveClip(clip) { viewModel.refreshCacheSize() }
                },
                onRejectClip = liveStreamViewModel::rejectClip,
                onExportClipMp4 = { clip ->
                    liveStreamViewModel.exportLiveClipToDownloads(clip) { viewModel.refreshCacheSize() }
                },
                onExportClipSrt = { clip ->
                    liveStreamViewModel.exportLiveSubtitlesSrt(clip) { viewModel.refreshCacheSize() }
                },
                onClearBuffer = {
                    liveStreamViewModel.clearBufferNow()
                    viewModel.refreshCacheSize()
                }
            )
        }

        composable(ROUTE_DOWNLOADS) {
            DownloadsScreen(
                downloads = downloads,
                onBack = { navController.popBackStack() },
                onDeleteDownload = viewModel::deleteDownloadRecord,
                onClearHistory = viewModel::clearDownloadHistory,
                onOpenLongVideoShorts = { onOpenModuleRoute(ROUTE_LONG_VIDEO_SHORTS) },
                onShowMessage = viewModel::showMessage
            )
        }

        composable(ROUTE_YOUTUBE_SHORTS) {
            YouTubeShortsScreen(
                state = ytState,
                settings = settings,
                onBack = { navController.popBackStack() },
                onUrlChange = youtubeShortsViewModel::onUrlInputChange,
                onFetchMetadata = youtubeShortsViewModel::fetchVideoDetails,
                onOwnershipConfirmedChange = youtubeShortsViewModel::onOwnershipConfirmedChange,
                onOriginalVideoPicked = youtubeShortsViewModel::onOriginalVideoPicked,
                onClearOriginalVideo = youtubeShortsViewModel::clearOriginalVideo,
                onTranscriptChange = youtubeShortsViewModel::onTranscriptChange,
                onTranscriptFilePicked = youtubeShortsViewModel::onTranscriptFilePicked,
                onClipCountChange = youtubeShortsViewModel::onRequestedClipCountChange,
                onSubtitleLanguageChange = youtubeShortsViewModel::onSubtitleLanguageChange,
                onConvertTo9x16Change = youtubeShortsViewModel::onConvertTo9x16Change,
                onAnalyzeWithAi = youtubeShortsViewModel::analyzeWithAi,
                onToggleClipSelection = youtubeShortsViewModel::toggleClipSelection,
                onSelectAllClips = youtubeShortsViewModel::selectAllClips,
                onSelectTopFiveClips = youtubeShortsViewModel::selectTopFiveClips,
                onClearSelection = youtubeShortsViewModel::clearClipSelection,
                onDownloadSingleMp4 = youtubeShortsViewModel::downloadSingleShortMp4,
                onDownloadSelectedBatch = youtubeShortsViewModel::downloadSelectedShortsBatch,
                onDownloadSubtitlesSrt = youtubeShortsViewModel::downloadSubtitlesSrt,
                onSaveSelectedToLibrary = {
                    youtubeShortsViewModel.saveSelectedToStudioLibrary { savedProjectId ->
                        viewModel.refreshCacheSize()
                        onOpenProject(savedProjectId)
                    }
                },
                onShowMessage = youtubeShortsViewModel::showStatus
            )
        }

        composable(ROUTE_UPLOAD) {
            UploadScreen(
                uploadState = uploadState,
                settings = settings,
                onMp4Picked = viewModel::onVideoPicked,
                onProjectTitleChange = viewModel::updateUploadProjectTitle,
                onAspectRatioChange = viewModel::updateUploadAspectRatio,
                onCaptionStyleChange = viewModel::updateUploadCaptionStyle,
                onTrimRangeChange = viewModel::updateTrimRange,
                onManualClipTitleChange = viewModel::updateManualClipTitle,
                onPreviewTrim = viewModel::triggerPreviewTrimSeek,
                onConsumePreviewSeek = viewModel::consumePreviewSeekTrigger,
                onAddManualClip = viewModel::addManualClipToStage,
                onGenerateSmartClips = viewModel::generateSmartClips,
                onRemoveStagedClip = viewModel::removeStagedClip,
                onClearSelection = viewModel::clearUploadSelection,
                onSaveProjectAndClips = {
                    viewModel.saveCurrentProjectAndClips { savedProjectId ->
                        onOpenProject(savedProjectId)
                    }
                },
                onOpenYouTubeShorts = { onOpenModuleRoute(ROUTE_YOUTUBE_SHORTS) },
                onManualClipSubtitleChange = viewModel::updateManualClipSubtitle,
                onNudgeTrimStart = viewModel::nudgeTrimStart,
                onNudgeTrimEnd = viewModel::nudgeTrimEnd,
                onPreviewStagedClip = viewModel::previewStagedClip,
                onUpdateStagedClip = viewModel::updateStagedClip,
                onExportStagedClipMp4 = viewModel::exportStagedClipMp4,
                onExportAllStagedClipsMp4 = viewModel::exportAllStagedClipsMp4,
                onAiTranscriptChange = viewModel::updateUploadAiTranscript,
                onAiTranscriptFilePicked = viewModel::loadUploadTranscriptFile,
                onAiConsentChange = viewModel::setUploadAiConsent,
                onAnalyzeAiHighlights = viewModel::analyzeUploadTranscriptWithAi,
                onApproveAiHighlight = viewModel::approveAiHighlight,
                onDismissAiHighlight = viewModel::dismissAiHighlight
            )
        }

        composable(ROUTE_PROJECTS) {
            ProjectsScreen(
                projects = projects,
                clips = clips,
                onNavigateToUpload = { onNavigateTopLevel(ROUTE_UPLOAD) },
                onOpenProject = onOpenProject,
                onDeleteProject = viewModel::deleteProject
            )
        }

        composable(
            route = ROUTE_PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: -1L
            val projectFlow = remember(projectId) { viewModel.observeProject(projectId) }
            val projectClipsFlow = remember(projectId) { viewModel.observeClipsForProject(projectId) }
            val project by projectFlow.collectAsStateWithLifecycle(initialValue = null)
            val projectClips by projectClipsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

            ProjectDetailScreen(
                project = project,
                clips = projectClips,
                settings = settings,
                onBack = { navController.popBackStack() },
                onAddClip = viewModel::addClipToProject,
                onToggleFavorite = viewModel::toggleClipFavorite,
                onDeleteClip = viewModel::deleteClip,
                onDeleteProject = viewModel::deleteProject,
                onShowMessage = viewModel::showMessage,
                onEditClip = viewModel::editSavedClip,
                onExportClipMp4 = viewModel::exportSavedClipToMp4,
                exportingClipId = exportingClipId,
                exportClipProgress = exportClipProgress
            )
        }

        composable(ROUTE_CLIPS) {
            ClipsScreen(
                clips = clips,
                settings = settings,
                onNavigateToUpload = { onNavigateTopLevel(ROUTE_UPLOAD) },
                onToggleFavorite = viewModel::toggleClipFavorite,
                onDeleteClip = viewModel::deleteClip,
                onShowMessage = viewModel::showMessage,
                onEditClip = viewModel::editSavedClip,
                onExportClipMp4 = viewModel::exportSavedClipToMp4,
                exportingClipId = exportingClipId,
                exportClipProgress = exportClipProgress
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                settings = settings,
                cacheSizeBytes = cacheSizeBytes,
                storageBreakdown = storageBreakdown,
                projectCount = projects.size,
                clipCount = clips.size,
                downloadCount = downloads.size,
                onExportQualityChange = viewModel::setExportQuality,
                onAspectRatioChange = viewModel::setDefaultAspectRatio,
                onSmartClipSecondsChange = viewModel::setSmartClipSeconds,
                onCaptionStyleChange = viewModel::setDefaultCaptionStyle,
                onShowBeginnerTipsChange = viewModel::setShowBeginnerTips,
                onLoopClipPreviewsChange = viewModel::setLoopClipPreviews,
                onClearCache = viewModel::clearThumbnailCache,
                onClearLiveBuffer = viewModel::clearLiveBufferCache,
                onClearAllCaches = viewModel::clearAllStudioCaches,
                onOpenDownloads = { onOpenModuleRoute(ROUTE_DOWNLOADS) },
                onResetSettings = viewModel::resetSettings,
                onDefaultLanguageChange = viewModel::setDefaultLanguageCode,
                onAutoConvert9x16Change = viewModel::setAutoConvert9x16Export
            )
        }
    }
}
