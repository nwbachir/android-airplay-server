package io.github.jqssun.airplay.ui

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.audio.TrackInfo
import io.github.jqssun.airplay.ui.gestures.ServerVolumeState
import io.github.jqssun.airplay.ui.theme.DarkGlass
import io.github.jqssun.airplay.ui.theme.LocalGlass
import io.github.jqssun.airplay.ui.theme.glass
import io.github.jqssun.airplay.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Lecteur plein écran affiché quand un appareil envoie de la musique en AirPlay.
 * Le fond reprend les couleurs dominantes de la pochette et change en fondu à chaque morceau.
 */
@Composable
fun NowPlayingScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val track by viewModel.trackInfo.collectAsState()
    val player = viewModel.dacpPlayer
    val cover = track.coverArt
    val colors = remember(cover) {
        cover?.let { runCatching { extractCoverColors(it) }.getOrNull() } ?: DefaultCoverColors
    }
    val ui = rememberPlayerUi(player, track.durationMs)
    val coverScale by animateFloatAsState(
        targetValue = if (ui.playing) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "cover-scale"
    )

    // le fond est toujours sombre : icônes claires dans les barres système
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val window = activity?.window
        if (window == null) {
            onDispose { }
        } else {
            val ctrl = WindowInsetsControllerCompat(window, window.decorView)
            val prevStatus = ctrl.isAppearanceLightStatusBars
            val prevNav = ctrl.isAppearanceLightNavigationBars
            ctrl.isAppearanceLightStatusBars = false
            ctrl.isAppearanceLightNavigationBars = false
            onDispose {
                ctrl.isAppearanceLightStatusBars = prevStatus
                ctrl.isAppearanceLightNavigationBars = prevNav
            }
        }
    }

    val base = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalGlass provides DarkGlass, LocalContentColor provides Color.White) {
        MaterialTheme(
            colorScheme = base.copy(
                primary = Color.White,
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White,
                onSurfaceVariant = Color.White.copy(alpha = 0.75f),
                surfaceVariant = Color.White.copy(alpha = 0.25f),
                secondaryContainer = Color.White.copy(alpha = 0.25f),
            )
        ) {
            Box(Modifier.fillMaxSize()) {
                CoverBackground(colors)

                val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (landscape) {
                    Row(
                        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            val side = if (maxWidth < maxHeight) maxWidth else maxHeight
                            CoverArt(cover, colors.a, coverScale, Modifier.size(side))
                        }
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center
                        ) {
                            TopBar(onClose)
                            Spacer(Modifier.height(12.dp))
                            TrackTitles(track)
                            Spacer(Modifier.height(16.dp))
                            ProgressBlock(ui)
                            Spacer(Modifier.height(16.dp))
                            TransportRow(viewModel, player, ui)
                            Spacer(Modifier.height(16.dp))
                            VolumeCard(viewModel)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(horizontal = 28.dp, vertical = 12.dp)
                    ) {
                        TopBar(onClose)
                        BoxWithConstraints(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val side = if (maxWidth < maxHeight) maxWidth else maxHeight
                            CoverArt(cover, colors.a, coverScale, Modifier.size(side))
                        }
                        TrackTitles(track)
                        Spacer(Modifier.height(16.dp))
                        ProgressBlock(ui)
                        Spacer(Modifier.height(16.dp))
                        TransportRow(viewModel, player, ui)
                        Spacer(Modifier.height(16.dp))
                        VolumeCard(viewModel)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- fond

private class CoverColors(val a: Color, val b: Color, val c: Color)

private val DefaultCoverColors = CoverColors(Color(0xFF2B3A67), Color(0xFF5B2A86), Color(0xFF0F6E7A))

@Composable
private fun CoverBackground(colors: CoverColors) {
    val a by animateColorAsState(colors.a, tween(900), label = "bg-a")
    val b by animateColorAsState(colors.b, tween(900), label = "bg-b")
    val c by animateColorAsState(colors.c, tween(900), label = "bg-c")
    val transition = rememberInfiniteTransition(label = "cover-bg")
    val t1 = transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "t1"
    )
    val t2 = transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(23000, easing = LinearEasing), RepeatMode.Reverse), label = "t2"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(a, Color.Black, 0.15f), lerp(b, Color.Black, 0.65f))))
            .drawBehind {
                val w = size.width
                val h = size.height
                val r = maxOf(w, h) * 0.7f
                fun blob(col: Color, cx: Float, cy: Float) {
                    val center = Offset(cx, cy)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(col.copy(alpha = 0.65f), col.copy(alpha = 0f)),
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )
                }
                blob(a, w * (0.15f + 0.40f * t1.value), h * (0.10f + 0.20f * t2.value))
                blob(c, w * (0.90f - 0.40f * t2.value), h * (0.50f + 0.15f * t1.value))
                blob(b, w * (0.20f + 0.30f * t2.value), h * (0.95f - 0.20f * t1.value))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))))
            }
    )
}

