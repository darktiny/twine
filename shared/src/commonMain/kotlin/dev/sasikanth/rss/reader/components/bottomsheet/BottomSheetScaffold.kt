/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.sasikanth.rss.reader.components.bottomsheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.DrawerDefaults
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FabPosition
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalDrawer
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.contentColorFor
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.sasikanth.rss.reader.components.bottomsheet.AnchoredDraggableState.AnchorChangedCallback
import dev.sasikanth.rss.reader.components.bottomsheet.BottomSheetValue.Collapsed
import dev.sasikanth.rss.reader.components.bottomsheet.BottomSheetValue.Expanded
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmName
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Iterates through a [List] using the index and calls [action] for each item. This does not
 * allocate an iterator like [Iterable.forEach].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
  contract { callsInPlace(action) }
  for (index in indices) {
    val item = get(index)
    action(item)
  }
}

// TODO: should be fastMaxByOrNull to match stdlib
/**
 * Returns the first element yielding the largest value of the given function or `null` if there are
 * no elements.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
inline fun <T, R : Comparable<R>> List<T>.fastMaxBy(selector: (T) -> R): T? {
  contract { callsInPlace(selector) }
  if (isEmpty()) return null
  var maxElem = get(0)
  var maxValue = selector(maxElem)
  for (i in 1..lastIndex) {
    val e = get(i)
    val v = selector(e)
    if (maxValue < v) {
      maxElem = e
      maxValue = v
    }
  }
  return maxElem
}

/** Possible values of [BottomSheetState]. */
@ExperimentalMaterialApi
enum class BottomSheetValue {
  /** The bottom sheet is visible, but only showing its peek height. */
  Collapsed,

  /** The bottom sheet is visible at its maximum height. */
  Expanded
}

@Deprecated(
  message =
    "This constructor is deprecated. confirmStateChange has been renamed to " +
      "confirmValueChange.",
  replaceWith =
    ReplaceWith("BottomSheetScaffoldState(initialValue, animationSpec, " + "confirmStateChange)")
)
@Suppress("Deprecation")
@ExperimentalMaterialApi
fun BottomSheetScaffoldState(
  initialValue: BottomSheetValue,
  animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
  confirmStateChange: (BottomSheetValue) -> Boolean
) =
  BottomSheetState(
    initialValue = initialValue,
    animationSpec = animationSpec,
    confirmValueChange = confirmStateChange
  )

