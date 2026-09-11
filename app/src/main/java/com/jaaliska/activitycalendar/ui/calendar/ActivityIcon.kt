package com.jaaliska.activitycalendar.ui.calendar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.jaaliska.activitycalendar.R
import com.jaaliska.activitycalendar.domain.ActivityType
import com.jaaliska.activitycalendar.ui.theme.activityColor

/** The mark that tells an activity's type apart: its own glyph in its own colour. */
@Composable
fun ActivityIcon(
    type: ActivityType,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(type.labelRes()),
) {
    Image(
        painter = painterResource(type.iconRes()),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(activityColor(type)),
        modifier = modifier
            .size(size)
            .scale(type.opticalScale()),
    )
}

@DrawableRes
private fun ActivityType.iconRes(): Int = when (this) {
    ActivityType.BADMINTON -> R.drawable.ic_activity_badminton
    ActivityType.BASKETBALL -> R.drawable.ic_activity_basketball
    ActivityType.BOXING -> R.drawable.ic_activity_boxing
    ActivityType.CYCLING -> R.drawable.ic_activity_cycling
    ActivityType.DANCING -> R.drawable.ic_activity_dancing
    ActivityType.HIKING -> R.drawable.ic_activity_hiking
    ActivityType.MARTIAL_ARTS -> R.drawable.ic_activity_martial_arts
    ActivityType.RUNNING -> R.drawable.ic_activity_running
    ActivityType.SKIING -> R.drawable.ic_activity_skiing
    ActivityType.SNOWBOARDING -> R.drawable.ic_activity_snowboarding
    ActivityType.SOCCER -> R.drawable.ic_activity_soccer
    ActivityType.STRENGTH_TRAINING -> R.drawable.ic_activity_strength_training
    ActivityType.STRETCHING -> R.drawable.ic_activity_stretching
    ActivityType.SWIMMING -> R.drawable.ic_activity_swimming
    ActivityType.TABLE_TENNIS -> R.drawable.ic_activity_table_tennis
    ActivityType.TENNIS -> R.drawable.ic_activity_tennis
    ActivityType.VOLLEYBALL -> R.drawable.ic_activity_volleyball
    ActivityType.WALKING -> R.drawable.ic_activity_walking
    ActivityType.YOGA -> R.drawable.ic_activity_yoga
    ActivityType.UNKNOWN -> R.drawable.ic_activity_unknown
}

// The name the interface gives the type; UNKNOWN is called Other there.
@StringRes
internal fun ActivityType.labelRes(): Int = when (this) {
    ActivityType.BADMINTON -> R.string.activity_badminton
    ActivityType.BASKETBALL -> R.string.activity_basketball
    ActivityType.BOXING -> R.string.activity_boxing
    ActivityType.CYCLING -> R.string.activity_cycling
    ActivityType.DANCING -> R.string.activity_dancing
    ActivityType.HIKING -> R.string.activity_hiking
    ActivityType.MARTIAL_ARTS -> R.string.activity_martial_arts
    ActivityType.RUNNING -> R.string.activity_running
    ActivityType.SKIING -> R.string.activity_skiing
    ActivityType.SNOWBOARDING -> R.string.activity_snowboarding
    ActivityType.SOCCER -> R.string.activity_soccer
    ActivityType.STRENGTH_TRAINING -> R.string.activity_strength_training
    ActivityType.STRETCHING -> R.string.activity_stretching
    ActivityType.SWIMMING -> R.string.activity_swimming
    ActivityType.TABLE_TENNIS -> R.string.activity_table_tennis
    ActivityType.TENNIS -> R.string.activity_tennis
    ActivityType.VOLLEYBALL -> R.string.activity_volleyball
    ActivityType.WALKING -> R.string.activity_walking
    ActivityType.YOGA -> R.string.activity_yoga
    ActivityType.UNKNOWN -> R.string.activity_other
}

private fun ActivityType.opticalScale(): Float = when (this) {
    ActivityType.BADMINTON -> 1.30f
    ActivityType.YOGA -> 0.84f
    else -> 1f
}
