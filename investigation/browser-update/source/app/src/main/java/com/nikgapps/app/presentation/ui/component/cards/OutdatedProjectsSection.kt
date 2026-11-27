package com.nikgapps.app.presentation.ui.component.cards

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalDragOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nikgapps.app.presentation.ui.component.ProjectGlassSurface
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun OutdatedProjectsSection(
    count: Int,
    onReboundChanged: (Dp) -> Unit = {},
    projectCard: @Composable (index: Int, modifier: Modifier, interactive: Boolean) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var reveal by remember { mutableFloatStateOf(if (expanded) 1f else 0f) }
    var dragging by remember { mutableStateOf(false) }
    var releaseVelocity by remember { mutableFloatStateOf(0f) }
    var reboundPeak by remember { mutableStateOf(0.dp) }
    var reboundOffset by remember { mutableStateOf(0.dp) }
    val dragDistance = with(LocalDensity.current) { 176.dp.toPx() }
    val spread by remember { derivedStateOf { expanded || reveal > 0f } }
    val reportRebound by rememberUpdatedState(onReboundChanged)

    DisposableEffect(Unit) {
        onDispose { reportRebound(0.dp) }
    }

    fun updateRebound(value: Dp) {
        reboundOffset = value
        reportRebound(value)
    }

    LaunchedEffect(expanded, dragging) {
        if (!dragging) {
            val opening = expanded
            val target = if (opening) 1f else 0f
            val peak = reboundPeak
            coroutineScope {
                var bounced = false
                animate(
                    initialValue = reveal,
                    targetValue = target,
                    initialVelocity = releaseVelocity,
                    animationSpec = spring(dampingRatio = 0.62f, stiffness = 105f, visibilityThreshold = 0.001f)
                ) { value, _ ->
                    if (!dragging && expanded == opening) {
                        reveal = value
                        val reachedTarget = if (opening) value >= target else value <= target
                        if (!bounced && reachedTarget && peak != 0.dp) {
                            bounced = true
                            launch {
                                // One spring pulse in the swipe direction as the deck reaches its target.
                                animate(0f, 0f, initialVelocity = 32f,
                                    animationSpec = spring(dampingRatio = 1f, stiffness = 144f, visibilityThreshold = 0.001f)
                                ) { fraction, _ ->
                                    if (!dragging && expanded == opening) {
                                        updateRebound(peak * fraction.coerceIn(0f, 1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun drag(delta: Float) {
        dragging = true
        if (reboundOffset != 0.dp) updateRebound(0.dp)
        reveal = (reveal + delta / dragDistance).coerceIn(0f, 1.3f)
    }

    fun finishDrag(open: Boolean, completed: Boolean, velocity: Float) {
        if (completed) expanded = open
        releaseVelocity = (velocity / dragDistance).coerceIn(-6f, 6f)
        val swipeInDirection = if (open) releaseVelocity.coerceAtLeast(0f) else releaseVelocity.coerceAtMost(0f)
        reboundPeak = if (completed) 10.dp * (swipeInDirection / 6f) else 0.dp
        dragging = false
    }

    fun deckSwipe(open: Boolean, horizontalInset: Dp = 0.dp) =
        Modifier.onVerticalSwipe(towardsDown = open, horizontalInset = horizontalInset, onDrag = ::drag) { completed, velocity ->
            finishDrag(open, completed, velocity)
        }

    val cornerRadius = 18.dp
    val horizontalInset = 16.dp
    val shape = RoundedCornerShape(cornerRadius)
    val colors = MaterialTheme.colorScheme
    val layerCount = count.coerceAtMost(2)

    Layout(
        modifier = Modifier.fillMaxWidth().then(if (expanded) deckSwipe(false, horizontalInset) else Modifier),
        content = {
            ProjectGlassSurface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalInset).clip(shape)
                    .then(if (expanded) Modifier else deckSwipe(true))
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (expanded) "Collapse outdated projects" else "Expand outdated projects"
                    ) {
                        updateRebound(0.dp)
                        releaseVelocity = 0f
                        reboundPeak = if (expanded) (-4).dp else 4.dp
                        expanded = !expanded
                    },
                shape = shape
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(Modifier.size(40.dp), shape = MaterialTheme.shapes.medium, color = colors.errorContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.FolderOff, contentDescription = null, tint = colors.onErrorContainer)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Outdated projects", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (count == 1) "1 project" else "$count projects",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Icon(
                        if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant
                    )
                }
            }
            repeat(if (spread) count else layerCount) { index ->
                val modifier = if (spread) Modifier else Modifier.clearAndSetSemantics { hideFromAccessibility() }
                projectCard(index, modifier, expanded)
            }
        }
    ) { measurables, constraints ->
        val rowConstraints = constraints.copy(minHeight = 0)
        val header = measurables.first().measure(rowConstraints)
        val cards = measurables.drop(1).map { it.measure(rowConstraints) }
        val gap = 12.dp.roundToPx()
        val layerStep = 5.dp.roundToPx()
        val headerCornerDepth = cornerRadius.roundToPx()
        val endpoint = reveal.coerceIn(0f, 1f)
        // Keep the spring's rebound visible without letting a hard fling spread the deck too far.
        val progress = endpoint + ((reveal - endpoint) * 0.3f).coerceIn(-0.06f, 0.06f)
        val offset = reboundOffset.roundToPx()
        var nextRowY = header.height + gap
        var height = header.height
        val positions = cards.mapIndexed { index, card ->
            // Keep the bottom edges aligned in the deck, even when a card has open details.
            val stackedY = header.height + (index + 1).coerceAtMost(layerCount) * layerStep - card.height
            val y = (stackedY + (nextRowY - stackedY) * progress).roundToInt()
            nextRowY += card.height + gap
            height = maxOf(height, y + card.height)
            y
        }
        layout(header.width, height) {
            cards.forEachIndexed { index, card ->
                val y = positions[index]
                val z = (cards.size - index).toFloat()
                if (y < headerCornerDepth) {
                    // Taller cards in the deck can extend into the header's rounded corners.
                    card.placeRelativeWithLayer(0, y + offset, zIndex = z) {
                        clip = true
                        this.shape = DeckCardClip((headerCornerDepth - y).toFloat())
                    }
                } else {
                    card.placeRelative(0, y + offset, zIndex = z)
                }
            }
            header.placeRelative(0, offset, zIndex = cards.size + 1f)
        }
    }
}

private data class DeckCardClip(val top: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(0f, top.coerceAtMost(size.height), size.width, size.height))
}

private fun Modifier.onVerticalSwipe(
    towardsDown: Boolean,
    horizontalInset: Dp,
    onDrag: (Float) -> Unit,
    onFinished: (Boolean, Float) -> Unit
) = pointerInput(towardsDown, horizontalInset) {
    val direction = if (towardsDown) 1 else -1
    val minDistance = 56.dp.toPx()
    val inset = horizontalInset.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (down.position.x < inset || down.position.x >= size.width - inset) return@awaitEachGesture
        var distance = 0f
        val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
            // Leave movement in the other direction available to the Projects list.
            if (overSlop * direction > 0) {
                distance = overSlop * direction
                change.consume()
            }
        } ?: return@awaitEachGesture
        var completed = false
        var velocity = 0f
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, Offset.Zero)
        tracker.addPosition(start.uptimeMillis, Offset(0f, distance * direction))
        try {
            onDrag(distance * direction)
            var lastChange = start
            while (true) {
                val change = awaitVerticalDragOrCancellation(lastChange.id) ?: break
                if (!change.pressed) {
                    val holdTime = change.uptimeMillis - lastChange.uptimeMillis
                    if (holdTime < 60L) velocity = tracker.calculateVelocity().y
                    val projectedDistance = distance + (velocity * direction * 0.06f)
                        .coerceIn(-minDistance / 2f, minDistance / 2f)
                    completed = distance >= minDistance / 2f && projectedDistance >= minDistance
                    break
                }
                val delta = change.positionChange().y
                distance += delta * direction
                tracker.addPosition(change.uptimeMillis, Offset(0f, distance * direction))
                onDrag(delta)
                change.consume()
                lastChange = change
            }
        } finally {
            onFinished(completed, velocity)
        }
    }
}
