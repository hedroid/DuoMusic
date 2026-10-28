package com.maxrave.simpmusic.extension

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kmpalette.palette.graphics.Palette
import com.maxrave.domain.data.model.ui.ScreenSizeInfo
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.theme.LocalAppColors
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.materials.HazeMaterials
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

fun Modifier.shimmer(): Modifier =
    composed {
        val appColors = LocalAppColors.current
        var size by remember {
            mutableStateOf(IntSize.Zero)
        }
        val transition = rememberInfiniteTransition(label = "Shimmer")
        val startOffsetX by transition.animateFloat(
            initialValue = -2 * size.width.toFloat(),
            targetValue = 2 * size.width.toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000),
                ),
            label = "Shimmer",
        )

        background(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            appColors.shimmerBackground,
                            appColors.shimmerLine,
                            appColors.shimmerBackground,
                        ),
                    start = Offset(startOffsetX, 0f),
                    end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat()),
                ),
        ).onGloballyPositioned {
            size = it.size
        }
    }

class GreyScaleModifier : DrawModifier {
    override fun ContentDrawScope.draw() {
        val saturationMatrix = ColorMatrix().apply { setToSaturation(0f) }
        val saturationFilter = ColorFilter.colorMatrix(saturationMatrix)
        val paint =
            Paint().apply {
                colorFilter = saturationFilter
            }
        drawIntoCanvas {
            it.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            it.restore()
        }
    }
}

fun LazyListState.visibilityPercent(info: LazyListItemInfo): Float {
    val cutTop = max(0, layoutInfo.viewportStartOffset - info.offset)
    val cutBottom = max(0, info.offset + info.size - layoutInfo.viewportEndOffset)

    return max(0f, 100f - (cutTop + cutBottom) * 100f / info.size)
}

fun Modifier.greyScale() = this.then(GreyScaleModifier())

/**
 * [colors] as gradient stops eased with smoothstep, the way [smoothScrimBrush] eases a scrim. Handed
 * to the renderer as two stops, each ramp is a straight line with a corner where it leaves one colour
 * and another where it reaches the next, and those corners read as creases across the glow.
 */
private fun easedColorStops(
    colors: List<Color>,
    stepsPerSegment: Int = 24,
): Array<Pair<Float, Color>> {
    val segments = colors.size - 1
    val total = segments * stepsPerSegment
    return Array(total + 1) { i ->
        val segment = minOf(i / stepsPerSegment, segments - 1)
        val t = (i - segment * stepsPerSegment) / stepsPerSegment.toFloat()
        i / total.toFloat() to androidx.compose.ui.graphics.lerp(colors[segment], colors[segment + 1], t * t * (3f - 2f * t))
    }
}

fun Modifier.angledGradientBackground(
    colors: List<Color>,
    degrees: Float,
) = this.then(
    if (colors.size < 2) {
        Modifier
    } else {
        Modifier.drawBehind {
            /*
            Have to compute length of gradient vector so that it lies within
            the visible rectangle.
            --------------------------------------------
            | length of gradient ^  /                  |
            |             --->  /  /                   |
            |                  /  / <- rotation angle  |
            |                 /  o --------------------|  y
            |                /  /                      |
            |               /  /                       |
            |              v  /                        |
            --------------------------------------------
                                 x

                       diagonal angle = atan2(y, x)
                     (it's hard to draw the diagonal)

            Simply rotating the diagonal around the centre of the rectangle
            will lead to points outside the rectangle area. Further, just
            truncating the coordinate to be at the nearest edge of the
            rectangle to the rotated point will distort the angle.
            Let α be the desired gradient angle (in radians) and γ be the
            angle of the diagonal of the rectangle.
            The correct for the length of the gradient is given by:
            x/|cos(α)|  if -γ <= α <= γ,   or   π - γ <= α <= π + γ
            y/|sin(α)|  if  γ <= α <= π - γ, or π + γ <= α <= 2π - γ
            where γ ∈ (0, π/2) is the angle that the diagonal makes with
            the base of the rectangle.

             */

            val (x, y) = size
            val gamma = atan2(y, x)

            if (gamma == 0f || gamma == (PI / 2).toFloat()) {
                // degenerate rectangle
                return@drawBehind
            }

            val degreesNormalised = (degrees % 360).let { if (it < 0) it + 360 else it }

            val alpha = (degreesNormalised * PI / 180).toFloat()

            val gradientLength =
                when (alpha) {
                    // ray from centre cuts the right edge of the rectangle
                    in 0f..gamma, in (2 * PI - gamma)..2 * PI -> {
                        x / cos(alpha)
                    }

                    // ray from centre cuts the top edge of the rectangle
                    in gamma..(PI - gamma).toFloat() -> {
                        y / sin(alpha)
                    }

                    // ray from centre cuts the left edge of the rectangle
                    in (PI - gamma)..(PI + gamma) -> {
                        x / -cos(alpha)
                    }

                    // ray from centre cuts the bottom edge of the rectangle
                    in (PI + gamma)..(2 * PI - gamma) -> {
                        y / -sin(alpha)
                    }

                    // default case (which shouldn't really happen)
                    else -> {
                        hypot(x, y)
                    }
                }

            val centerOffsetX = cos(alpha) * gradientLength / 2
            val centerOffsetY = sin(alpha) * gradientLength / 2

            drawRect(
                brush =
                    Brush.linearGradient(
                        colorStops = easedColorStops(colors),
                        // negative here so that 0 degrees is left -> right
                        start = Offset(center.x - centerOffsetX, center.y - centerOffsetY),
                        end = Offset(center.x + centerOffsetX, center.y + centerOffsetY),
                    ),
                size = size,
            )
        }
    },
)