/** Couleurs dominantes de la pochette (3 teintes distinctes, favorisant les couleurs vives). */
private fun extractCoverColors(src: Bitmap): CoverColors {
    val n = 32
    val small = Bitmap.createScaledBitmap(src, n, n, true)
    val px = IntArray(n * n)
    small.getPixels(px, 0, n, 0, 0, n, n)

    class Bucket(var w: Float = 0f, var r: Float = 0f, var g: Float = 0f, var b: Float = 0f)

    val buckets = HashMap<Int, Bucket>()
    val hsv = FloatArray(3)
    for (p in px) {
        if (AndroidColor.alpha(p) < 128) continue
        AndroidColor.colorToHSV(p, hsv)
        val weight = 0.05f + hsv[1] * hsv[2]
        val r = AndroidColor.red(p)
        val g = AndroidColor.green(p)
        val b = AndroidColor.blue(p)
        val key = ((r shr 5) shl 6) or ((g shr 5) shl 3) or (b shr 5)
        val bk = buckets.getOrPut(key) { Bucket() }
        bk.w += weight
        bk.r += r * weight
        bk.g += g * weight
        bk.b += b * weight
    }
    val ranked = buckets.values
        .filter { it.w > 0f }
        .sortedByDescending { it.w }
        .map { Color(it.r / it.w / 255f, it.g / it.w / 255f, it.b / it.w / 255f) }

    fun dist(x: Color, y: Color) = abs(x.red - y.red) + abs(x.green - y.green) + abs(x.blue - y.blue)
    val picked = mutableListOf<Color>()
    for (c in ranked) {
        if (picked.none { dist(it, c) < 0.45f }) picked += c
        if (picked.size == 3) break
    }
    if (picked.isEmpty()) return DefaultCoverColors
    val a = picked[0]
    val b = picked.getOrNull(1) ?: a.shiftHue(35f)
    val c = picked.getOrNull(2) ?: a.shiftHue(-35f)
    return CoverColors(a.tuned(), b.tuned(), c.tuned())
}

private fun Color.shiftHue(deg: Float): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    return Color.hsv(((hsv[0] + deg) % 360f + 360f) % 360f, hsv[1], hsv[2])
}

// ni trop clair ni trop sombre : le texte blanc reste lisible
private fun Color.tuned(): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    return Color.hsv(hsv[0], (hsv[1] * 1.1f).coerceIn(0f, 1f), hsv[2].coerceIn(0.30f, 0.62f))
}

// ---------------------------------------------------------------- contenu

@Composable
private fun CoverArt(cover: Bitmap?, glow: Color, scale: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(36.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = cover, animationSpec = tween(600), label = "cover") { bmp ->
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_cover_art),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MusicNote, null,
                        modifier = Modifier.size(96.dp),
                        tint = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        GlassCircle(size = 44.dp, pressed = pressed) { mod ->
            Box(
                mod.clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown, stringResource(R.string.cd_minimize),
                    tint = Color.White, modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.now_playing),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(Modifier.size(44.dp))
    }
}

