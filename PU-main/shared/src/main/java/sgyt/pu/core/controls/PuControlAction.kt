package sgyt.pu.core.controls

/** Game actions that can be assigned to touch, keyboard, and gamepad controls. */
enum class PuControlAction(
    val id: String,
    val label: String,
    val shortLabel: String,
) {
    UP("up", "Move up", "↑"),
    DOWN("down", "Move down", "↓"),
    LEFT("left", "Move left", "←"),
    RIGHT("right", "Move right", "→"),
    CONFIRM("confirm", "Confirm", "A"),
    CANCEL("cancel", "Cancel", "B"),
    MENU("menu", "Menu", "≡");

    companion object {
        fun fromId(id: String): PuControlAction? = entries.firstOrNull { action -> action.id == id }
    }
}
