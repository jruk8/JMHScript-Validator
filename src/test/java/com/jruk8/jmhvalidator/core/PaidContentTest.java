package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.ports.TagRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Premium probe: paid heads or paid args anywhere in the file. */
class PaidContentTest {
    private static final TagRegistry REGISTRY = TagRegistry.of(
            List.of("p", "place", "pstat", "give", "if", "win"),
            List.of("win"));

    @Test
    void paidHeadMarksPremium() {
        assertTrue(PaidContent.isPremium(file("say <win:HUNTER,done>"), REGISTRY));
    }

    @Test
    void paidArgsMarkPremium() {
        assertTrue(PaidContent.isPremium(file("say <pstat:Steve,bukkit.mine_block,stone>"),
                REGISTRY));
        assertTrue(PaidContent.isPremium(file("<give:Steve,[dirt,1,[tag,foo]]>"), REGISTRY));
        assertTrue(PaidContent.isPremium(file("<place:1 65 2,schematic:hut>"), REGISTRY));
    }

    @Test
    void nestedPaidContentMarksPremium() {
        assertTrue(PaidContent.isPremium(file("say <if:<p> eq Steve,<win:HUNTER,done>,hi>"),
                REGISTRY));
        assertTrue(PaidContent.isPremium(
                file("<give:Steve,[dirt,<pstat:Steve,bukkit.mine_block>,[tag,foo]]>"), REGISTRY));
    }

    @Test
    void freeContentStaysFree() {
        assertFalse(PaidContent.isPremium(file("say hi"), REGISTRY));
        assertFalse(PaidContent.isPremium(file("say <p>"), REGISTRY));
        assertFalse(PaidContent.isPremium(file("<place:1 65 2,stone>"), REGISTRY));
        assertFalse(PaidContent.isPremium(file("<pstat:Steve,health>"), REGISTRY));
        assertFalse(PaidContent.isPremium(file("<give:Steve,[dirt,1,[keep,false]]>"),
                REGISTRY));
        assertFalse(PaidContent.isPremium(new BehaviorFile(Map.of()), REGISTRY));
        assertFalse(PaidContent.isPremium(null, REGISTRY));
        assertFalse(PaidContent.isPremium(file("say hi"), null));
    }

    private static BehaviorFile file(String line) {
        return new BehaviorFile(Map.of("main", new BehaviorFile.BehaviorBlock(
                Map.of("console", List.of(line)), List.of("ON_START"))));
    }
}
