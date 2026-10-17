package com.example.cafeenlinea.ui.onboarding.view

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeenlinea.ui.onboarding.model.OnboardingPage
import com.example.cafeenlinea.ui.onboarding.viewmodel.OnboardingViewModel
import com.example.cafeenlinea.ui.theme.GreenDark
import com.example.cafeenlinea.ui.theme.GreenDarkest
import com.example.cafeenlinea.ui.theme.GreenLightest
import com.example.cafeenlinea.ui.theme.GreenMedium
import kotlin.math.absoluteValue

/** Altura reservada para los controles inferiores, para que el texto no quede debajo. */
private val BottomControlsHeight = 112.dp

/** Espacio arriba del encabezado para la barra superior y tamaño base de la ilustración. */
private val HeroTopPadding = 56.dp
private val IllustrationSize = 280.dp

/** Posiciones (x, y) de los íconos flotantes relativas al centro de la ilustración. */
private val HighlightOffsets = listOf(
    -108.dp to -76.dp,
    112.dp to -36.dp,
    -84.dp to 96.dp
)

/**
 * Onboarding de Café en Línea: un encabezado con degradado e ilustración animada por
 * página, texto con etiqueta, indicador de progreso y botón circular para avanzar.
 */
@Composable
fun OnboardingView(
    onFinishOnboarding: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { uiState.pages.size })

    val finish = {
        viewModel.completeOnboarding()
        onFinishOnboarding()
    }

    LightStatusBarIcons()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    LaunchedEffect(uiState.currentPageIndex) {
        if (pagerState.currentPage != uiState.currentPageIndex) {
            pagerState.animateScrollToPage(uiState.currentPageIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset =
                ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            OnboardingPageContent(
                page = uiState.pages[page],
                pageOffset = pageOffset.coerceIn(0f, 1f)
            )
        }

        OnboardingTopBar(
            showSkip = !uiState.isLastPage,
            onSkip = finish,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        OnboardingBottomControls(
            pageCount = uiState.pages.size,
            currentPage = uiState.currentPageIndex,
            isLastPage = uiState.isLastPage,
            onNext = { viewModel.goToNextPage() },
            onFinish = finish,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * El encabezado es verde oscuro en ambos temas, así que los íconos de la barra de estado
 * deben ser claros mientras se muestra el onboarding.
 */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
            ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

@Composable
private fun OnboardingTopBar(
    showSkip: Boolean,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 12.dp, top = 8.dp)
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocalCafe,
                contentDescription = null,
                tint = GreenLightest,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Café en Línea",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        AnimatedVisibility(visible = showSkip, enter = fadeIn(), exit = fadeOut()) {
            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(contentColor = GreenLightest)
            ) {
                Text("Omitir", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, pageOffset: Float) {
    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingHero(
            page = page,
            pageOffset = pageOffset,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 32.dp)
                .navigationBarsPadding()
                .padding(bottom = BottomControlsHeight)
                .graphicsLayer {
                    alpha = 1f - pageOffset
                    translationY = pageOffset * 40f
                }
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = page.tag.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnboardingHero(
    page: OnboardingPage,
    pageOffset: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp))
            .background(Brush.verticalGradient(listOf(GreenDarkest, GreenDark, GreenMedium)))
    ) {
        // Círculos decorativos de fondo
        DecorativeCircle(size = 280.dp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 96.dp, y = (-72).dp))
        DecorativeCircle(size = 200.dp, modifier = Modifier.align(Alignment.BottomStart).offset(x = (-72).dp, y = 56.dp))

        // En pantallas bajas la ilustración se reduce para no cortarse
        val fitScale = ((maxHeight - HeroTopPadding) / IllustrationSize).coerceIn(0.6f, 1f)

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = HeroTopPadding)
                .graphicsLayer {
                    val scale = fitScale * (1f - pageOffset * 0.25f)
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - pageOffset
                },
            contentAlignment = Alignment.Center
        ) {
            HeroIllustration(page = page)
        }
    }
}

@Composable
private fun DecorativeCircle(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color.White.copy(alpha = 0.06f), CircleShape)
    )
}

@Composable
private fun HeroIllustration(page: OnboardingPage) {
    val transition = rememberInfiniteTransition(label = "float")
    val float by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    Box(modifier = Modifier.requiredSize(IllustrationSize), contentAlignment = Alignment.Center) {
        // Anillos concéntricos
        Box(
            modifier = Modifier
                .size(240.dp)
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(188.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )

        // Ícono principal
        Box(
            modifier = Modifier
                .offset(y = (float * 4).dp)
                .size(136.dp)
                .shadow(elevation = 20.dp, shape = CircleShape)
                .background(GreenLightest, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = page.title,
                tint = GreenDarkest,
                modifier = Modifier.size(64.dp)
            )
        }

        // Íconos flotantes alrededor, cada uno con un desfase distinto
        page.highlights.take(HighlightOffsets.size).forEachIndexed { index, icon ->
            val (x, y) = HighlightOffsets[index]
            val direction = if (index % 2 == 0) 1 else -1
            HighlightChip(
                icon = icon,
                modifier = Modifier.offset(x = x, y = y + (float * 8 * direction).dp)
            )
        }
    }
}

@Composable
private fun HighlightChip(icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GreenDark,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun OnboardingBottomControls(
    pageCount: Int,
    currentPage: Int,
    isLastPage: Boolean,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(BottomControlsHeight)
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageIndicator(pageCount = pageCount, currentPage = currentPage)

        AnimatedContent(
            targetState = isLastPage,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith
                    (fadeOut() + scaleOut(targetScale = 0.9f))
            },
            label = "nextButton"
        ) { last ->
            if (last) {
                Button(
                    onClick = onFinish,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 28.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Comenzar", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            } else {
                ProgressNextButton(
                    progress = (currentPage + 1f) / pageCount.coerceAtLeast(1),
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "indicatorWidth")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                label = "indicatorColor"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .background(color, RoundedCornerShape(50))
            )
        }
    }
}

/** Botón circular con un anillo que muestra cuánto falta del onboarding. */
@Composable
private fun ProgressNextButton(progress: Float, onClick: () -> Unit) {
    val animatedProgress by animateFloatAsState(progress, label = "progress")
    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
            strokeWidth = 3.dp,
            strokeCap = StrokeCap.Round
        )
        FilledIconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Siguiente")
        }
    }
}