/**
 * State of the persistent bottom sheet in [BottomSheetScaffold].
 *
 * @param initialValue The initial value of the state.
 * @param density The density that this state can use to convert values to and from dp.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Suppress("Deprecation")
@ExperimentalMaterialApi
@Stable
fun BottomSheetState(
  initialValue: BottomSheetValue,
  density: Density,
  animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
  confirmValueChange: (BottomSheetValue) -> Boolean = { true }
) = BottomSheetState(initialValue, animationSpec, confirmValueChange).also { it.density = density }

/**
 * State of the persistent bottom sheet in [BottomSheetScaffold].
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@ExperimentalMaterialApi
@Stable
class BottomSheetState
@Deprecated(
  "This constructor is deprecated. Density must be provided by the component. " +
    "Please use the constructor that provides a [Density].",
  ReplaceWith(
    """
            BottomSheetState(
                initialValue = initialValue,
                density = LocalDensity.current,
                animationSpec = animationSpec,
                confirmValueChange = confirmValueChange
            )
            """
  )
)
constructor(
  initialValue: BottomSheetValue,
  animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
  confirmValueChange: (BottomSheetValue) -> Boolean = { true }
) {

  internal val anchoredDraggableState =
    AnchoredDraggableState(
      initialValue = initialValue,
      animationSpec = animationSpec,
      confirmValueChange = confirmValueChange,
      positionalThreshold = {
        with(requireDensity()) { BottomSheetScaffoldPositionalThreshold.toPx() }
      },
      velocityThreshold = { with(requireDensity()) { BottomSheetScaffoldVelocityThreshold.toPx() } }
    )

  val currentValue: BottomSheetValue
    get() = anchoredDraggableState.currentValue

  val targetValue: BottomSheetValue
    get() = anchoredDraggableState.targetValue

  /** Whether the bottom sheet is expanded. */
  val isExpanded: Boolean
    get() = anchoredDraggableState.currentValue == Expanded

  /** Whether the bottom sheet is collapsed. */
  val isCollapsed: Boolean
    get() = anchoredDraggableState.currentValue == Collapsed

  /**
   * The fraction of the progress going from [currentValue] to the targetValue, within [0f..1f]
   * bounds, or 1f if the sheet is in a settled state.
   */
  /*@FloatRange(from = 0f, to = 1f)*/
  val progress: Float
    get() = anchoredDraggableState.progress

  val offsetProgress: Float by derivedStateOf {
    val maxOffset = anchoredDraggableState.maxOffset
    val minOffset = anchoredDraggableState.minOffset
    val currentOffset = anchoredDraggableState.offset

    val delta = maxOffset - minOffset
    if (!currentOffset.isNaN()) {
      ((maxOffset - anchoredDraggableState.requireOffset()) / delta).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f
      )
    } else {
      0f
    }
  }

  /**
   * Expand the bottom sheet with an animation and suspend until the animation finishes or is
   * cancelled. Note: If the peek height is equal to the sheet height, this method will animate to
   * the [Collapsed] state.
   *
   * This method will throw [CancellationException] if the animation is interrupted.
   */
  suspend fun expand() {
    val target = if (anchoredDraggableState.hasAnchorForValue(Expanded)) Expanded else Collapsed
    anchoredDraggableState.animateTo(target)
  }

  /**
   * Collapse the bottom sheet with animation and suspend until it if fully collapsed or animation
   * has been cancelled. This method will throw [CancellationException] if the animation is
   * interrupted.
   */
  suspend fun collapse() = anchoredDraggableState.animateTo(Collapsed)

  @Deprecated(
    message = "Use requireOffset() to access the offset.",
    replaceWith = ReplaceWith("requireOffset()")
  )
  val offset: Float
    get() = error("Use requireOffset() to access the offset.")

  /**
   * Require the current offset.
   *
   * @throws IllegalStateException If the offset has not been initialized yet
   */
  fun requireOffset() = anchoredDraggableState.requireOffset()

  internal suspend fun animateTo(
    target: BottomSheetValue,
    velocity: Float = anchoredDraggableState.lastVelocity
  ) = anchoredDraggableState.animateTo(target, velocity)

  internal suspend fun snapTo(target: BottomSheetValue) = anchoredDraggableState.snapTo(target)

  internal fun trySnapTo(target: BottomSheetValue) = anchoredDraggableState.trySnapTo(target)

  internal val isAnimationRunning: Boolean
    get() = anchoredDraggableState.isAnimationRunning

  internal var density: Density? = null

  private fun requireDensity() =
    requireNotNull(density) {
      "The density on BottomSheetState ($this) was not set. Did you use BottomSheetState with " +
        "the BottomSheetScaffold composable?"
    }

  internal val lastVelocity: Float
    get() = anchoredDraggableState.lastVelocity

  companion object {

    /** The default [Saver] implementation for [BottomSheetState]. */
    fun Saver(
      animationSpec: AnimationSpec<Float>,
      confirmStateChange: (BottomSheetValue) -> Boolean,
      density: Density
    ): Saver<BottomSheetState, *> =
      Saver(
        save = { it.anchoredDraggableState.currentValue },
        restore = {
          BottomSheetState(
            initialValue = it,
            density = density,
            animationSpec = animationSpec,
            confirmValueChange = confirmStateChange
          )
        }
      )

    /** The default [Saver] implementation for [BottomSheetState]. */
    @Deprecated(
      message =
        "This function is deprecated. Please use the overload where Density is" + " provided.",
      replaceWith = ReplaceWith("Saver(animationSpec, confirmStateChange, density)")
    )
    @Suppress("Deprecation")
    fun Saver(
      animationSpec: AnimationSpec<Float>,
      confirmStateChange: (BottomSheetValue) -> Boolean
    ): Saver<BottomSheetState, *> =
      Saver(
        save = { it.anchoredDraggableState.currentValue },
        restore = {
          BottomSheetState(
            initialValue = it,
            animationSpec = animationSpec,
            confirmValueChange = confirmStateChange
          )
        }
      )
  }
}

