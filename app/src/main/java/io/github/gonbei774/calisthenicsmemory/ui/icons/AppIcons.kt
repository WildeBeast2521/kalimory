package io.github.gonbei774.calisthenicsmemory.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.github.gonbei774.calisthenicsmemory.R

/**
 * Every icon in the app, from one library: Material Symbols, Rounded (Apache-2.0), fetched by
 * scripts/fetch-material-symbols.sh. Names say what an icon means here, not what it draws, so a
 * symbol can change without touching screens. Directional icons are mirrored for right-to-left.
 */
object AppIcons {

    // Navigation and structure
    val Back: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_arrow_back)
    val Forward: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_chevron_right)
    val ExpandMore: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_keyboard_arrow_down)
    val ExpandLess: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_keyboard_arrow_up)
    val DropDown: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_arrow_drop_down)
    val Menu: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_menu)
    val More: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_more_vert)
    val DragHandle: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_drag_handle)

    // Primary destinations: outlined, and filled when selected
    val Today: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_today)
    val TodaySelected: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_today_fill)
    val Train: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_fitness_center)
    val TrainSelected: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_fitness_center_fill)
    val Progress: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_insights)
    val ProgressSelected: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_insights_fill)
    val Library: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_book_2)
    val LibrarySelected: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_book_2_fill)

    // Training
    val Workout: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_play_arrow)
    val Play: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_play_arrow_fill)
    val SkipNext: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_skip_next)
    val Exercise: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_exercise)
    val Program: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_format_list_bulleted)
    val Interval: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_timer)
    val Group: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_folder)
    val RecordManually: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_edit_note)
    val Done: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_check_circle)
    val DoneFilled: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_check_circle_fill)
    val Repeat: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_repeat)
    val Rest: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_hourglass_empty)
    val Timer: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_schedule)
    val History: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_history)
    val Chart: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_bar_chart)
    val Invert: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_swap_vert)

    // Actions
    val Add: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_add)
    val Remove: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_remove)
    val Edit: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_edit)
    val Delete: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_delete)
    val Close: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_close)
    val Check: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_check)
    val Search: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_search)
    val Save: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_save)
    val Download: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_download)
    val Upload: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_upload)
    val Copy: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_content_paste)
    val Share: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_handshake)
    val Favorite: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_star)
    val FavoriteFilled: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_star_fill)

    // Settings and information
    val Settings: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_settings)
    val SourceCode: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_code)
    val Info: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_info)
    val Warning: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_warning)
    val Theme: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_palette)
    val Brightness: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_light_mode)
    val Vibration: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_vibration)
    val Language: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_language)
    val Comment: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_chat)
    val Document: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_description)
    val Folder: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_folder)
    val Camera: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_photo_camera)
    val List: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ms_list_alt)
}
