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
    ActivityType.CYCLING -> R.drawable.ic_activity_cycling
    ActivityType.RUNNING -> R.drawable.ic_activity_running
    ActivityType.STRENGTH_TRAINING -> R.drawable.ic_activity_strength_training
    ActivityType.WALKING -> R.drawable.ic_activity_walking
    ActivityType.YOGA -> R.drawable.ic_activity_yoga
    ActivityType.UNKNOWN -> R.drawable.ic_activity_unknown
}

// The name the interface gives the type; UNKNOWN is called Other there.
@StringRes
private fun ActivityType.labelRes(): Int = when (this) {
    ActivityType.BADMINTON -> R.string.activity_badminton
    ActivityType.CYCLING -> R.string.activity_cycling
    ActivityType.RUNNING -> R.string.activity_running
    ActivityType.STRENGTH_TRAINING -> R.string.activity_strength_training
    ActivityType.WALKING -> R.string.activity_walking
    ActivityType.YOGA -> R.string.activity_yoga
    ActivityType.UNKNOWN -> R.string.activity_other
}

// The sources have different proportions; without this badminton and yoga look heavier
// than the rest at cell size. Measured on a phone, see docs/ux/mockup-plan.md.
private fun ActivityType.opticalScale(): Float = when (this) {
    ActivityType.BADMINTON -> 0.90f
    ActivityType.YOGA -> 0.84f
    else -> 1f
}