/**
 * Create a [BottomSheetState] and [remember] it.
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterialApi
fun rememberBottomSheetState(
  initialValue: BottomSheetValue,
  animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
  confirmStateChange: (BottomSheetValue) -> Boolean = { true }
): BottomSheetState {
  val density = LocalDensity.current
  return rememberSaveable(
    animationSpec,
    saver =
      BottomSheetState.Saver(
        animationSpec = animationSpec,
        confirmStateChange = confirmStateChange,
        density = density
      )
  ) {
    BottomSheetState(
      initialValue = initialValue,
      animationSpec = animationSpec,
      confirmValueChange = confirmStateChange,
      density = density
    )
  }
}

/**
 * State of the [BottomSheetScaffold] composable.
 *
 * @param drawerState The state of the navigation drawer.
 * @param bottomSheetState The state of the persistent bottom sheet.
 * @param snackbarHostState The [SnackbarHostState] used to show snackbars inside the scaffold.
 */
@ExperimentalMaterialApi
@Stable
class BottomSheetScaffoldState(
  val drawerState: DrawerState,
  val bottomSheetState: BottomSheetState,
  val snackbarHostState: SnackbarHostState
)

/**
 * Create and [remember] a [BottomSheetScaffoldState].
 *
 * @param drawerState The state of the navigation drawer.
 * @param bottomSheetState The state of the persistent bottom sheet.
 * @param snackbarHostState The [SnackbarHostState] used to show snackbars inside the scaffold.
 */
@Composable
@ExperimentalMaterialApi
fun rememberBottomSheetScaffoldState(
  drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
  bottomSheetState: BottomSheetState = rememberBottomSheetState(Collapsed),
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): BottomSheetScaffoldState {
  return remember(drawerState, bottomSheetState, snackbarHostState) {
    BottomSheetScaffoldState(
      drawerState = drawerState,
      bottomSheetState = bottomSheetState,
      snackbarHostState = snackbarHostState
    )
  }
}

/**
 * <a href="https://material.io/components/sheets-bottom#standard-bottom-sheet" class="external"
 * target="_blank">Material Design standard bottom sheet</a>.
 *
 * Standard bottom sheets co-exist with the screen’s main UI region and allow for simultaneously
 * viewing and interacting with both regions. They are commonly used to keep a feature or secondary
 * content visible on screen when content in main UI region is frequently scrolled or panned.
 *
 * ![Standard bottom sheet
 * image](https://developer.android.com/images/reference/androidx/compose/material/standard-bottom-sheet.png)
 *
 * This component provides an API to put together several material components to construct your
 * screen. For a similar component which implements the basic material design layout strategy with
 * app bars, floating action buttons and navigation drawers, use the standard [Scaffold]. For
 * similar component that uses a backdrop as the centerpiece of the screen, use [BackdropScaffold].
 *
 * A simple example of a bottom sheet scaffold looks like this:
 *
 * @param sheetContent The content of the bottom sheet.
 * @param modifier An optional [Modifier] for the root of the scaffold.
 * @param scaffoldState The state of the scaffold.
 * @param topBar An optional top app bar.
 * @param snackbarHost The composable hosting the snackbars shown inside the scaffold.
 * @param floatingActionButton An optional floating action button.
 * @param floatingActionButtonPosition The position of the floating action button.
 * @param sheetGesturesEnabled Whether the bottom sheet can be interacted with by gestures.
 * @param sheetShape The shape of the bottom sheet.
 * @param sheetElevation The elevation of the bottom sheet.
 * @param sheetBackgroundColor The background color of the bottom sheet.
 * @param sheetContentColor The preferred content color provided by the bottom sheet to its
 *   children. Defaults to the matching content color for [sheetBackgroundColor], or if that is not
 *   a color from the theme, this will keep the same content color set above the bottom sheet.
 * @param sheetPeekHeight The height of the bottom sheet when it is collapsed. If the peek height
 *   equals the sheet's full height, the sheet will only have a collapsed state.
 * @param drawerContent The content of the drawer sheet.
 * @param drawerGesturesEnabled Whether the drawer sheet can be interacted with by gestures.
 * @param drawerShape The shape of the drawer sheet.
 * @param drawerElevation The elevation of the drawer sheet.
 * @param drawerBackgroundColor The background color of the drawer sheet.
 * @param drawerContentColor The preferred content color provided by the drawer sheet to its
 *   children. Defaults to the matching content color for [drawerBackgroundColor], or if that is not
 *   a color from the theme, this will keep the same content color set above the drawer sheet.
 * @param drawerScrimColor The color of the scrim that is applied when the drawer is open.
 * @param content The main content of the screen. You should use the provided [PaddingValues] to
 *   properly offset the content, so that it is not obstructed by the bottom sheet when collapsed.
 * @sample androidx.compose.material.samples.BottomSheetScaffoldSample
 */
