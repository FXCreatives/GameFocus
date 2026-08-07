package com.gamefocus.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamefocus.R
import com.gamefocus.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AnimatedNumber(
    targetValue: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    durationMillis: Int = 800
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis, easing = EaseOutCubic),
        label = "numberAnimation"
    )
    Text(
        text = "$prefix${animatedValue}$suffix",
        modifier = modifier,
        fontSize = 22.sp,
        fontWeight = FontWeight.Black,
        color = TextPrimary
    )
}

@Composable
fun PremiumEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = EmeraldGreen.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxSize()
            ) {}
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PremiumErrorCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = GamingError.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxSize()
            ) {}
            Text(
                text = "!",
                color = GamingError,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onButtonClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GamingError),
                border = androidx.compose.foundation.BorderStroke(1.dp, GamingError.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 32.dp)
            ) {
                Text(text = buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
