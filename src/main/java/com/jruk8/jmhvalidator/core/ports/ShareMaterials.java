package com.jruk8.jmhvalidator.core.ports;

import com.jruk8.jmhvalidator.core.share.ShareCodec;
import java.util.Collection;
import java.util.Set;

/**
 * Material names port. The engine owns the material table (it
 * defines validity by resolving Bukkit materials); the validator
 * only reads it, in process from the engine adapter or leniently
 * from CLI adapters that have no engine table. The core never
 * hardcodes material names.
 */
public interface ShareMaterials {
    /** True when this icon names a usable material. */
    boolean validIcon(String icon);

    /** Fallback icon written when a meta block has none. */
    String fallbackIcon();

    /** Lenient materials for adapters without the engine table. */
    static ShareMaterials lenient() {
        return new ShareMaterials() {
            @Override
            public boolean validIcon(String icon) {
                return icon != null && !icon.isBlank();
            }

            @Override
            public String fallbackIcon() {
                return ShareCodec.DEFAULT_ICON;
            }
        };
    }

    /** Fixed icon set for tests. */
    static ShareMaterials of(Collection<String> icons, String fallback) {
        Set<String> copy = Set.copyOf(icons);
        return new ShareMaterials() {
            @Override
            public boolean validIcon(String icon) {
                return copy.contains(icon);
            }

            @Override
            public String fallbackIcon() {
                return fallback;
            }
        };
    }
}