@Composable
@ExperimentalMaterialApi
fun BottomSheetScaffold(
  sheetContent: @Composable ColumnScope.() -> Unit,
  modifier: Modifier = Modifier,
  scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
  topBar: (@Composable () -> Unit)? = null,
  snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
  floatingActionButton: (@Composable () -> Unit)? = null,
  floatingActionButtonPosition: FabPosition = FabPosition.End,
  sheetGesturesEnabled: Boolean = true,
  sheetShape: Shape = MaterialTheme.shapes.large,
  sheetElevation: Dp = BottomSheetScaffoldDefaults.SheetElevation,
  sheetBackgroundColor: Color = MaterialTheme.colors.surface,
  sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
  sheetPeekHeight: Dp = BottomSheetScaffoldDefaults.SheetPeekHeight,
  drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
  drawerGesturesEnabled: Boolean = true,
  drawerShape: Shape = MaterialTheme.shapes.large,
  drawerElevation: Dp = DrawerDefaults.Elevation,
  drawerBackgroundColor: Color = MaterialTheme.colors.surface,
  drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
  drawerScrimColor: Color = DrawerDefaults.scrimColor,
  backgroundColor: Color = MaterialTheme.colors.background,
  contentColor: Color = contentColorFor(backgroundColor),
  content: @Composable (PaddingValues) -> Unit
) {
  // b/278692145 Remove this once deprecated methods without density are removed
  if (scaffoldState.bottomSheetState.density == null) {
    val density = LocalDensity.current
    SideEffect { scaffoldState.bottomSheetState.density = density }
  }

  val peekHeightPx =
    with(LocalDensity.current) { sheetPeekHeight.toPx() + WindowInsets.systemBars.getBottom(this) }
  val child =
    @Composable {
      BottomSheetScaffoldLayout(
        topBar = topBar,
        body = content,
        bottomSheet = { layoutHeight ->
          val nestedScroll =
            if (sheetGesturesEnabled) {
              Modifier.nestedScroll(
                remember(scaffoldState.bottomSheetState.anchoredDraggableState) {
                  ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                    state = scaffoldState.bottomSheetState.anchoredDraggableState,
                    orientation = Orientation.Vertical
                  )
                }
              )
            } else Modifier
          BottomSheet(
            state = scaffoldState.bottomSheetState,
            modifier =
              nestedScroll
                .fillMaxWidth()
                .requiredHeightIn(min = sheetPeekHeight)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
            calculateAnchors = { sheetSize ->
              val sheetHeight = sheetSize.height.toFloat()
              val collapsedHeight = layoutHeight - peekHeightPx
              if (sheetHeight == 0f || sheetHeight == peekHeightPx) {
                mapOf(Collapsed to collapsedHeight)
              } else {
                mapOf(Collapsed to collapsedHeight, Expanded to layoutHeight - sheetHeight)
              }
            },
            sheetBackgroundColor = sheetBackgroundColor,
            sheetContentColor = sheetContentColor,
            sheetElevation = sheetElevation,
            sheetGesturesEnabled = sheetGesturesEnabled,
            sheetShape = sheetShape,
            content = sheetContent
          )
        },
        floatingActionButton = floatingActionButton,
        snackbarHost = { snackbarHost(scaffoldState.snackbarHostState) },
        sheetOffset = { scaffoldState.bottomSheetState.requireOffset() },
        sheetPeekHeight = sheetPeekHeight,
        sheetState = scaffoldState.bottomSheetState,
        floatingActionButtonPosition = floatingActionButtonPosition
      )
    }
  Surface(modifier.fillMaxSize(), color = backgroundColor, contentColor = contentColor) {
    if (drawerContent == null) {
      child()
    } else {
      ModalDrawer(
        drawerContent = drawerContent,
        drawerState = scaffoldState.drawerState,
        gesturesEnabled = drawerGesturesEnabled,
        drawerShape = drawerShape,
        drawerElevation = drawerElevation,
        drawerBackgroundColor = drawerBackgroundColor,
        drawerContentColor = drawerContentColor,
        scrimColor = drawerScrimColor,
        content = child
      )
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheet(
  state: BottomSheetState,
  sheetGesturesEnabled: Boolean,
  calculateAnchors: (sheetSize: IntSize) -> Map<BottomSheetValue, Float>,
  sheetShape: Shape,
  sheetElevation: Dp,
  sheetBackgroundColor: Color,
  sheetContentColor: Color,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  val scope = rememberCoroutineScope()
  val anchorChangeCallback =
    remember(state, scope) { BottomSheetScaffoldAnchorChangeCallback(state, scope) }
  Surface(
    modifier
      .anchoredDraggable(
        state = state.anchoredDraggableState,
        orientation = Orientation.Vertical,
        enabled = sheetGesturesEnabled,
      )
      .onSizeChanged { layoutSize ->
        state.anchoredDraggableState.updateAnchors(
          newAnchors = calculateAnchors(layoutSize),
          onAnchorsChanged = anchorChangeCallback
        )
      }
      .semantics {
        // If we don't have anchors yet, or have only one anchor we don't want any
        // accessibility actions
        if (state.anchoredDraggableState.anchors.size > 1) {
          if (state.isCollapsed) {
            expand {
              if (state.anchoredDraggableState.confirmValueChange(Expanded)) {
                scope.launch { state.expand() }
              }
              true
            }
          } else {
            collapse {
              if (state.anchoredDraggableState.confirmValueChange(Collapsed)) {
                scope.launch { state.collapse() }
              }
              true
            }
          }
        }
      },
    shape = sheetShape,
    elevation = sheetElevation,
    color = sheetBackgroundColor,
    contentColor = sheetContentColor,
    content = { Column(content = content) }
  )
}

/** Contains useful defaults for [BottomSheetScaffold]. */
object BottomSheetScaffoldDefaults {
  /** The default elevation used by [BottomSheetScaffold]. */
  val SheetElevation = 8.dp

  /** The default peek height used by [BottomSheetScaffold]. */
  val SheetPeekHeight = 56.dp
}

private enum class BottomSheetScaffoldLayoutSlot {
  TopBar,
  Body,
  Sheet,
  Fab,
  Snackbar
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetScaffoldLayout(
  topBar: @Composable (() -> Unit)?,
  body: @Composable (innerPadding: PaddingValues) -> Unit,
  bottomSheet: @Composable (layoutHeight: Int) -> Unit,
  floatingActionButton: (@Composable () -> Unit)?,
  snackbarHost: @Composable () -> Unit,
  sheetPeekHeight: Dp,
  floatingActionButtonPosition: FabPosition,
  sheetOffset: () -> Float,
  sheetState: BottomSheetState,
) {
  SubcomposeLayout { constraints ->
    val layoutWidth = constraints.maxWidth
    val layoutHeight = constraints.maxHeight
    val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

    val sheetPlaceables =
      subcompose(BottomSheetScaffoldLayoutSlot.Sheet) { bottomSheet(layoutHeight) }
        .map { it.measure(looseConstraints) }
    val sheetOffsetY = sheetOffset().roundToInt()

    val topBarPlaceables =
      topBar?.let {
        subcompose(BottomSheetScaffoldLayoutSlot.TopBar, topBar).map {
          it.measure(looseConstraints)
        }
      }
    val topBarHeight = topBarPlaceables?.fastMaxBy { it.height }?.height ?: 0

    val bodyConstraints = looseConstraints.copy(maxHeight = layoutHeight - topBarHeight)
    val bodyPlaceables =
      subcompose(BottomSheetScaffoldLayoutSlot.Body) {
          body(PaddingValues(bottom = sheetPeekHeight))
        }
        .map { it.measure(bodyConstraints) }

    val fabPlaceable =
      floatingActionButton?.let { fab ->
        subcompose(BottomSheetScaffoldLayoutSlot.Fab, fab).map { it.measure(looseConstraints) }
      }
    val fabWidth = fabPlaceable?.fastMaxBy { it.width }?.width ?: 0
    val fabHeight = fabPlaceable?.fastMaxBy { it.height }?.height ?: 0
    val fabOffsetX =
      when (floatingActionButtonPosition) {
        FabPosition.Center -> (layoutWidth - fabWidth) / 2
        else -> layoutWidth - fabWidth - FabSpacing.roundToPx()
      }
    // In case sheet peek height < (FAB height / 2), give the FAB some minimum space
    val fabOffsetY =
      if (sheetPeekHeight.toPx() < fabHeight / 2) {
        sheetOffsetY - fabHeight - FabSpacing.roundToPx()
      } else sheetOffsetY - (fabHeight / 2)

    val snackbarPlaceables =
      subcompose(BottomSheetScaffoldLayoutSlot.Snackbar, snackbarHost).map {
        it.measure(looseConstraints)
      }
    val snackbarWidth = snackbarPlaceables.fastMaxBy { it.width }?.width ?: 0
    val snackbarHeight = snackbarPlaceables.fastMaxBy { it.height }?.height ?: 0
    val snackbarOffsetX = (layoutWidth - snackbarWidth) / 2
    val snackbarOffsetY =
      when (sheetState.currentValue) {
        Collapsed -> fabOffsetY - snackbarHeight
        Expanded -> layoutHeight - snackbarHeight
      }
    layout(layoutWidth, layoutHeight) {
      // Placement order is important for elevation
      bodyPlaceables.fastForEach { it.placeRelative(0, topBarHeight) }
      topBarPlaceables?.fastForEach { it.placeRelative(0, 0) }
      sheetPlaceables.fastForEach { it.placeRelative(0, sheetOffsetY) }
      fabPlaceable?.fastForEach { it.placeRelative(fabOffsetX, fabOffsetY) }
      snackbarPlaceables.fastForEach { it.placeRelative(snackbarOffsetX, snackbarOffsetY) }
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
private fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
  state: AnchoredDraggableState<*>,
  orientation: Orientation
): NestedScrollConnection =
  object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
      val delta = available.toFloat()
      return if (delta < 0 && source == NestedScrollSource.Drag) {
        state.dispatchRawDelta(delta).toOffset()
      } else {
        Offset.Zero
      }
    }

    override fun onPostScroll(
      consumed: Offset,
      available: Offset,
      source: NestedScrollSource
    ): Offset {
      return if (source == NestedScrollSource.Drag) {
        state.dispatchRawDelta(available.toFloat()).toOffset()
      } else {
        Offset.Zero
      }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
      val toFling = available.toFloat()
      val currentOffset = state.requireOffset()
      return if (toFling < 0 && currentOffset > state.minOffset) {
        state.settle(velocity = toFling)
        // since we go to the anchor with tween settling, consume all for the best UX
        available
      } else {
        Velocity.Zero
      }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
      state.settle(velocity = available.toFloat())
      return available
    }

    private fun Float.toOffset(): Offset =
      Offset(
        x = if (orientation == Orientation.Horizontal) this else 0f,
        y = if (orientation == Orientation.Vertical) this else 0f
      )

    @JvmName("velocityToFloat")
    private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

    @JvmName("offsetToFloat")
    private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
  }

@OptIn(ExperimentalMaterialApi::class)
private fun BottomSheetScaffoldAnchorChangeCallback(
  state: BottomSheetState,
  scope: CoroutineScope
) =
  AnchorChangedCallback<BottomSheetValue> { prevTarget, prevAnchors, newAnchors ->
    val previousTargetOffset = prevAnchors[prevTarget]
    val newTarget =
      when (prevTarget) {
        Collapsed -> Collapsed
        Expanded -> if (newAnchors.containsKey(Expanded)) Expanded else Collapsed
      }
    val newTargetOffset = newAnchors.getValue(newTarget)
    if (newTargetOffset != previousTargetOffset) {
      if (state.isAnimationRunning) {
        // Re-target the animation to the new offset if it changed
        scope.launch { state.animateTo(newTarget, velocity = state.lastVelocity) }
      } else {
        // Snap to the new offset value of the target if no animation was running
        val didSnapSynchronously = state.trySnapTo(newTarget)
        if (!didSnapSynchronously) scope.launch { state.snapTo(newTarget) }
      }
    }
  }

private val FabSpacing = 16.dp
private val BottomSheetScaffoldPositionalThreshold = 56.dp
private val BottomSheetScaffoldVelocityThreshold = 125.dp
