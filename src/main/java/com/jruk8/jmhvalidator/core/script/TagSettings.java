package com.jruk8.jmhvalidator.core.script;


/**
 * Modifier-owned tags: {@code <setting:id>} (alias {@code <s:id>})
 * reads one config setting of the running modifier, and {@code
 * <modcolor>} reads its broadcast color. Both stay pure behind the
 * context readers; unknown modifiers and paths resolve {@code
 * null} (brand color for modcolor), never throw.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagSettings {

    private TagSettings() {
    }


    /** Edit-time shape error for {@code <setting>}, or null when fine. */
    public static String settingError(String args) {
        if (args == null || args.trim().isEmpty()) {
            return "Tag <setting> needs a setting id like <setting:base-y>.";
        }
        return null;
    }

}