// Angle Gradient Background without size
fun GradientOffset(angle: GradientAngle): GradientOffset =
    when (angle) {
        GradientAngle.CW45 -> {
            GradientOffset(
                start = Offset.Zero,
                end = Offset.Infinite,
            )
        }

        GradientAngle.CW90 -> {
            GradientOffset(
                start = Offset.Zero,
                end = Offset(0f, Float.POSITIVE_INFINITY),
            )
        }

        GradientAngle.CW135 -> {
            GradientOffset(
                start = Offset(Float.POSITIVE_INFINITY, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY),
            )
        }

        GradientAngle.CW180 -> {
            GradientOffset(
                start = Offset(Float.POSITIVE_INFINITY, 0f),
                end = Offset.Zero,
            )
        }

        GradientAngle.CW225 -> {
            GradientOffset(
                start = Offset.Infinite,
                end = Offset.Zero,
            )
        }

        GradientAngle.CW270 -> {
            GradientOffset(
                start = Offset(0f, Float.POSITIVE_INFINITY),
                end = Offset.Zero,
            )
        }

        GradientAngle.CW315 -> {
            GradientOffset(
                start = Offset(0f, Float.POSITIVE_INFINITY),
                end = Offset(Float.POSITIVE_INFINITY, 0f),
            )
        }

        else -> {
            GradientOffset(
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, 0f),
            )
        }
    }

/**
 * Offset for [Brush.linearGradient] to rotate gradient depending on [start] and [end] offsets.
 */
data class GradientOffset(
    val start: Offset,
    val end: Offset,
)

enum class GradientAngle {
    CW0,
    CW45,
    CW90,
    CW135,
    CW180,
    CW225,
    CW270,
    CW315,
}

@Composable
expect fun getScreenSizeInfo(): ScreenSizeInfo