@Composable
private fun TrackTitles(track: TrackInfo) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = track.title.ifEmpty { stringResource(R.string.unknown_track) },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (track.artist.isNotEmpty()) {
            Text(
                text = track.artist,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (track.album.isNotEmpty()) {
            Text(
                text = track.album,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProgressBlock(ui: PlayerUi) {
    val frac = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
    val shown by animateFloatAsState(frac, tween(500, easing = LinearEasing), label = "progress")
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown)
                    .background(Color.White, CircleShape)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(ui.positionMs), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
            Text(
                if (ui.durationMs > 0) formatTime(ui.durationMs) else "",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TransportRow(viewModel: MainViewModel, player: Player?, ui: PlayerUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HoldButton(
            icon = Icons.Default.FastRewind,
            description = stringResource(R.string.cd_rewind),
            size = 44.dp,
            onBegin = { viewModel.audioScanBegin(false) },
            onEnd = { viewModel.audioScanEnd() }
        )
        ClickButton(Icons.Default.SkipPrevious, stringResource(R.string.cd_previous), 56.dp, 30.dp) {
            player?.seekToPrevious()
        }
        ClickButton(
            icon = if (ui.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            description = stringResource(if (ui.playing) R.string.cd_pause else R.string.cd_play),
            size = 80.dp,
            iconSize = 44.dp,
            filled = true
        ) {
            player?.let { if (it.playWhenReady) it.pause() else it.play() }
        }
        ClickButton(Icons.Default.SkipNext, stringResource(R.string.cd_next), 56.dp, 30.dp) {
            player?.seekToNext()
        }
        HoldButton(
            icon = Icons.Default.FastForward,
            description = stringResource(R.string.cd_fast_forward),
            size = 44.dp,
            onBegin = { viewModel.audioScanBegin(true) },
            onEnd = { viewModel.audioScanEnd() }
        )
    }
}

@Composable
private fun VolumeCard(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val context = LocalContext.current
        val serverVolume = remember { ServerVolumeState(context) }
        DisposableEffect(serverVolume) { serverVolume.handleLifecycle(this) }
        VolumeSlider(
            icon = painterResource(R.drawable.ic_download),
            label = stringResource(R.string.volume_server),
            value = serverVolume.percentage / 100f,
            onValueChange = { serverVolume.update((it * 100).roundToInt()) },
            onStep = { serverVolume.adjust(it > 0) }
        )

        val clientVolume by viewModel.clientVolume.collectAsState()
        val clientAdjustable by viewModel.clientVolumeAdjustable.collectAsState()
        // the sender reports back only after each dacp step
        var clientDrag by remember { mutableStateOf<Float?>(null) }
        val clientShown = clientDrag ?: clientVolume
        VolumeSlider(
            icon = painterResource(R.drawable.ic_upload),
            label = stringResource(R.string.volume_client),
            value = clientShown,
            enabled = clientAdjustable,
            onValueChange = { clientDrag = it; viewModel.setClientVolume(it) },
            onValueChangeFinished = { clientDrag = null },
            onStep = { viewModel.setClientVolume((clientShown + it * CLIENT_VOLUME_STEP).coerceIn(0f, 1f)) }
        )
    }
}

// ---------------------------------------------------------------- boutons en verre

/** Disque en verre ; `filled` = bouton principal blanc plein. */
@Composable
private fun GlassCircle(
    size: Dp,
    pressed: Boolean,
    filled: Boolean = false,
    content: @Composable (Modifier) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    val look = if (filled) {
        Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.92f))
    } else {
        Modifier.glass(CircleShape, strong = true)
    }
    content(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(look)
            .dpadFocus(CircleShape)
    )
}

@Composable
private fun ClickButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    iconSize: Dp = 28.dp,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    GlassCircle(size, pressed, filled) { mod ->
        Box(
            mod.clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, description,
                tint = if (filled) Color.Black else Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

// dacp scanning is press-and-hold: beginff/beginrew while held, playresume on release
@Composable
private fun HoldButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    onBegin: () -> Unit,
    onEnd: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    GlassCircle(size, pressed) { mod ->
        Box(
            mod.pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    onBegin()
                    try {
                        awaitRelease()
                    } finally {
                        pressed = false
                        onEnd()
                    }
                })
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, description, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

// ---------------------------------------------------------------- état du lecteur

private class PlayerUi(val playing: Boolean, val positionMs: Long, val durationMs: Long)

@Composable
private fun rememberPlayerUi(player: Player?, fallbackDurationMs: Long): PlayerUi {
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                tick++
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    val version = tick // read so that the player events recompose us
    val playing = version >= 0 && player?.playWhenReady == true
    var pos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(player, playing, version) {
        while (player != null) {
            pos = player.currentPosition
            if (!playing) break
            delay(500)
        }
    }
    val duration = player?.duration?.takeIf { it > 0 && it != C.TIME_UNSET } ?: fallbackDurationMs
    return PlayerUi(playing, pos.coerceAtLeast(0L), duration)
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