@Composable
fun NonLazyGrid(
    columns: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    content:
        @Composable()
        (Int) -> Unit,
) {
    Column(modifier = modifier) {
        var rows = (itemCount / columns)
        if (itemCount.mod(columns) > 0) {
            rows += 1
        }

        for (rowId in 0 until rows) {
            val firstIndex = rowId * columns

            Row {
                for (columnId in 0 until columns) {
                    val index = firstIndex + columnId
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    ) {
                        if (index < itemCount) {
                            content(index)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Apple Music's handover between two lyric lines: the page does not move as one block. The row
 * at the top of the page sets off first and every row below it a little after the one above it,
 * so the spacing opens as the page leaves and closes as it lands. The schedule is AMLL's
 * (`calcLayout` in packages/core/src/lyric-player/base/index.ts): nothing for the first row still
 * on the page, then 50 ms more per row, the step shrinking by 1/1.05 a row once past the sung line.
 *
 * Done inside a LazyColumn the way BitChord does it: the list itself scrolls by the undelayed
 * curve, and each row draws itself back ([lagPx]) by what its own delayed curve has not covered
 * yet. A row with no delay therefore sits still against the list — the line leaving through the
 * top is clipped like any scroll — and a row with one only ever trails downwards, into space the
 * list has already laid out, so nothing pops in at either edge.
 *
 * One handover at a time: a new line ends the running one, and any row still trailing snaps into
 * place. With lines a second or more apart nothing is left to snap; under about 0.7 s apart a few
 * pixels are, mostly on the blurred rows further down.
 */
class LyricsStagger {
    // The curve the list scrolls on AND the one every row trails on: two different curves, and a
    // row with no delay would still drift against the list it sits in.
    //
    // A spring, not a tween. A tween covers a fixed distance in a fixed time, so a one-line step
    // and a six-line jump after a seek both take 650ms — the short one crawls, the long one races.
    // A spring is driven by the distance itself, which is why Apple's page settles the same way
    // whether it moved a little or a lot. Damping just under 1 keeps it soft without bouncing, and
    // low stiffness is what makes it read as gliding rather than snapping into place.
    private val spring =
        FloatSpringSpec(dampingRatio = 0.9f, stiffness = 180f, visibilityThreshold = 0.001f)
    private val springMs = spring.getDurationNanos(0f, 1f, 0f) / 1_000_000f

    // Read inside the rows' graphicsLayer, so a handover re-runs only those layers — draw phase,
    // no recomposition and no layout.
    private var lagDistance by mutableFloatStateOf(0f)
    internal var elapsedMs by mutableFloatStateOf(0f)

    private var firstOnPage = 0
    private var target = -1

    /** How far along the scroll is, 0..1, [ms] into the handover. */
    internal fun progress(ms: Float): Float =
        when {
            ms <= 0f -> 0f
            ms >= springMs -> 1f
            else -> spring.getValueFromNanos((ms * 1_000_000f).toLong(), 0f, 1f, 0f)
        }

    /**
     * Starts a handover of [distance] px to [index]; returns how long until the last row on the
     * page has landed, in ms. Only a step to the NEXT line staggers: a seek, a tapped line or a
     * jump from off screen moves as one block, as AMLL turns the stagger off on a seek.
     */
    internal fun begin(
        layoutInfo: LazyListLayoutInfo,
        index: Int,
        distance: Float,
        wasVisible: Boolean,
    ): Float {
        val staggered = wasVisible && index == target + 1
        target = index
        // The first row still on the page once it lands. AMLL counts the delays from there, at
        // zero, so every row above it — the one about to leave through the top included — moves
        // with the list.
        val pageTop = layoutInfo.viewportStartOffset + distance
        firstOnPage =
            layoutInfo.visibleItemsInfo
                .firstOrNull { it.offset + it.size > pageTop }
                ?.index ?: index
        elapsedMs = 0f
        lagDistance = if (staggered) distance else 0f
        if (!staggered) return springMs
        // Two rows past the bottom: the ones the scroll is about to bring in.
        val lastRow = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: index) + 2
        return springMs + delayMs(lastRow)
    }

    internal fun end() {
        lagDistance = 0f
        elapsedMs = 0f
    }

    /** How far below its laid-out place row [index] is drawn right now, in px. */
    fun lagPx(index: Int): Float {
        val distance = lagDistance
        if (distance == 0f) return 0f
        val delay = delayMs(index)
        if (delay == 0f) return 0f
        // Read only past the early returns: a row with nothing to trail never reads the clock, so
        // a handover invalidates only the layers that actually move against the list.
        val ms = elapsedMs
        return distance * (progress(ms) - progress(ms - delay))
    }

    // AMLL calcLayout: 0 for the first row on the page, then 50 ms more per row; from the sung
    // line down the step shrinks by 1/1.05 a row.
    private fun delayMs(index: Int): Float {
        var delay = 0f
        var step = 50f
        for (row in firstOnPage until index) {
            delay += step
            if (row >= target) step /= 1.05f
        }
        return delay
    }
}

/**
 * Scrolls [index] to the TOP edge of the viewport, the counterpart to
 * [animateScrollAndCentralizeItem]. Used by the Apple Music lyrics style, which anchors the page
 * near the top rather than around the middle.
 *
 * Gentler than the centralising version on purpose: that one repositions a list the user is
 * reading, while this one runs on every lyric line — a fixed 300ms step reads as a jolt once per
 * line, where a spring reads as the page drifting with the song. The one-frame wait and the
 * jump-if-offscreen guard are kept verbatim; both are needed for layoutInfo to hold the target
 * item before its offset is measured.
 *
 * [stagger] lets the rows trail the scroll; see [LyricsStagger].
 */
suspend fun LazyListState.animateScrollAndAnchorItemTop(
    index: Int,
    /**
     * Extra pixels added to the scroll. Negative leaves that much of the PREVIOUS content visible
     * above [index] — used to expose exactly one physical text row, which is not the same as
     * scrolling to the previous item: a lyric line that wraps is one item but several rows.
     */
    extraOffsetPx: Float = 0f,
    stagger: LyricsStagger,
) {
    if (index < 0) return
    val initiallyVisible = this.layoutInfo.visibleItemsInfo.any { it.index == index }
    if (!initiallyVisible) {
        this.scrollToItem(index)
    }
    withFrameNanos { }
    val itemInfo =
        this.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val distance =
        (itemInfo.offset - this.layoutInfo.viewportStartOffset).toFloat() + extraOffsetPx
    val spanMs = stagger.begin(this.layoutInfo, index, distance, initiallyVisible)
    try {
        // animateScrollBy's own loop, but stepped by the clock rather than by the spring, so the
        // rows can read the same clock and run the same curve a little behind it.
        var scrolled = 0f
        scroll {
            val clock = tween<Float>(spanMs.toInt(), easing = LinearEasing)
            animate(0f, spanMs, animationSpec = clock) { ms, _ ->
                stagger.elapsedMs = ms
                scrolled += scrollBy(distance * stagger.progress(ms) - scrolled)
            }
        }
    } finally {
        // Done, or cut short — by the next line, or by a finger taking the list. Either way every
        // row goes back to where the list has it.
        stagger.end()
    }
}

suspend fun LazyListState.animateScrollAndCentralizeItem(index: Int) {
    if (index < 0) return
    // If target item is not currently visible, jump close to it first so layoutInfo updates next frame.
    val initiallyVisible = this.layoutInfo.visibleItemsInfo.any { it.index == index }
    if (!initiallyVisible) {
        this.scrollToItem(index)
    }
    // Wait for one frame so visibleItemsInfo reflects the latest layout pass.
    withFrameNanos { }
    val itemInfo =
        this.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportStart = this.layoutInfo.viewportStartOffset
    val viewportEnd = this.layoutInfo.viewportEndOffset
    val viewportCenter = (viewportStart + viewportEnd) / 2
    val itemCenter = itemInfo.offset + itemInfo.size / 2
    this.animateScrollBy(
        value = (itemCenter - viewportCenter).toFloat(),
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
    )
}

@Composable
expect fun KeepScreenOn()

@Composable
fun LazyListState.isScrollingUp(): State<Boolean> {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }

    LaunchedEffect(Unit) {
        snapshotFlow { layoutInfo.totalItemsCount }.collect {
            Logger.w("isScrollingUp", "firstVisibleItemIndex: $firstVisibleItemIndex")
            previousIndex = firstVisibleItemIndex
            previousScrollOffset = firstVisibleItemScrollOffset
        }
    }

    return remember(this) {
        derivedStateOf {
            if (firstVisibleItemIndex > 0) {
                if (previousIndex != firstVisibleItemIndex) {
                    previousIndex > firstVisibleItemIndex
                } else {
                    previousScrollOffset >= firstVisibleItemScrollOffset
                }.also {
                    previousIndex = firstVisibleItemIndex
                    previousScrollOffset = firstVisibleItemScrollOffset
                }
            } else {
                true
            }
        }
    }
}

@Composable
fun LazyGridState.isScrollingUp(): State<Boolean> {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }

    LaunchedEffect(Unit) {
        snapshotFlow { layoutInfo.totalItemsCount }.collect {
            Logger.w("isScrollingUp", "firstVisibleItemIndex: $firstVisibleItemIndex")
            previousIndex = firstVisibleItemIndex
            previousScrollOffset = firstVisibleItemScrollOffset
        }
    }

    return remember(this) {
        derivedStateOf {
            if (firstVisibleItemIndex > 0) {
                if (previousIndex != firstVisibleItemIndex) {
                    previousIndex > firstVisibleItemIndex
                } else {
                    previousScrollOffset >= firstVisibleItemScrollOffset
                }.also {
                    previousIndex = firstVisibleItemIndex
                    previousScrollOffset = firstVisibleItemScrollOffset
                }
            } else {
                true
            }
        }
    }
}

/**
 * 滚动方向翻转的净位移迟滞门(2026-10-28):库页/主页各列表底部的 footer+锁定 padding 是
 * 双向可滚的"空滚动区",高密度真机上一次手指微抖就能产生 down→up→down 的连续方向翻转
 * 信号,每次翻转驱动液态玻璃底栏 300ms 形变=迷你条/底栏"上下跳动"(用户真机反馈)。
 * 两个具体病灶都能用它吸收:
 * ①贴底微触:canScrollForward 在 snapshotFlow 外读,首帧陈旧 false 把"从最底开始的向上
 *   微滚"硬判成下滚,下一帧纠正回上滚——一次触摸=收起+展开两个形变;
 * ②撞底回吸(bae1c760 的 ⑧ 号护栏针对的场景):净位移 ~10px,远够不到阈值。
 *
 * 设计约束:不回到 3881e85f 的"门控上报"(吞报告把底栏卡死在上一个方向,544a7e99 推翻的
 * 教训)——**同向上报全放行**(底栏即时跟随连续滚动),仅"方向翻转"需要自锚点累计净位移
 * ≥阈值(跨 item 视为立即过阈,一行高度必超);抖动净位移归零永不翻转。消费方
 * (isScrolledToTop/headerAtTop)按值去重,重复同向上报零副作用。
 */
private class ScrollDirectionGate(private val thresholdPx: Int) {
    private var acceptedUp: Boolean? = null
    private var anchorIndex = 0
    private var anchorOffset = 0

    fun report(index: Int, offset: Int, rawUp: Boolean): Boolean {
        val current = acceptedUp
        if (current == null || rawUp == current) {
            acceptedUp = rawUp
            anchorIndex = index
            anchorOffset = offset
            return rawUp
        }
        val netPx = if (index != anchorIndex) thresholdPx + 1 else abs(offset - anchorOffset)
        if (netPx > thresholdPx) {
            acceptedUp = rawUp
            anchorIndex = index
            anchorOffset = offset
            return rawUp
        }
        // 反向未过阈:维持已接受方向(等效于吞掉这次翻转尝试)
        return current
    }
}

/** 方向翻转需要的净滚动位移:约一行封面的高度,真滚动一帧即过、手指抖动永远够不到。 */
private val ScrollDirectionFlipThreshold = 20.dp

/**
 * 库页/主页各滚动页共用的上报效果(驱动顶栏"透明↔模糊条"状态 + App 底栏 isScrolledToTop)。
 *
 * 信号粒度=每次滚动位移(index+offset),方向内联计算,不用 index-only 流:贴底微调
 * 常落在同一 item 内(拿不到 index 变化=零上报,底栏/标题卡死在上一个方向),且
 * snapshotFlow 对 index 跳变做 conflated 后方向推导会读到陈旧 previous(2026-10-03
 * 实测"下滚后迷你条不再沉到导航行"两案)。防振荡不靠吞报告,靠切断几何燃料:各
 * chip 列表底部 padding 锁定展开态高度(见 LibraryScreen lockedBottomPadding),底栏
 * 形变不再改列表几何。index<=1 视作在顶(各页原口径,位置驱动不吃迟滞)。深处保存态
 * 恢复时首帧 index==previous、offset 相等→上报 false(收起),与 CR-30"深处首帧上报
 * 即 false=正确语义"一致。
 *
 * 边界防误判三层:
 * ①canScrollForward=false(已到底)时同项 offset 递减不判上滑——fling 撞到列表末端被
 *   拦截时最后一项的 offset 会被钳制值回吸(逐帧递减,实测 105→96),裸读成"上滑"会让
 *   底栏刚收起又跳回展开(bae1c760);
 * ②方向翻转经 [ScrollDirectionGate] 净位移迟滞——贴底首帧 canScrollForward 陈旧值把
 *   向上微滚误判成下滚、下一帧又纠正(2026-10-28 用户真机"很小的一个触摸触发迷你条/
 *   底栏上下跳动"),以及一切快速连续翻转信号,净位移不够一律维持原方向;
 * ③顶部对称处理由 index<=1 分支兜住(canScrollBackward=false 时必在 index<=1)。
 */
@Composable
fun LazyListState.scrollReportingEffect(onScrolling: (onTop: Boolean) -> Unit) {
    val thresholdPx = with(LocalDensity.current) { ScrollDirectionFlipThreshold.toPx().toInt() }
    LaunchedEffect(this) {
        val gate = ScrollDirectionGate(thresholdPx)
        var previousIndex = firstVisibleItemIndex
        var previousOffset = firstVisibleItemScrollOffset
        snapshotFlow { firstVisibleItemIndex to firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val rawUp =
                    if (index != previousIndex) {
                        index < previousIndex
                    } else if (!canScrollForward) {
                        // 已到底:同项 offset 递减=末端钳制回吸,不是上滑
                        false
                    } else {
                        offset < previousOffset
                    }
                previousIndex = index
                previousOffset = offset
                if (index <= 1) {
                    // 位置驱动不吃迟滞;同步门内方向,离顶后首个反向不因陈旧锚点误判
                    gate.report(index, offset, true)
                    onScrolling(true)
                } else {
                    onScrolling(gate.report(index, offset, rawUp))
                }
            }
    }
}

@Composable
fun LazyGridState.scrollReportingEffect(onScrolling: (onTop: Boolean) -> Unit) {
    val thresholdPx = with(LocalDensity.current) { ScrollDirectionFlipThreshold.toPx().toInt() }
    LaunchedEffect(this) {
        val gate = ScrollDirectionGate(thresholdPx)
        var previousIndex = firstVisibleItemIndex
        var previousOffset = firstVisibleItemScrollOffset
        snapshotFlow { firstVisibleItemIndex to firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val rawUp =
                    if (index != previousIndex) {
                        index < previousIndex
                    } else if (!canScrollForward) {
                        // 已到底:同项 offset 递减=末端钳制回吸,不是上滑(见 List 版注释)
                        false
                    } else {
                        offset < previousOffset
                    }
                previousIndex = index
                previousOffset = offset
                if (index <= 1) {
                    gate.report(index, offset, true)
                    onScrolling(true)
                } else {
                    onScrolling(gate.report(index, offset, rawUp))
                }
            }
    }
}

fun Palette?.getColorFromPalette(): Color {
    val p = this ?: return Color.Black
    val defaultColor = 0x000000
    var startColor = p.getDarkVibrantColor(defaultColor)
    if (startColor == defaultColor) {
        startColor = p.getDarkMutedColor(defaultColor)
        if (startColor == defaultColor) {
            startColor = p.getVibrantColor(defaultColor)
            if (startColor == defaultColor) {
                startColor =
                    p.getMutedColor(defaultColor)
                if (startColor == defaultColor) {
                    startColor =
                        p.getLightVibrantColor(
                            defaultColor,
                        )
                    if (startColor == defaultColor) {
                        startColor =
                            p.getLightMutedColor(
                                defaultColor,
                            )
                    }
                }
            }
        }
    }
    return Color(startColor)
}

/**
 * Apple-Music-style immersive page background derived from the artwork.
 * Uses the DOMINANT swatch (overall tone of the image, by pixel area) instead of a vivid Muted
 * swatch — so a light image yields a light-ish tone rather than a small saturated patch (e.g. a
 * bright portrait no longer turns brick-red from the hair/skin). Then darkens adaptively: the
 * lighter the source, the harder it is pulled toward black, so white text stays readable on any
 * artwork.
 */
fun Palette?.toImmersiveBackground(): Color {
    val p = this ?: return Color.Black
    val rgb =
        p.getDominantColor(0).takeIf { it != 0 }
            ?: p.getMutedColor(0).takeIf { it != 0 }
            ?: p.getVibrantColor(0).takeIf { it != 0 }
            ?: return Color.Black
    val base = Color(rgb)
    // Perceived luminance (0 dark .. 1 light) of the source swatch.
    val luminance = 0.299f * base.red + 0.587f * base.green + 0.114f * base.blue
    // Darken more for lighter artwork so the page stays dark enough for white text.
    val darkenFactor = 0.35f + 0.45f * luminance
    return androidx.compose.ui.graphics.lerp(base, Color.Black, darkenFactor)
}

/**
 * A colour read off an artwork [palette] ([pick], the immersive page background by default), held
 * and faded instead of snapped.
 *
 * kmpalette reads `palette` as null for the whole of every generation (it sets Loading before it
 * suspends), so reading it straight paints [fallback] each time and then jumps to the colour. The
 * last colour that resolved is held instead, and every real change fades over Home's 500ms. Held
 * saveably, so coming back to a page starts on its colour rather than fading in again.
 */
@Composable
fun rememberPaletteColor(
    palette: Palette?,
    fallback: Color = Color.Black,
    pick: (Palette) -> Color = { it.toImmersiveBackground() },
): Color {
    var held by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(palette) { palette?.let { held = pick(it).toArgb() } }
    return animateColorAsState(held?.let { Color(it) } ?: fallback, tween(500), label = "paletteColor").value
}

/**
 * The frosted top bar of the immersive screens: their own page colour behind a 24dp blur, washed
 * with that colour again at [tintAlpha]. Where haze does not blur (Android 11 and below), the bar is
 * that colour, opaque.
 *
 * Blur is left at haze's default (Android 12+ and Desktop) on purpose. Forcing it on sends older
 * Android through haze's RenderScript blur, which crashes natively (SIGSEGV in
 * `GrallocConsumer::lockNextBuffer`) while one blurred screen replaces another, Library into the
 * four tiles screen above all. A native crash leaves haze no chance to fall back.
 *
 * A plain function, not remembered: `HazeBlurStyle { }` records its writes into a list and compares
 * by them, so an unchanged tint recomposes into an equal Style and leaves the node alone.
 */
fun barBlurStyle(
    tint: Color,
    tintAlpha: Float,
): HazeBlurStyle =
    HazeBlurStyle {
        blurRadius(24.dp)
        backgroundColor(tint)
        colorEffects(listOf(HazeColorEffect.tint(tint.copy(alpha = tintAlpha))))
        fallbackColorEffect(HazeColorEffect.tint(tint.copy(alpha = 1f)))
    }

/** [HazeMaterials.ultraThin] for the plain top bars, with the same opaque fallback as [barBlurStyle]. */
@Composable
fun ultraThinBarStyle(): HazeBlurStyle {
    val surface = MaterialTheme.colorScheme.surface
    return HazeMaterials.ultraThin(surface).then { fallbackColorEffect(HazeColorEffect.tint(surface)) }
}

/**
 * Vertical scrim from [from] to [to] that fades without showing an edge.
 *
 * Two details are what make this read as smooth where a plain
 * `verticalGradient(Transparent, bg)` does not:
 *  - **smoothstep easing**, so the curve is flat at BOTH ends. A linear ramp has a corner where it
 *    leaves 0, and the eye tracks the derivative of brightness — that corner IS the visible edge.
 *  - **the colours are interpolated here, not by Skia**, so a caller can pass
 *    `color.copy(alpha = 0f)` instead of [Color.Transparent]. Transparent is *black* with alpha 0,
 *    and Skia interpolates stops un-premultiplied, so RGB gets dragged toward black alongside the
 *    alpha and the middle of the scrim turns into a dirty grey band.
 *
 * [steps] stops (rather than the 2 the curve needs) keep the piecewise-linear approximation of the
 * S-curve below the point where 8-bit banding becomes visible on a dark background.
 *
 * [startFraction]/[endFraction] confine the ramp to part of the box (the rest is held at [from] /
 * [to] by [TileMode.Clamp]); [startY]/[endY] do the same in pixels when the caller knows them.
 */
fun smoothScrimBrush(
    from: Color,
    to: Color,
    startFraction: Float = 0f,
    endFraction: Float = 1f,
    startY: Float = 0f,
    endY: Float = Float.POSITIVE_INFINITY,
    steps: Int = 24,
): Brush =
    Brush.verticalGradient(
        colorStops =
            Array(steps + 1) { i ->
                val t = i / steps.toFloat()
                val position = startFraction + (endFraction - startFraction) * t
                position to androidx.compose.ui.graphics.lerp(from, to, t * t * (3f - 2f * t))
            },
        startY = startY,
        endY = endY,
    )

/**
 * The common case of [smoothScrimBrush]: a bottom scrim that melts artwork into the page
 * background ([toImmersiveBackground]) by ramping [color] from invisible to opaque.
 * Must stay a pure fade to [color] — page-level glow layers (home first shelf,
 * [AmbientThemeGlow], Mix for you, Listen together) end on the page colour, so any
 * darkening here reads as a gray veil with a hard seam where the layer stops.
 * White text over the artwork itself wants [artworkTextScrimBrush] instead.
 */
fun artworkScrimBrush(
    color: Color,
    steps: Int = 24,
): Brush =
    smoothScrimBrush(
        from = color.copy(alpha = 0f),
        to = color,
        steps = steps,
    )

/**
 * [artworkScrimBrush] variant for headers overlaying white text on the artwork: the ramp
 * ends 45% toward black so bright covers (netease playlists skew light) still bottom out
 * dark enough for the title/author line.
 */
fun artworkTextScrimBrush(
    color: Color,
    steps: Int = 24,
): Brush =
    smoothScrimBrush(
        from = color.copy(alpha = 0f),
        to = lerp(color, Color.Black, 0.45f),
        steps = steps,
    )

fun Modifier.isElementVisible(onVisibilityChanged: (Boolean) -> Unit) =
    composed {
        val isVisible by remember { derivedStateOf { mutableStateOf(false) } }
        LaunchedEffect(isVisible.value) { onVisibilityChanged.invoke(isVisible.value) }
        this.onGloballyPositioned { layoutCoordinates ->
            isVisible.value = layoutCoordinates.parentLayoutCoordinates?.let {
                val parentBounds = it.boundsInWindow()
                val childBounds = layoutCoordinates.boundsInWindow()
                parentBounds.overlaps(childBounds)
            } == true
        }
    }

fun Color.rgbFactor(factor: Float): Color {
    val r = min(red * factor, 255f)
    val g = min(green * factor, 255f)
    val b = min(blue * factor, 255f)
    return Color(r, g, b, alpha)
}

/**
 * Parse a hex color string ("ad1e5d", "#ad1e5d", or 8-digit "aarrggbb") into a Compose [Color].
 * Returns null on malformed input.
 */
fun String.hexToColorOrNull(): Color? =
    runCatching {
        val clean = removePrefix("#")
        val argb =
            when (clean.length) {
                6 -> 0xFF000000L or clean.toLong(16)
                8 -> clean.toLong(16)
                else -> return null
            }
        Color(argb)
    }.getOrNull()

fun TextStyle.greyScale(): TextStyle =
    this.copy(
        color = Color.Gray,
    )

@Composable
expect fun rememberIsInPipMode(): Boolean

@Composable
fun animateAlignmentAsState(targetAlignment: Alignment): State<Alignment> {
    val biased = targetAlignment as BiasAlignment
    val horizontal by animateFloatAsState(biased.horizontalBias)
    val vertical by animateFloatAsState(biased.verticalBias)
    return remember { derivedStateOf { BiasAlignment(horizontal, vertical) } }
}

@Composable
fun PaddingValues.copy(
    start: Dp? = null,
    top: Dp? = null,
    end: Dp? = null,
    bottom: Dp? = null,
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = start ?: this.calculateStartPadding(layoutDirection),
        top = top ?: this.calculateTopPadding(),
        end = end ?: this.calculateEndPadding(layoutDirection),
        bottom = bottom ?: this.calculateBottomPadding(),
    )
}

/**
 * Lays [content] out left-to-right even in a right-to-left language. For playback controls, seek
 * bars and anything else that shows which way time runs: those follow the timeline, not the
 * reading direction, so ⏮ stays left of ⏭ and a progress bar fills from the left.
 */
@Composable
fun LeftToRight(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}

fun ImageBitmap.toResizedBitmap(
    width: Int,
    height: Int,
): ImageBitmap {
    val resized = ImageBitmap(width, height)
    val canvas = Canvas(resized)
    canvas.drawImageRect(
        image = this,
        dstSize = IntSize(width, height),
        paint = Paint(),
    )
    return resized
}

fun getStringBlocking(res: StringResource): String =
    runBlocking {
        getString(res)
    }

/** Converts HSV (hue 0-360, saturation 0-1, value 0-1) to Compose Color. */
fun hsvToColor(
    hue: Float,
    saturation: Float,
    value: Float,
): Color {
    val c = value * saturation
    val x = c * (1 - abs((hue / 60f) % 2f - 1f))
    val m = value - c
    val (r, g, b) =
        when {
            hue < 60f -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
        alpha = 1f,
    )
}